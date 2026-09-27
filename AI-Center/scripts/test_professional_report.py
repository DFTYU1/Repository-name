#!/usr/bin/env python3
"""Evidence integrity tests: do not execute or simulate an AI model."""
import copy,hashlib,json,tempfile,unittest
from pathlib import Path
from professional_report import summarize
ROOT=Path(__file__).resolve().parents[1]
class EvidenceTests(unittest.TestCase):
 def setUp(self):
  self.suite=ROOT/'app/src/androidTest/assets/professional-100.json';self.questions=json.loads(self.suite.read_text())['cases']
  self.base={'suite_sha256':hashlib.sha256(self.suite.read_bytes()).hexdigest(),'commit':'test-commit','results':[]}
 def run_report(self,batches):
  with tempfile.TemporaryDirectory() as tmp:
   paths=[]
   for i,b in enumerate(batches):
    p=Path(tmp)/f'{i}.json';p.write_text(json.dumps(b));paths.append(p)
   return summarize(self.suite,paths,'test-commit')
 def test_missing_results_never_pass(self):
  r=self.run_report([self.base]);self.assertEqual(r['counts']['NOT_RUN'],100);self.assertFalse(r['execution_complete'])
 def test_wrong_commit_rejected(self):
  b=copy.deepcopy(self.base);b['commit']='old';r=self.run_report([b]);self.assertTrue(r['errors']);self.assertEqual(r['status'],'NOT_ACCEPTED')
 def test_manual_auto_pass_rejected(self):
  b=copy.deepcopy(self.base);q=self.questions[0];b['results']=[{'id':q['id'],'input':q['prompt'],'status':'PASS'}]
  with self.assertRaises(ValueError):self.run_report([b])
 def test_duplicate_results_rejected(self):
  b=copy.deepcopy(self.base);q=self.questions[0];b['results']=[{'id':q['id'],'input':q['prompt'],'status':'MANUAL_REVIEW'}]
  with self.assertRaises(ValueError):self.run_report([b,b])
 def test_manual_completion_is_not_acceptance(self):
  b=copy.deepcopy(self.base);b['results']=[{'id':q['id'],'input':q['prompt'],'status':'MANUAL_REVIEW' if q['kind']=='manual' else 'FAIL'} for q in self.questions]
  r=self.run_report([b]);self.assertTrue(r['execution_complete']);self.assertEqual(r['status'],'NOT_ACCEPTED');self.assertGreater(r['counts']['MANUAL_REVIEW'],0)
 def test_coverage_and_frozen_contracts(self):
  self.assertEqual(len(self.questions),100);self.assertEqual(len({q['prompt'] for q in self.questions}),100)
  self.assertTrue({'tool','cancel','conversation','number','formula','manual'} <= {q['kind'] for q in self.questions})
  self.assertTrue(all(q['rubric'] and q['timeout_ms']>0 for q in self.questions))
if __name__=='__main__':unittest.main()
