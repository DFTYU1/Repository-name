> 2026-09-28更新：以下为Run18原始诊断。Run19双端判定无变化。上述计划已实现受限计算及有限单层公式检查并接入生产chat；34核心+30工具断言通过；真实Android UI验证待执行，详见Documentation/TOOL_ASSISTED_CHAT.md。未实现通用公式业务语义证明，不应据此声称六道公式能力错误已解决。

# Run18公式与数值错误诊断

基线提交46b4d73c08cb3a96997ce78712ad864e058e69ee；双端各25 PASS / 22 FAIL / 53 MANUAL_REVIEW。此报告不更改历史评分。

|题目|分类|实际输出（手机/平板）|冻结预期|
|---|---|---|---|
|Q027|函数名错误：AVG不是Excel AVERAGE|=AVG(B2:B11) / =AVG(B2:B11)|['=AVERAGE(B2:B11)']|
|Q028|函数语义错误：COUNTIF文本条件不能统计数字单元格|=COUNTIF(C2:C20,"数字") / =COUNTIF(C2:C20,"数字")|['=COUNT(C2:C20)']|
|Q034|参数角色颠倒：条件范围与求和范围交换|=SUMIF(B2:B20,"=NG",A2:A20) / =SUMIF(B2:B20,"=NG",A2:A20)|['=SUMIF(A2:A20,"NG",B2:B20)']|
|Q035|多条件聚合理解错误：SUMIF代替SUMIFS且参数不合法|=SUMIF(C2:C20,A2:B20,IF(B2:B20>0,C2:C20,0)) / =SUMIF(C2:C20,A2:B20,IF(B2:B20>0,C2:C20,0))|['=SUMIFS(C2:C20,A2:A20,"A",B2:B20,">0")']|
|Q037|精确结构契约不匹配：IFERROR冗余封装；正常查找结果等价，不能称为真实数值错误|=IFERROR(XLOOKUP(E2,A2:A20,B2:B20,"未找到"),XLOOKUP(E2,A2:A20,B2:B20,"未找到")) / =IFERROR(XLOOKUP(E2,A2:A20,B2:B20,"未找到"),XLOOKUP(E2,A2:A20,B2:B20,"未找到"))|['=XLOOKUP(E2,A2:A20,B2:B20,"未找到")']|
|Q038|返回值语义错误：INDEX返回单元格值而非MATCH位置|=INDEX(A2:A20,MATCH(E2,A2:A20,0)) / =INDEX(A2:A20,MATCH(E2,A2:A20,0))|['=MATCH(E2,A2:A20,0)']|
|Q076|Cpk公式应用或算术错误；仅有最终数值，无法区分模型内部推导|2.66 / 2.66|2|
|Q077|Cpk公式应用或算术错误；仅有最终数值，无法区分模型内部推导|1.0 / 1.0|1.6666666666666667|
|Q078|Cpk公式应用或算术错误；仅有最终数值，无法区分模型内部推导|2.0 / 2.0|1.5|
|Q079|Cpk公式应用或算术错误；仅有最终数值，无法区分模型内部推导|2.66 / 2.66|0.6666666666666666|
|Q080|Cpk公式应用或算术错误；仅有最终数值，无法区分模型内部推导|2.1 / 2.1|1.5|
|Q082|百分比数量级错误：实际输出为正确值的10倍|15 / 15|1.5|
|Q083|百分比数量级错误：实际输出为正确值的10倍|20 / 20|2|
|Q084|百分比数量级错误：实际输出为正确值的10倍|25 / 25|2.5|
|Q085|百分比数量级错误：实际输出为正确值的10倍|15 / 15|1.5|
|Q087|算术聚合错误：均值|13 / 13|12|
|Q088|算术聚合错误：均值|6.5 / 6.5|6|
|Q089|算术聚合错误：均值|33.33333333333333 / 33.33333333333333|30|
|Q093|算术聚合错误：单元格SUM|13 / 13|12|
|Q095|算术聚合错误：单元格SUM|24 / 24|66|

14道数值题已从输入独立重算，与冻结预期及容差一致；没有发现这14道题的判定器误判。6道公式按现有精确结构契约均FAIL。Q037需单独理解：冗余包装在正常查找下等价，但遇其他错误时不保证等价，保持精确契约FAIL。

## 生产代码定位

- app/src/main/java/local/aicenter/app/CenterApplication.java：onCreate仅注册storage/files/tasks/chat/search；chat直接models.answer；planTool枚举无计算或公式工具。
- core/src/main/java/local/aicenter/core/AgentRuntime.java：通用执行器已有限步、取消、权限及日志，但尚无计算实现注册。
- app/src/androidTest/java/local/aicenter/app/ProfessionalBenchmark.java：answer最后直接localModel.generate处理manual/formula/number，只有tool/conversation使用Agent。因此单独增加聊天工具不会改善现有裸模型基线，不能混报两条路径。
- 同文件grade：number要求纯数值及容差；formula只忽略字符串外空白/大小写。不应把通用AcceptanceEvaluator.firstNumber误认为这里实际评分路径。
- platform-android/src/main/cpp/engine.cpp：生产路径传system到ChatML，预填空think区，EOG停止；generated统计输出token。这20题输出已结束且非预算失败，没有证据表明靠提高token能修复。

## 待Run19结束落实的通用方案

1. 增加独立、受限的数值工具：Decimal算术、mean/sum、percentage、Cp/Cpk。模型输出有类型操作与参数；校验有限数、非空数据、sigma>0、USL>LSL、0<=不良数<=样本数；禁止eval和任意代码执行。
2. 通过Agent注册计算工具，模型仅负责选择操作和提取参数，工具返回精确值及来源。精确数值请求直接格式化工具结果，避免二次模型改写；缺参拒算并澄清。取消与失败必须传递。
3. Excel工具用结构化表达式树和函数签名校验、范围角色及维度校验，支持COUNT/AVERAGE/SUMIF/SUMIFS/MATCH/XLOOKUP；语法合法不能证明语义正确，需核对用户请求中的范围和操作。
4. 保留冻结裸模型结果与评分；增加独立的生产Agent针对性测试，不偷换历史基线。不同数值、范围、语序、中英文表述及负例，记录模型计划、工具参数、结果与最终输出；禁止传expected/rubric/id到生产。
5. 先真实Android小范围验证Cpk/百分比/均值/SUM及六类公式，连同原PASS回归；通过后再双端100题。Run19结果到齐先逐题比较，不重复启动。

## 验证状态

已完成20题双端证据归类、14题独立重算、生产调用链定位。生产修复与不同表述的真实模型验证尚未执行。53题清单在manual-review.json，包含两端完整输入/输出/耗时/原始rubric，审核人和决定为空，全部PENDING_MANUAL_REVIEW。
