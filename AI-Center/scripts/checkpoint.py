#!/usr/bin/env python3
"""Create an atomic source checkpoint. A separate official save operation makes it durable."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import tempfile
import zipfile
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
EXCLUDED = {'.git', '.gradle', 'build', '.idea', '__pycache__', 'toolchains', 'private'}
SECRET_SUFFIXES = {'.jks', '.keystore', '.gguf', '.onnx', '.tflite', '.part'}

def source_files(root):
    for path in sorted(root.rglob('*')):
        rel = path.relative_to(root)
        if any(part in EXCLUDED for part in rel.parts):
            continue
        if path.is_symlink():
            raise ValueError('Checkpoint refuses symlinks: ' + str(rel))
        if not path.is_file() or path.name in {'local.properties', 'CHECKPOINT.json', '.env'}:
            continue
        if path.suffix in SECRET_SUFFIXES or path.name.startswith('.env.'):
            continue
        yield path

def create_checkpoint(root, output, reason):
    project_id = (root / 'PROJECT_ID').read_text().strip()
    if project_id != 'personal-ai-center-20260909':
        raise ValueError('Wrong project identity; do not initialize another project')
    for name in ('PROJECT_STATE.md', 'TODO.md', 'BUILD_STATUS.md', 'CHANGELOG.md', 'DEVELOPMENT_PROGRESS.md'):
        if not (root / name).is_file():
            raise ValueError('Required state missing: ' + name)
    hashes = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in source_files(root)}
    manifest = {'schema': 1, 'project_id': project_id, 'version': (root/'VERSION').read_text().strip(),
                'created_utc': datetime.now(timezone.utc).isoformat(), 'reason': reason,
                'durability': 'Local checkpoint; executor must persist through official file saving.',
                'files': hashes}
    output.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(prefix=output.name+'.', suffix='.tmp', dir=output.parent)
    os.close(fd)
    try:
        with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as z:
            for name, expected in hashes.items():
                data = (root/name).read_bytes()
                if hashlib.sha256(data).hexdigest() != expected:
                    raise ValueError('Source changed during checkpoint: ' + name)
                z.writestr('AI-Center/'+name, data)
            z.writestr('AI-Center/CHECKPOINT.json', json.dumps(manifest, ensure_ascii=False, indent=2))
        with zipfile.ZipFile(tmp) as z:
            if z.testzip():
                raise ValueError('Archive CRC verification failed')
            for name, expected in hashes.items():
                if hashlib.sha256(z.read('AI-Center/'+name)).hexdigest() != expected:
                    raise ValueError('Archive hash verification failed: ' + name)
        with open(tmp, 'rb') as f:
            os.fsync(f.fileno())
        os.replace(tmp, output)
        return {'project_id': project_id, 'archive': str(output), 'files': len(hashes),
                'sha256': hashlib.sha256(output.read_bytes()).hexdigest(), 'bytes': output.stat().st_size}
    finally:
        if os.path.exists(tmp):
            os.unlink(tmp)

if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--output', type=Path, default=ROOT.parent/'Deliverables'/'AI-Center-Source.zip')
    p.add_argument('--reason', default='development checkpoint')
    args = p.parse_args()
    print(json.dumps(create_checkpoint(ROOT, args.output.resolve(), args.reason), ensure_ascii=False, indent=2))
