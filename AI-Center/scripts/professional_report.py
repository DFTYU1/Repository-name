#!/usr/bin/env python3
"""Aggregate immutable Android evidence; incomplete/manual results never imply acceptance."""
import hashlib
import json
from pathlib import Path

def summarize(suite_path, batch_paths, commit):
    raw=Path(suite_path).read_bytes();suite=json.loads(raw)
    expected={q['id']:q for q in suite['cases']};results={};errors=[]
    if len(expected)!=100:raise ValueError('Exactly 100 unique cases required')
    digest=hashlib.sha256(raw).hexdigest()
    for path in batch_paths:
        report=json.loads(Path(path).read_text())
        if report.get('suite_sha256')!=digest or report.get('commit')!=commit:
            errors.append(f'{Path(path).name}: evidence provenance mismatch');continue
        for item in report.get('results',[]):
            key=item.get('id')
            if key not in expected or key in results:raise ValueError('Unknown or duplicate result: '+str(key))
            item=dict(item)
            if item.get('input')!=expected[key]['prompt']:raise ValueError('Question changed during execution: '+key)
            if item.get('status') not in {'PASS','FAIL','MANUAL_REVIEW'}:item['status']='NOT_RUN'
            if expected[key]['kind']=='manual' and item['status']=='PASS':raise ValueError('Unreviewed manual answer claimed PASS: '+key)
            item['evidence_file']=str(path);results[key]=item
    ordered=[results.get(key,{'id':key,'status':'NOT_RUN'}) for key in expected]
    counts={s:sum(x['status']==s for x in ordered) for s in ['PASS','FAIL','MANUAL_REVIEW','NOT_RUN']}
    return {'suite':suite['id'],'suite_sha256':digest,'commit':commit,'status':'PASS' if counts['PASS']==100 and not errors else 'NOT_ACCEPTED',
            'counts':counts,'execution_complete':counts['NOT_RUN']==0 and not errors,
            'automatic_pass_fraction_all_questions':counts['PASS']/100,'accuracy':'UNDETERMINED until manual review completed',
            'errors':errors,'results':ordered,'physical_devices':'NOT_RUN'}

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--commit',required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('batches',nargs='+',type=Path);a=p.parse_args()
    root=Path(__file__).resolve().parents[1]
    result=summarize(root/'app/src/androidTest/assets/professional-100.json',a.batches,a.commit)
    a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:result[k] for k in ['status','counts','execution_complete']}))
    raise SystemExit(0 if result['status']=='PASS' else 1)
