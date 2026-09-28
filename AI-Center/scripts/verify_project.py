#!/usr/bin/env python3
"""Real SQLite constraints, Android source parsing, and checkpoint round-trip validation."""
from pathlib import Path
from datetime import datetime, timezone
import json
import sqlite3
import subprocess
import sys
import tempfile
import zipfile
import xml.etree.ElementTree as ET
from checkpoint import create_checkpoint
ROOT=Path(__file__).resolve().parents[1]
passed=[]
def test(name,fn):
    fn();passed.append(name);print('PASS',name)
def schema_constraints():
    db=sqlite3.connect(':memory:');db.executescript((ROOT/'platform-android/src/main/assets/schema.sql').read_text())
    db.execute("INSERT INTO documents VALUES(?,?,?,?,?,?,?,?)",('d','encrypted','copy.txt','hash',1,0,'USER_UPLOAD','IMPORTED'))
    db.execute("INSERT INTO chunks VALUES(?,?,?,?)",('d',0,0,'encrypted'))
    for command,params in [
        ('INSERT INTO chunks VALUES(?,?,?,?)',('missing',0,0,'encrypted')),
        ('INSERT INTO documents VALUES(?,?,?,?,?,?,?,?)',('e','encrypted','copy2.txt','hash',-1,0,'USER_UPLOAD','IMPORTED')),
        ('INSERT INTO messages(role,body_enc,created_utc) VALUES(?,?,?)',('system','encrypted',0))]:
        try:db.execute(command,params)
        except sqlite3.IntegrityError:pass
        else:raise AssertionError('Constraint failed')
    db.execute('DELETE FROM documents WHERE id=?',('d',))
    assert db.execute('SELECT COUNT(*) FROM chunks').fetchone()[0]==0
    assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
    db.close()
def transaction_rollback():
    db=sqlite3.connect(':memory:');db.executescript((ROOT/'platform-android/src/main/assets/schema.sql').read_text())
    db.execute("INSERT INTO documents VALUES(?,?,?,?,?,?,?,?)",('d','encrypted','copy.txt','hash',1,0,'USER_UPLOAD','IMPORTED'));db.commit()
    try:
        with db:
            db.execute('INSERT INTO chunks VALUES(?,?,?,?)',('d',0,0,'encrypted'))
            raise RuntimeError('simulated interruption')
    except RuntimeError:pass
    assert db.execute('SELECT COUNT(*) FROM chunks').fetchone()[0]==0
    assert db.execute('SELECT index_state FROM documents').fetchone()[0]=='IMPORTED'
def syntax():
    command=['java',str(ROOT/'scripts/JavaSyntaxCheck.java'),str(ROOT/'platform-android/src/main/java'),str(ROOT/'app/src/main/java'),str(ROOT/'app/src/androidTest/java')]
    result=subprocess.run(command,capture_output=True,text=True,timeout=60)
    (ROOT/'Tests/android-syntax.log').write_text(result.stdout+result.stderr)
    print(result.stdout+result.stderr)
    if result.returncode:raise AssertionError('Java syntax errors')
def manifest_scope():
    root=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}'
    permissions={p.get(ns+'name') for p in root.findall('uses-permission')}
    assert permissions=={'android.permission.USE_BIOMETRIC'}
    application=root.find('application');assert application.get(ns+'allowBackup')=='false'
    assert application.get(ns+'usesCleartextTraffic')=='false'
    assert root.find('.//service') is None
def professional_definition():
    q=json.loads((ROOT/'app/src/androidTest/assets/professional-100.json').read_text())['cases']
    assert len(q)==100 and len({x['id'] for x in q})==100
    assert {'tool','cancel','conversation','number','formula','manual'} <= {x['kind'] for x in q}
    assert all(x['rubric'] and x['source'] and x['difficulty'] for x in q)
    engine=(ROOT/'platform-android/src/main/cpp/engine.cpp').read_text()
    assert 'finish below 450 tokens' in engine and 'output only that requested value' in engine
    assert 'Q025' not in engine and q[24]['prompt'] not in engine and q[24]['rubric'] not in engine
def checkpoint_roundtrip():
    with tempfile.TemporaryDirectory() as tmp:
        output=Path(tmp)/'checkpoint.zip';info=create_checkpoint(ROOT,output,'automated checkpoint round-trip')
        with zipfile.ZipFile(output) as archive:
            archive.extractall(Path(tmp)/'restored')
            manifest=json.loads(archive.read('AI-Center/CHECKPOINT.json'))
            assert manifest['project_id']==(ROOT/'PROJECT_ID').read_text().strip()
        restored=Path(tmp)/'restored/AI-Center'
        result=subprocess.run([sys.executable,str(restored/'scripts/resume.py')],capture_output=True,text=True,timeout=30)
        assert result.returncode==0,result.stdout+result.stderr
        (restored/'VERSION').write_text('tampered')
        changed=subprocess.run([sys.executable,str(restored/'scripts/resume.py')],capture_output=True,text=True,timeout=30)
        assert changed.returncode!=0,'Changed checkpoint silently accepted'
        assert info['files']>20
(ROOT/'Tests').mkdir(exist_ok=True)
status='PASS';error=None
try:
    test('SQLite schema constraints and foreign keys',schema_constraints)
    test('SQLite indexing rollback preserves imported document',transaction_rollback)
    test('Android Java syntax parsing only',syntax)
    test('Android manifest requests no broad access or network',manifest_scope)
    test('100 fixed professional questions and execution kinds',professional_definition)
    test('Checkpoint restore and tamper detection',checkpoint_roundtrip)
except Exception as e:
    status='FAIL';error=str(e);print('FAIL',error)
report={'time_utc':datetime.now(timezone.utc).isoformat(),'status':status,'passed':passed,'error':error,
        'limitations':'SQLite tests use host SQLite; Java parse excludes Android type checking, emulator and device tests.'}
(ROOT/'Tests/project-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
sys.exit(0 if status=='PASS' else 1)
