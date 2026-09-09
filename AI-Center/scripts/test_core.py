#!/usr/bin/env python3
"""Compile the exact production Java core with the JDK compiler module and run its tests."""
from pathlib import Path
from datetime import datetime, timezone
import json
import shutil
import subprocess
import sys
ROOT = Path(__file__).resolve().parents[1]
build = ROOT/'core/build/host-tests'
build.mkdir(parents=True, exist_ok=True)
sources = sorted((ROOT/'core/src/main/java').rglob('*.java'))+sorted((ROOT/'core/src/test/java').rglob('*.java'))
java = shutil.which('java')
if not java:
    sys.exit('Java runtime not found')
compile_command = [java, '-m', 'jdk.compiler/com.sun.tools.javac.Main', '--release', '17', '-Xlint:all', '-Werror', '-encoding', 'UTF-8', '-d', str(build)]+[str(p) for p in sources]
compiled = subprocess.run(compile_command, capture_output=True, text=True, timeout=120)
report = {'time_utc': datetime.now(timezone.utc).isoformat(), 'scope':'Production core on host JVM; excludes Android framework, APK and real LLM/device tests.',
          'compile_exit':compiled.returncode, 'compile_output':compiled.stdout+compiled.stderr,'test_exit':None,'passed':0}
output = ''
if compiled.returncode == 0:
    tested = subprocess.run([java,'-ea','-cp',str(build),'local.aicenter.core.CoreTest'],capture_output=True,text=True,timeout=120)
    report['test_exit'] = tested.returncode
    output = tested.stdout+tested.stderr
    report['passed'] = sum(line.startswith('PASS ') for line in tested.stdout.splitlines())
report['status'] = 'PASS' if report['compile_exit']==0 and report['test_exit']==0 else 'FAIL'
report['test_output'] = output
(ROOT/'Tests').mkdir(exist_ok=True)
(ROOT/'Tests/core-tests.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
(ROOT/'Tests/core-tests.log').write_text(report['compile_output']+output)
print(report['compile_output']+output)
print('Core status:', report['status'])
sys.exit(0 if report['status']=='PASS' else 1)
