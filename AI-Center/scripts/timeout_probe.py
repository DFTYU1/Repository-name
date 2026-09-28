#!/usr/bin/env python3
"""Reproduce previously observed timeouts on an installed synthetic CI emulator.
No production question IDs/answers are used. Defaults to plan-only, no adb action.
"""
import argparse,json,os,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def select(summary,suite):
    rows=summary if isinstance(summary,list) else summary['results']
    failures={r['id'] for r in rows if r.get('after',r).get('timeout') is True}
    return [(i,q['id']) for i,q in enumerate(suite['cases']) if q['id'] in failures]
def main():
    p=argparse.ArgumentParser();p.add_argument('--summary',type=Path,required=True);p.add_argument('--serial');p.add_argument('--execute',action='store_true');a=p.parse_args()
    selected=select(json.loads(a.summary.read_text()),json.loads((ROOT/'app/src/androidTest/assets/professional-100.json').read_text()))
    print(json.dumps({'scope':'diagnostic only; frozen grading and timeout unchanged','cases':selected,'execution':'REQUESTED' if a.execute else 'NOT_RUN'}))
    if not a.execute:return
    if not a.serial or not os.environ.get('AI_CENTER_CI_FIXTURE_PASSWORD'):p.error('explicit synthetic emulator serial and fixture password required')
    if not a.serial.startswith('emulator-'):p.error('probe refuses physical device')
    dest=ROOT/'Tests/timeout-probe';dest.mkdir(exist_ok=True)
    for index,qid in selected:
        cmd=['adb','-s',a.serial,'shell','am','instrument','-w','-r','-e','professional_suite','true','-e','timeout_recovery','true','-e','case_start',str(index),'-e','case_count','1','-e','fixture_password',os.environ['AI_CENTER_CI_FIXTURE_PASSWORD'],'local.aicenter.app.dev.test/local.aicenter.app.FoundationInstrumentation']
        try:result=subprocess.run(cmd,capture_output=True,text=True,timeout=350)
        except subprocess.TimeoutExpired:raise SystemExit('Probe infrastructure timeout; command deliberately redacted')
        # Do not retain command/fixture credential in public logs.
        output=result.stdout.replace(os.environ['AI_CENTER_CI_FIXTURE_PASSWORD'],'[REDACTED]')
        (dest/(qid+'.log')).write_text(output)
        capture=subprocess.run(['adb','-s',a.serial,'exec-out','run-as','local.aicenter.app.dev','cat','files/ci-professional-benchmark.json'],capture_output=True,timeout=30)
        if capture.returncode:raise SystemExit('Cannot retrieve diagnostic report')
        (dest/(qid+'.json')).write_bytes(capture.stdout)
        evidence=json.loads(capture.stdout)['results'][0]
        if 'phase_timing' not in evidence:raise SystemExit('Installed APK lacks current diagnostics; refusing stale evidence')
        if evidence.get('timeout') and 'post_timeout_recovery' not in evidence:raise SystemExit('Missing post-timeout recovery evidence')
        if result.returncode or 'benchmark_status=EXECUTED' not in output:raise SystemExit('Probe infrastructure failed')
if __name__=='__main__':main()
