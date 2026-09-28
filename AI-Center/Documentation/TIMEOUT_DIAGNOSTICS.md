## Run20结果与定向修复 · 2026-09-28

Run20 https://github.com/DFTYU1/Repository-name/actions/runs/36412645674 源提交6ea9df7d2e30b81bedf90f97b018b305ffa567a6，结束FAIL。34核心、30工具夹具、11报告、6项目验证、签名APK/测试APK及lint PASS。手机/平板10项真实UI工具辅助测试全部FAIL，完整100题NOT_RUN，专业状态NOT_ACCEPTED。失败根因优先定位生产MainActivity无条件调用planTool：该小模型把百分数误选storage、均值误选tasks、公式误选files或storage，其它误选search；只有Cpk追问进入chat但返回澄清。每端逐项原输入、输出、耗时见Tests/ci-runs/36412645674/run-summary.json。Artifact10967146022 SHA256 80a50f723cb432324f4a58a40db6e533d024d7f05ac0f090f299606851a34c82，完整诊断仅本地保存，不上传ZIP。前一轮裸模型Run19成绩25/22/53/0不变。

本批生产入口加入ReadOnlyToolIntent：仅显式本地状态查询才调用模型选择只读工具；其它自然语言直接进入chat，再由ToolChat提出受限计算计划。该规则按通用意图而非题号/答案；本地核心34和工具38断言通过，Android类型编译及真实聊天输出待下一Run。先双模拟器各跑百分数与缺参两项，再运行Q013/Q073的诊断复现；两端10项全量工具辅助测试需在代表题通过后恢复。已准备native阶段计时与取消后下一请求的独立恢复探针，无法据本地结果宣称Android超时修复。冻结100题、答案和评分不变，53人工题PENDING_MANUAL_REVIEW，真机vivo/Y900 NOT_RUN，Phase1未完成。

## 2026-09-28 Run20等待期间续作：恢复验证已接线（尚未提交）

远程仍6ea9df7d2e30b81bedf90f97b018b305ffa567a6，Run20双模拟器步骤仍在运行，尚无10项逐项结果。新增ProfessionalBenchmark可选timeout_recovery：超时快照保存后再执行独立90秒/16tokens恢复请求，记录输出、耗时、tokens、termination，恢复成功不改变原题FAIL。CI定向模式新增按Run19历史timeout选择两题；任何超时或恢复失败仍令该轮失败。未降低原180秒门限，未改题库/标准答案/评分。
本地C++计时器测试、3项诊断选择检查及6项项目验证通过；真实Android复现、阻塞阶段及恢复效果均NOT_RUN，待Run20结束后提交执行。阶段日志补丁及恢复检查只保存在当前工作区检查点，尚未远程提交，不能称持久同步成功。

## 未提交诊断修复 · Run20仍在执行

2026-09-28T11:05Z核实：Run20 36412645674 构建/签名/lint通过，双模拟器步骤in_progress，Artifact空，job108896233370日志404 BlobNotFound。当前已提交源码仍6ea9df7d2e30b81bedf90f97b018b305ffa567a6；无并发提交/新Run。
发现并修复诊断盲区：LocalModelEngine取消后token.check在nativeStats前抛出，原生统计也只在成功返回时更新，导致Q013/Q073超时部分输出与token丢失。独立目录已实现native PhaseTrace异常退出计时、每请求重置、Java finally在持有序列锁时读取指标和部分文本；ProfessionalBenchmark等待工作线程结束后保存phase_timing/partial_output，不改变FAIL、评分、题库和180秒门限。阶段包含加载、模板/分词、上下文、prefill、decode/cleanup、Java锁等待、总时间及结束原因。
新增timeout_probe.py按历史timeout=true动态挑选复现题；Run19自动选出Q013/Q073，未实际运行Android。新C++生产计时器主机测试通过取消展开、请求重置、EOG、budget、error和阶段总和；34核心+30工具断言+11报告+6项目检查通过。JNI/Android编译及真实超时诊断尚未运行，不能声称超时已消除。
待Run20结束先读取真实界面首个失败，合并相关修复后再提交本批，禁止覆盖正在测试的提交。Phase1未完成；53人工待审，vivo/Y900 NOT_RUN。


复现入口：scripts/timeout_probe.py --summary Tests/ci-runs/36384039111/raw/Tests/phone-professional-summary.json；默认只打印选择结果。--execute仅允许显式emulator序列号和CI合成fixture密码，凭据不写入日志。两题输入、预算640及180秒门限不变。指标仅在native退出并持有LOCK时读取，禁止跨线程读取非原子统计。
