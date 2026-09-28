import pathlib,json,re,decimal,hashlib
p=pathlib.Path(__file__).resolve().parents[2]; out=p/'Tests/run18-analysis';out.mkdir(exist_ok=True)
profiles={}
for profile in ('phone','tablet'):
 rows={}
 for f in (p/'Tests/ci-runs/36370715194/raw/Tests').glob(profile+'-professional-*.json'):
  if 'representative' in f.name or 'summary' in f.name:continue
  for r in json.loads(f.read_text()).get('results',[]):rows[r['id']]=r
 profiles[profile]=rows
assert all(len(r)==100 for r in profiles.values())
D=decimal.Decimal
classifications={ 'Q027':'函数名错误：AVG不是Excel AVERAGE', 'Q028':'函数语义错误：COUNTIF文本条件不能统计数字单元格', 'Q034':'参数角色颠倒：条件范围与求和范围交换', 'Q035':'多条件聚合理解错误：SUMIF代替SUMIFS且参数不合法', 'Q037':'精确结构契约不匹配：IFERROR冗余封装；正常查找结果等价，不能称为真实数值错误', 'Q038':'返回值语义错误：INDEX返回单元格值而非MATCH位置'}
checks=[]; failures=[]; manual=[]
for key,r in sorted(profiles['phone'].items()):
 t=profiles['tablet'][key]
 if r['status']=='MANUAL_REVIEW':
  manual.append({'id':key,'status':'PENDING_MANUAL_REVIEW','input':r['input'],'rubric':r['rubric'],'phone':r,'tablet':t,'reviewer':None,'reviewed_at':None,'decision':None,'notes':''})
 if r['status']!='FAIL' or r.get('timeout'):continue
 kind=r['expected_rule']['kind']; calc=None
 if kind=='number':
  s=r['input']
  if 'Cpk' in s:
   vals=[D(x) for x in re.findall(r'=(\d+(?:\.\d+)?)',s)]
   l,u,m,sd=vals;calc=min((u-m)/(3*sd),(m-l)/(3*sd));label='Cpk公式应用或算术错误；仅有最终数值，无法区分模型内部推导'
  elif '不合格品率' in s:
   total,bad=[D(x) for x in re.findall(r'(\d+)件',s)];calc=100*bad/total;label='百分比数量级错误：实际输出为正确值的10倍'
  elif '算术平均' in s:
   vals=[D(x.strip()) for x in re.search(r'\[([^]]+)\]',s)[1].split(',')];calc=sum(vals)/len(vals);label='算术聚合错误：均值'
  else:
   vals=[D(x) for x in re.findall(r'A\d+=(\d+)',s)];calc=sum(vals);label='算术聚合错误：单元格SUM'
  assert abs(calc-D(str(r['expected_rule']['expected'])))<=D(str(r['expected_rule']['tolerance']))
  checks.append({'id':key,'independent_result':str(calc),'expected_matches':True})
 else:label=classifications[key]
 failures.append({'id':key,'category':label,'input':r['input'],'expected_rule':r['expected_rule'],'independent_numeric_result':str(calc) if calc is not None else None,'phone':r,'tablet':t,'same_output':r['output']==t['output']})
assert len(failures)==20 and len(checks)==14 and len(manual)==53
(out/'failures.json').write_text(json.dumps(failures,ensure_ascii=False,indent=2))
(out/'manual-review.json').write_text(json.dumps(manual,ensure_ascii=False,indent=2))
(out/'independent-numeric-checks.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2))
lines=['# Run18公式与数值错误诊断','', '基线提交46b4d73c08cb3a96997ce78712ad864e058e69ee；双端各25 PASS / 22 FAIL / 53 MANUAL_REVIEW。此报告不更改历史评分。','', '|题目|分类|实际输出（手机/平板）|冻结预期|','|---|---|---|---|']
for r in failures:
 esc=lambda x:str(x).replace('|','\\|').replace('\n',' ')
 lines.append('|'+ '|'.join(map(esc,[r['id'],r['category'],r['phone']['output']+' / '+r['tablet']['output'],r['expected_rule']['expected']]))+'|')
lines+=['','14道数值题已从输入独立重算，与冻结预期及容差一致；没有发现这14道题的判定器误判。6道公式按现有精确结构契约均FAIL。Q037需单独理解：冗余包装在正常查找下等价，但遇其他错误时不保证等价，保持精确契约FAIL。','', '## 生产代码定位','', '- app/src/main/java/local/aicenter/app/CenterApplication.java：onCreate仅注册storage/files/tasks/chat/search；chat直接models.answer；planTool枚举无计算或公式工具。','- core/src/main/java/local/aicenter/core/AgentRuntime.java：通用执行器已有限步、取消、权限及日志，但尚无计算实现注册。','- app/src/androidTest/java/local/aicenter/app/ProfessionalBenchmark.java：answer最后直接localModel.generate处理manual/formula/number，只有tool/conversation使用Agent。因此单独增加聊天工具不会改善现有裸模型基线，不能混报两条路径。','- 同文件grade：number要求纯数值及容差；formula只忽略字符串外空白/大小写。不应把通用AcceptanceEvaluator.firstNumber误认为这里实际评分路径。','- platform-android/src/main/cpp/engine.cpp：生产路径传system到ChatML，预填空think区，EOG停止；generated统计输出token。这20题输出已结束且非预算失败，没有证据表明靠提高token能修复。','', '## 待Run19结束落实的通用方案','', '1. 增加独立、受限的数值工具：Decimal算术、mean/sum、percentage、Cp/Cpk。模型输出有类型操作与参数；校验有限数、非空数据、sigma>0、USL>LSL、0<=不良数<=样本数；禁止eval和任意代码执行。','2. 通过Agent注册计算工具，模型仅负责选择操作和提取参数，工具返回精确值及来源。精确数值请求直接格式化工具结果，避免二次模型改写；缺参拒算并澄清。取消与失败必须传递。','3. Excel工具用结构化表达式树和函数签名校验、范围角色及维度校验，支持COUNT/AVERAGE/SUMIF/SUMIFS/MATCH/XLOOKUP；语法合法不能证明语义正确，需核对用户请求中的范围和操作。','4. 保留冻结裸模型结果与评分；增加独立的生产Agent针对性测试，不偷换历史基线。不同数值、范围、语序、中英文表述及负例，记录模型计划、工具参数、结果与最终输出；禁止传expected/rubric/id到生产。','5. 先真实Android小范围验证Cpk/百分比/均值/SUM及六类公式，连同原PASS回归；通过后再双端100题。Run19结果到齐先逐题比较，不重复启动。','', '## 验证状态','', '已完成20题双端证据归类、14题独立重算、生产调用链定位。生产修复与不同表述的真实模型验证尚未执行。53题清单在manual-review.json，包含两端完整输入/输出/耗时/原始rubric，审核人和决定为空，全部PENDING_MANUAL_REVIEW。']
(out/'DIAGNOSIS.md').write_text('\n'.join(lines)+'\n')
print(json.dumps({'failures':len(failures),'independently_recomputed':len(checks),'manual_pending':len(manual),'both_profiles':True}))
