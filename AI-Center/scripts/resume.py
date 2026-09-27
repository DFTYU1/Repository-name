#!/usr/bin/env python3
"""Read state first, then verify the last archive manifest when available."""
from pathlib import Path
import hashlib
import json
import sys
ROOT = Path(__file__).resolve().parents[1]
if (ROOT/'PROJECT_ID').read_text().strip() != 'personal-ai-center-20260909':
    sys.exit('项目 ID 不一致，停止；禁止自动创建新工程。')
print((ROOT/'PROJECT_STATE.md').read_text())
manifest = ROOT/'CHECKPOINT.json'
if manifest.exists():
    data = json.loads(manifest.read_text())
    if data['project_id'] != (ROOT/'PROJECT_ID').read_text().strip():
        sys.exit('检查点项目 ID 不一致。')
    errors = []
    for name, expected in data['files'].items():
        path = (ROOT/name).resolve()
        if ROOT not in path.parents or not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            errors.append(name)
    if errors:
        sys.exit('自检查点起有修改或缺失，请检查差异，不要覆盖已有工作：\n'+'\n'.join(errors))
    print('检查点内容校验通过。')
else:
    print('当前是工作树；归档内 CHECKPOINT.json 保存每个文件的校验值。')
print((ROOT/'TODO.md').read_text())
