#!/usr/bin/env python3
"""Aggregate immutable Android evidence; incomplete/manual results never imply acceptance."""
import hashlib
import json
import math
import statistics
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
            if expected[key]['kind']!='manual' and item['status']=='MANUAL_REVIEW':raise ValueError('Automatic case cannot evade grading: '+key)
            if expected[key]['kind']=='manual' and item['status']=='PASS':raise ValueError('Unreviewed manual answer claimed PASS: '+key)
            item['evidence_file']=str(path);results[key]=item
    ordered=[results.get(key,{'id':key,'status':'NOT_RUN'}) for key in expected]
    counts={s:sum(x['status']==s for x in ordered) for s in ['PASS','FAIL','MANUAL_REVIEW','NOT_RUN']}
    terminal=[x for x in ordered if x['status'] in {'PASS','FAIL','MANUAL_REVIEW'}]
    auto=[x for x in terminal if expected[x['id']]['kind']!='manual']
    def measurements(field):
        values=[x.get(field) for x in terminal]
        values=[v for v in values if isinstance(v,(int,float)) and not isinstance(v,bool) and math.isfinite(v) and v>=0]
        return {'observed':len(values),'mean':statistics.mean(values) if values else None,
                'median':statistics.median(values) if values else None,'max':max(values) if values else None}
    def rate(field):
        known=[x[field] for x in terminal if isinstance(x.get(field),bool)]
        return {'events':sum(known),'observed':len(known),'unknown':len(terminal)-len(known),
                'rate':sum(known)/len(known) if known else None}
    metrics={field:measurements(field) for field in ['ttft_ms','tokens_per_second','elapsed_ms','peak_pss_kb']}
    metrics.update({'timeouts':rate('timeout'),'crashes':rate('crash'),'cancellations':rate('cancelled'),
                    'measured_case_total_ms':sum(x.get('elapsed_ms',0) for x in terminal)})
    complete=counts['NOT_RUN']==0 and not errors
    status='NOT_ACCEPTED' if not complete or counts['FAIL'] else ('PENDING_MANUAL_REVIEW' if counts['MANUAL_REVIEW'] else 'PASS')
    return {'suite':suite['id'],'suite_sha256':digest,'commit':commit,'status':status,
            'counts':counts,'execution_complete':counts['NOT_RUN']==0 and not errors,
            'automatic_pass_fraction_all_questions':counts['PASS']/100,'automatic_eligible_executed':len(auto),
            'automatic_pass_rate':sum(x['status']=='PASS' for x in auto)/len(auto) if auto else None,
            'failure_fraction_all_questions':counts['FAIL']/100,'performance':metrics,'accuracy':'UNDETERMINED until manual review completed',
            'errors':errors,'results':ordered,'physical_devices':'NOT_RUN'}

def is_accepted(summary):
    # Pending human review is not an automatic failure of the answer, nor acceptance.
    counts=summary.get('counts',{})
    return (summary.get('status')=='PASS' and summary.get('execution_complete') is True
            and not summary.get('errors') and counts.get('FAIL')==0
            and counts.get('NOT_RUN')==0 and counts.get('MANUAL_REVIEW')==0)

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--commit',required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('batches',nargs='+',type=Path);a=p.parse_args()
    root=Path(__file__).resolve().parents[1]
    result=summarize(root/'app/src/androidTest/assets/professional-100.json',a.batches,a.commit)
    a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:result[k] for k in ['status','counts','execution_complete']}))
    raise SystemExit(0 if result['status']=='PASS' else 1)
