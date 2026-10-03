"""Authenticated encrypted build transport. Never publishes an APK or a private key."""
import argparse
import hashlib
import hmac
import json
import os
from pathlib import Path
import subprocess
import tempfile

PART_BYTES = 28 * 1024 * 1024
MAX_BYTES = 15 * 1024**3

def canonical(value):
    return json.dumps(value,sort_keys=True,separators=(',',':')).encode()

def sha(path):
    digest=hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda:stream.read(1024**2),b''):digest.update(block)
    return digest.hexdigest()

def wrap(key,public):
    return subprocess.check_output(['openssl','pkeyutl','-encrypt','-pubin','-inkey',str(public),'-pkeyopt','rsa_padding_mode:oaep','-pkeyopt','rsa_oaep_md:sha256'],input=key).hex()

def encrypt(apk,metadata,public,output):
    size=apk.stat().st_size
    if not 0<size<=MAX_BYTES:raise ValueError('Payload size out of bounds')
    if size!=metadata['bytes'] or sha(apk)!=metadata['sha256']:raise ValueError('Verified APK metadata mismatch')
    output.mkdir(parents=True,exist_ok=False)
    key=os.urandom(64);iv=os.urandom(16)
    chunks=[]
    process=subprocess.Popen(['openssl','enc','-aes-256-ctr','-K',key[:32].hex(),'-iv',iv.hex(),'-in',str(apk)],stdout=subprocess.PIPE)
    try:
        while True:
            block=process.stdout.read(PART_BYTES)
            if not block:break
            name='part-%02d.bin'%len(chunks)
            (output/name).write_bytes(block)
            chunks.append({'file':name,'bytes':len(block),'sha256':hashlib.sha256(block).hexdigest()})
        if process.wait()!=0:raise ValueError('Encryption failed')
    finally:
        process.stdout.close()
        if process.poll() is None:process.kill();process.wait()
    manifest={'schema':1,'algorithm':'RSA-OAEP-SHA256 + AES-256-CTR + HMAC-SHA256','wrapped_key':wrap(key,public),'iv':iv.hex(),'payload':metadata,'chunks':chunks}
    manifest['mac']=hmac.new(key[32:],canonical(manifest),hashlib.sha256).hexdigest()
    (output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    return manifest

def decrypt(folder,private,output):
    manifest=json.loads((folder/'manifest.json').read_text())
    key=subprocess.check_output(['openssl','pkeyutl','-decrypt','-inkey',str(private),'-pkeyopt','rsa_padding_mode:oaep','-pkeyopt','rsa_oaep_md:sha256'],input=bytes.fromhex(manifest['wrapped_key']))
    if len(key)!=64:raise ValueError('Key length')
    mac=manifest.pop('mac')
    if not hmac.compare_digest(mac,hmac.new(key[32:],canonical(manifest),hashlib.sha256).hexdigest()):raise ValueError('Manifest authentication failed')
    if manifest['schema']!=1 or not 0<manifest['payload']['bytes']<=MAX_BYTES:raise ValueError('Unsupported manifest')
    total=0
    for i,part in enumerate(manifest['chunks']):
        if part['file']!='part-%02d.bin'%i:raise ValueError('Invalid part path')
        path=folder/part['file']
        if path.is_symlink() or path.stat().st_size!=part['bytes'] or sha(path)!=part['sha256']:raise ValueError('Ciphertext integrity failure')
        total+=part['bytes']
    if total!=manifest['payload']['bytes']:raise ValueError('Missing ciphertext')
    output.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(dir=output.parent) as temporary:
        ciphertext=Path(temporary)/'cipher';plain=Path(temporary)/'plain'
        with ciphertext.open('wb') as dest:
            for part in manifest['chunks']:
                with (folder/part['file']).open('rb') as stream:
                    for block in iter(lambda:stream.read(1024**2),b''):dest.write(block)
        subprocess.run(['openssl','enc','-d','-aes-256-ctr','-K',key[:32].hex(),'-iv',manifest['iv'],'-in',str(ciphertext),'-out',str(plain)],check=True)
        if sha(plain)!=manifest['payload']['sha256']:raise ValueError('Plaintext integrity failure')
        os.replace(plain,output)
    return manifest['payload']

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('mode',choices=['encrypt','decrypt']);parser.add_argument('--input',required=True);parser.add_argument('--key',required=True);parser.add_argument('--output',required=True);parser.add_argument('--metadata')
    args=parser.parse_args()
    if args.mode=='encrypt':
        value=encrypt(Path(args.input),json.loads(Path(args.metadata).read_text()),Path(args.key),Path(args.output))
        print('Encrypted verified application into',len(value['chunks']),'authenticated transport parts. No private key or plaintext uploaded.')
    else:
        value=decrypt(Path(args.input),Path(args.key),Path(args.output));print(json.dumps({'bytes':value['bytes'],'sha256':value['sha256'],'source_sha':value.get('source_sha')}))
