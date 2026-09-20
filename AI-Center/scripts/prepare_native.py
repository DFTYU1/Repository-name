#!/usr/bin/env python3
"""Fetch public, pinned build inputs. Never use this script to upload data."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
LOCK = json.loads((ROOT/'ci/native-lock.json').read_text())
ASSETS = ROOT/'platform-android/build/generated/modelAssets'

def digest(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda:f.read(1024*1024),b''): h.update(block)
    return h.hexdigest()

def main():
    if (ROOT/'PROJECT_ID').read_text().strip()!=LOCK['project_id']: raise ValueError('Wrong project')
    engine=ROOT/'toolchains/llama.cpp'
    if not engine.exists():
        engine.mkdir(parents=True)
        subprocess.run(['git','init',str(engine)],check=True)
        subprocess.run(['git','-C',str(engine),'remote','add','origin',LOCK['llama_repository']],check=True)
    head=subprocess.run(['git','-C',str(engine),'rev-parse','HEAD'],capture_output=True,text=True)
    if head.stdout.strip()!=LOCK['llama_commit']:
        if subprocess.run(['git','-C',str(engine),'status','--porcelain'],capture_output=True,text=True,check=True).stdout.strip():
            raise ValueError('Existing native checkout has edits; preserved')
        subprocess.run(['git','-C',str(engine),'fetch','--depth','1','origin',LOCK['llama_commit']],check=True,timeout=300)
        subprocess.run(['git','-C',str(engine),'checkout','--detach',LOCK['llama_commit']],check=True)
    actual=subprocess.check_output(['git','-C',str(engine),'rev-parse','HEAD'],text=True).strip()
    if actual!=LOCK['llama_commit']: raise ValueError('Engine revision mismatch')
    spec=LOCK['model']; ASSETS.mkdir(parents=True,exist_ok=True); target=ASSETS/'base.gguf'
    if not (target.is_file() and target.stat().st_size==spec['bytes'] and digest(target)==spec['sha256']):
        part=ASSETS/'base.gguf.part'
        try:
            url='https://huggingface.co/'+spec['repository']+'/resolve/'+spec['revision']+'/'+spec['filename']
            request=urllib.request.Request(url,headers={'User-Agent':'AI-Center-build/0.2'})
            with urllib.request.urlopen(request,timeout=120) as response,part.open('wb') as output:
                size=0
                while True:
                    block=response.read(1024*1024)
                    if not block: break
                    size+=len(block)
                    if size>spec['bytes']: raise ValueError('Model exceeds pinned size')
                    output.write(block)
                output.flush();os.fsync(output.fileno())
            if part.stat().st_size!=spec['bytes'] or digest(part)!=spec['sha256']: raise ValueError('Model SHA-256/size mismatch')
            os.replace(part,target)
        finally:
            part.unlink(missing_ok=True)
    (ASSETS/'model-manifest.json').write_text(json.dumps(spec,indent=2)+'\n')
    licenses=ASSETS/'licenses';licenses.mkdir(exist_ok=True)
    (licenses/'llama.cpp-MIT.txt').write_bytes((engine/'LICENSE').read_bytes())
    # Apache text is included in the project's license bundle, fetched from model author at build time.
    with urllib.request.urlopen('https://huggingface.co/Qwen/Qwen3.5-0.8B/resolve/main/LICENSE',timeout=60) as response:
        license_text=response.read(100000)
    if b'Apache License' not in license_text: raise ValueError('Model license unavailable')
    (licenses/'Qwen-Apache-2.0.txt').write_bytes(license_text)
    report={'engine_commit':actual,'model':spec,'verified_sha256':digest(target),'status':'VERIFIED_BUILD_INPUTS_ONLY'}
    (ROOT/'Tests').mkdir(exist_ok=True)
    (ROOT/'Tests/native-inputs.json').write_text(json.dumps(report,indent=2)+'\n')
    print('Pinned engine and model verified; inference still requires Android execution.')

if __name__=='__main__': main()
