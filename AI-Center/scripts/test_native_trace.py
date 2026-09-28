#!/usr/bin/env python3
"""Host test of production native timing lifecycle, not llama/Android execution."""
from pathlib import Path
import json,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix='aicenter-trace-') as d:
    binary=str(Path(d)/'trace-test')
    compile_result=subprocess.run(['c++','-std=c++17','-Wall','-Wextra','-Werror',str(ROOT/'Tests/native/inference_trace_test.cpp'),'-o',binary],capture_output=True,text=True)
    run=subprocess.run([binary],capture_output=True,text=True) if compile_result.returncode==0 else None
    report={'scope':'Host test of production PhaseTrace; Android and actual inference NOT_RUN','compile_exit':compile_result.returncode,'test_exit':None if run is None else run.returncode,'output':compile_result.stdout+compile_result.stderr+('' if run is None else run.stdout+run.stderr)}
    (ROOT/'Tests/native-trace-tests.json').write_text(json.dumps(report,indent=2)+'\n');print(report['output'])
    raise SystemExit(0 if run is not None and run.returncode==0 else 1)
