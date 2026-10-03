import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from encrypted_delivery import encrypt,decrypt

class TransportTest(unittest.TestCase):
    def test_roundtrip_and_tamper(self):
        with tempfile.TemporaryDirectory() as directory:
            r=Path(directory);key=r/'key';public=r/'public';apk=r/'input';out=r/'encrypted'
            subprocess.run(['openssl','genpkey','-algorithm','RSA','-pkeyopt','rsa_keygen_bits:2048','-out',str(key)],check=True,stderr=subprocess.DEVNULL)
            subprocess.run(['openssl','pkey','-in',str(key),'-pubout','-out',str(public)],check=True)
            apk.write_bytes(b'synthetic application bytes\x00'*5000)
            metadata={'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest()}
            encrypt(apk,metadata,public,out);decrypt(out,key,r/'restored');self.assertEqual(apk.read_bytes(),(r/'restored').read_bytes())
            m=json.loads((out/'manifest.json').read_text());m['payload']['bytes']+=1;(out/'manifest.json').write_text(json.dumps(m))
            with self.assertRaisesRegex(ValueError,'authentication'):decrypt(out,key,r/'bad')
            m['payload']['bytes']-=1;(out/'manifest.json').write_text(json.dumps(m));part=out/'part-00.bin';part.write_bytes(b'x'+part.read_bytes()[1:])
            with self.assertRaisesRegex(ValueError,'integrity'):decrypt(out,key,r/'bad')
            self.assertFalse((r/'bad').exists())
    def test_mismatched_verified_payload(self):
        with tempfile.TemporaryDirectory() as directory:
            r=Path(directory);apk=r/'input';apk.write_bytes(b'fixture')
            with self.assertRaisesRegex(ValueError,'metadata'):encrypt(apk,{'bytes':7,'sha256':'0'*64},r/'unused',r/'out')

unittest.main()
