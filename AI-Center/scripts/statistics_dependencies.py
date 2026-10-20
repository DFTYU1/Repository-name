#!/usr/bin/env python3
"""Fetch only pinned Apache statistics artifacts for host checks; never used by the Android runtime."""
from pathlib import Path
import hashlib,json,os,re,urllib.request
ROOT=Path(__file__).resolve().parents[1]
def ensure():
    lock=json.loads((ROOT/'ci/statistics-lock.json').read_text())
    cache=ROOT/'core/build/host-deps';cache.mkdir(parents=True,exist_ok=True)
    paths=[]
    for item in lock['artifacts']:
        name,version=item['artifact'],item['version']
        if not re.fullmatch(r'[a-z0-9-]+',name) or not re.fullmatch(r'[0-9.]+',version):raise ValueError('Invalid statistical dependency identifier')
        url=f'https://repo.maven.apache.org/maven2/org/apache/commons/{name}/{version}/{name}-{version}.jar'
        if item['url']!=url or not re.fullmatch('[0-9a-f]{64}',item['sha256']):raise ValueError('Invalid pinned dependency origin/checksum')
        path=cache/(name+'-'+version+'.jar')
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest()!=item['sha256']:
            data=urllib.request.urlopen(url,timeout=45).read(5*1024*1024+1)
            if len(data)>5*1024*1024 or hashlib.sha256(data).hexdigest()!=item['sha256']:raise ValueError('Statistical dependency checksum mismatch: '+name)
            temp=path.with_suffix('.part');temp.write_bytes(data);os.replace(temp,path)
        paths.append(path)
    return paths
if __name__=='__main__':
    files=ensure();print('Verified Apache statistical dependencies:',len(files),'files')
