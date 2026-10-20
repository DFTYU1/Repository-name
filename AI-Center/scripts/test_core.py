#!/usr/bin/env python3
"""Compile the exact production Java core with the JDK compiler module and run its tests."""
from pathlib import Path
from datetime import datetime, timezone
import json
import os
from statistics_dependencies import ensure
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
try:
    dependencies=ensure()
except Exception as error:
    (ROOT/'Tests/core-tests.json').write_text(json.dumps({'status':'BLOCKED','scope':'Host dependency preparation','error':str(error)},indent=2)+'\n')
    raise
dependency_path=os.pathsep.join(str(p) for p in dependencies)
class_path=str(build)+os.pathsep+dependency_path
compile_command = [java, '-m', 'jdk.compiler/com.sun.tools.javac.Main', '--release', '17', '-Xlint:all', '-Werror', '-encoding', 'UTF-8', '-classpath', dependency_path, '-d', str(build)]+[str(p) for p in sources]
compiled = subprocess.run(compile_command, capture_output=True, text=True, timeout=120)
report = {'time_utc': datetime.now(timezone.utc).isoformat(), 'scope':'Production core on host JVM; excludes Android framework, APK and real LLM/device tests.',
          'compile_exit':compiled.returncode, 'compile_output':compiled.stdout+compiled.stderr,'test_exit':None,'passed':0}
output = ''
if compiled.returncode == 0:
    tested = subprocess.run([java,'-ea','-cp',class_path,'local.aicenter.core.CoreTest'],capture_output=True,text=True,timeout=120)
    report['test_exit'] = tested.returncode
    output = tested.stdout+tested.stderr
    report['passed'] = sum(line.startswith('PASS ') for line in tested.stdout.splitlines())
if compiled.returncode == 0:
    tool_test = subprocess.run([java,'-ea','-cp',class_path,'local.aicenter.core.ToolCalculationTest'],capture_output=True,text=True,timeout=120)
    tool_report = {'scope':'Production calculator and formula validator; planner fixtures, NOT real model or Android', 'exit':tool_test.returncode,'output':tool_test.stdout+tool_test.stderr}
    (ROOT/'Tests/tool-calculation-tests.json').write_text(json.dumps(tool_report,ensure_ascii=False,indent=2)+'\n')
    output += tool_report['output']
    if tool_test.returncode: report['test_exit']=tool_test.returncode
if compiled.returncode == 0:
    knowledge = subprocess.run([java,'-ea','-cp',class_path,'local.aicenter.core.KnowledgeChatTest'],capture_output=True,text=True,timeout=120)
    output += knowledge.stdout+knowledge.stderr
    if knowledge.returncode: report['test_exit']=knowledge.returncode
if compiled.returncode == 0:
    regression = subprocess.run([java,'-ea','-cp',class_path,'local.aicenter.core.RefactorRegressionTest'],capture_output=True,text=True,timeout=120)
    output += regression.stdout+regression.stderr
    if regression.returncode: report['test_exit']=regression.returncode
if compiled.returncode == 0:
    xlsx = subprocess.run([java,'-ea','-cp',class_path,'local.aicenter.core.XlsxReaderTest'],capture_output=True,text=True,timeout=120)
    output += xlsx.stdout+xlsx.stderr
    if xlsx.returncode: report['test_exit']=xlsx.returncode
if compiled.returncode == 0:
    editor = subprocess.run([java,'-ea','-cp',class_path,'local.aicenter.core.XlsxEditorTest'],capture_output=True,text=True,timeout=120)
    output += editor.stdout+editor.stderr
    if editor.returncode: report['test_exit']=editor.returncode
if compiled.returncode == 0:
    imr = subprocess.run([java,'-ea','-cp',class_path,'local.aicenter.core.IndividualsChartTest'],capture_output=True,text=True,timeout=120)
    output += imr.stdout+imr.stderr
    if imr.returncode: report['test_exit']=imr.returncode
if compiled.returncode == 0:
    charts = subprocess.run([java,'-ea','-cp',class_path,'local.aicenter.core.ControlChartsTest'],capture_output=True,text=True,timeout=120)
    output += charts.stdout+charts.stderr
    if charts.returncode: report['test_exit']=charts.returncode
if compiled.returncode == 0:
    msa = subprocess.run([java,'-ea','-cp',class_path,'local.aicenter.core.MeasurementSystemTest'],capture_output=True,text=True,timeout=120)
    output += msa.stdout+msa.stderr
    if msa.returncode: report['test_exit']=msa.returncode
report['status'] = 'PASS'  if report['compile_exit']==0 and report['test_exit']==0 else 'FAIL'
report['test_output'] = output
(ROOT/'Tests').mkdir(exist_ok=True)
(ROOT/'Tests/core-tests.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
(ROOT/'Tests/core-tests.log').write_text(report['compile_output']+output)
print(report['compile_output']+output)
print('Core status:', report['status'])
sys.exit(0 if report['status']=='PASS' else 1)
