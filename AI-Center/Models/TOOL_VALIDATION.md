## Run36延迟证据与候选修复 · 2026-10-03

Run36双端10项均PASS，但缺参Cpk、除零、歧义百分比和两项公式检查各耗时约76–82秒，证明正确结果仍被模型规划延迟。候选修复仅在当前用户字面输入足以判定时走本地闭合校验，并给真实UI增加10秒上限；主机75项断言通过，Android尚待验证。多个公式、嵌套公式和未知语义不得猜测；`STRUCTURE_ONLY`仍不等于业务正确。

## Run29证据与当前边界 · 2026-09-29

Run29双端各8/10 PASS。模型对明确mean与同单位小数加法仍返回CLARIFY；本地Decimal实现本身正确。当前修复允许生产聊天在严格验证当前消息后直接执行mean/sum：操作意图唯一、至少两个字面量、明确无量纲或每个数值具相同支持单位。语法匹配不代表一般数学语义理解；不满足条件继续澄清。该应用工具成绩不得覆盖裸模型100题。

## Run30真实生产UI · 2026-09-29

手机/平板API35 x86_64的simple范围均4/4 PASS。英文mean(-4,8,17)返回7；0.1kg+0.2kg返回0.3kg；百分比控制返回1.25；缺少标准差的Cpk返回澄清。Run30工作流FAIL来自范围解析器多要求未选择的followup，不是工具输出失败。完整10项及裸模型100题NOT_RUN，不能据此扩大结论。

## Run31完整工具UI · 2026-09-29

手机API35 x86_64完整10/10 PASS，覆盖确定性计算、缺参/歧义/除零/单位拒绝、多轮Cpk与公式结构边界。平板因安装空间不足全部NOT_RUN；不得据手机结果推断平板通过。裸模型100题仍未执行，Run18基线不变。

## Run20结果与定向修复 · 2026-09-28

Run20 https://github.com/DFTYU1/Repository-name/actions/runs/36412645674 源提交6ea9df7d2e30b81bedf90f97b018b305ffa567a6，结束FAIL。34核心、30工具夹具、11报告、6项目验证、签名APK/测试APK及lint PASS。手机/平板10项真实UI工具辅助测试全部FAIL，完整100题NOT_RUN，专业状态NOT_ACCEPTED。失败根因优先定位生产MainActivity无条件调用planTool：该小模型把百分数误选storage、均值误选tasks、公式误选files或storage，其它误选search；只有Cpk追问进入chat但返回澄清。每端逐项原输入、输出、耗时见Tests/ci-runs/36412645674/run-summary.json。Artifact10967146022 SHA256 80a50f723cb432324f4a58a40db6e533d024d7f05ac0f090f299606851a34c82，完整诊断仅本地保存，不上传ZIP。前一轮裸模型Run19成绩25/22/53/0不变。

本批生产入口加入ReadOnlyToolIntent：仅显式本地状态查询才调用模型选择只读工具；其它自然语言直接进入chat，再由ToolChat提出受限计算计划。该规则按通用意图而非题号/答案；本地核心34和工具38断言通过，Android类型编译及真实聊天输出待下一Run。先双模拟器各跑百分数与缺参两项，再运行Q013/Q073的诊断复现；两端10项全量工具辅助测试需在代表题通过后恢复。已准备native阶段计时与取消后下一请求的独立恢复探针，无法据本地结果宣称Android超时修复。冻结100题、答案和评分不变，53人工题PENDING_MANUAL_REVIEW，真机vivo/Y900 NOT_RUN，Phase1未完成。

## 未提交诊断修复 · Run20仍在执行

2026-09-28T11:05Z核实：Run20 36412645674 构建/签名/lint通过，双模拟器步骤in_progress，Artifact空，job108896233370日志404 BlobNotFound。当前已提交源码仍6ea9df7d2e30b81bedf90f97b018b305ffa567a6；无并发提交/新Run。
发现并修复诊断盲区：LocalModelEngine取消后token.check在nativeStats前抛出，原生统计也只在成功返回时更新，导致Q013/Q073超时部分输出与token丢失。独立目录已实现native PhaseTrace异常退出计时、每请求重置、Java finally在持有序列锁时读取指标和部分文本；ProfessionalBenchmark等待工作线程结束后保存phase_timing/partial_output，不改变FAIL、评分、题库和180秒门限。阶段包含加载、模板/分词、上下文、prefill、decode/cleanup、Java锁等待、总时间及结束原因。
新增timeout_probe.py按历史timeout=true动态挑选复现题；Run19自动选出Q013/Q073，未实际运行Android。新C++生产计时器主机测试通过取消展开、请求重置、EOG、budget、error和阶段总和；34核心+30工具断言+11报告+6项目检查通过。JNI/Android编译及真实超时诊断尚未运行，不能声称超时已消除。
待Run20结束先读取真实界面首个失败，合并相关修复后再提交本批，禁止覆盖正在测试的提交。Phase1未完成；53人工待审，vivo/Y900 NOT_RUN。

# 工具辅助模型验证

仍使用既有固定Qwen3.5-2B-Q4_K_M及原生引擎锁定版本。新增生产规划器调用使用256-token预算，不修改裸模型100题参数。确定性计算不等于模型理解正确。真实UI参数提取、普通聊天回退延迟和多轮歧义处理尚待定向Android验证。Run19原始双端成绩25/22/53/0保留。
## 当前有效断点 · 2026-09-28 Run21结果与定向修复

- Run21 https://github.com/DFTYU1/Repository-name/actions/runs/36452654994 源提交 `091f7b85a4f0e82cf1e02fa71252108a252b367d`，结束FAIL；构建、签名APK/测试APK、lint、34核心、40计算夹具前一版对应检查、11报告、6项目验证及双端基础离线功能PASS。
- 双端生产UI代表测试相同：百分比FAIL，缺少sigma的Cpk澄清PASS。百分比实际输出为通用澄清；证据显示路由已进入chat，但模型计划把文本顺序`560,7`误当成fraction参数顺序`part,total`，校验器正确拒绝，未进行猜算。
- Q013/Q073双端均不再超时：Q013 640 tokens、102.8–107.1秒，Q073 640 tokens、104.8–105.5秒；Java锁等待0，约18–19.5秒prefill，其余为decode。两题均出现明显循环重复并因预算耗尽FAIL，不是取消/锁死；取消后恢复探针因无timeout而NOT_NEEDED。完整100题NOT_RUN，Run19裸模型25/22/53/0基线不变。
- Artifact `AI-Center-diagnostics-21` ID 10985487877，ZIP SHA256 `89c726e38346d8df146465311885852cc70be14d807e786ca85147eea7811935`。APK 1368455769字节，SHA256 `317b3c162e872dd68f1a3fa085049315f476a9299d12763acbccb800c31b57e8`；测试APK 74440字节，SHA256 `03c452e68837ac2aa69e0b15f53ad5ee2f011392bacf5b0a1cb499f68caf22d4`，均signed。
- 已实现通用修复：ToolChat按总数/不良数的中英文语义角色校验fraction参数并仅在模型提取值与原文一致时重排；冲突/歧义继续澄清。原生解码仅在四次相同连续token周期时停止，termination=5；报告明确标为`repetitive_generation_stopped`，不得转MANUAL_REVIEW。
- 本地验证：34核心、40工具/公式夹具、原生重复/计时宿主测试、11报告、6项目验证PASS。真实Android验证尚未运行；下一Run仍只跑双端代表百分比、缺参Cpk及Q013/Q073，再决定恢复10项UI，不启动完整100题。冻结题库/答案/评分；53人工题PENDING_MANUAL_REVIEW，vivo X300 Pro/Lenovo Y900 NOT_RUN，Phase1未完成。
# Run32 binary gate

Tool-assisted UI evidence remains separate from the raw-model benchmark. An installable internal application must additionally prove ARM64 engine presence and the exact pinned bundled model hash. This packaging proof does not convert any raw-model FAIL or MANUAL_REVIEW to PASS.
# Run33 separation

Double-emulator tool-assisted UI: 10/10 PASS per profile. Raw-model 100 cases and Q013/Q073 were NOT_RUN and retain their prior evidence. The new repetition guard is a resource/termination control only; stopped but incomplete or incorrect content remains FAIL under frozen grading.

# Run34 targeted repetition evidence

Both profiles retained 10/10 tool-assisted UI PASS. Raw-model Q013 stopped at 143 tokens with termination=5 and remained FAIL. Q073 repeated a three-item sequence in one full-width-semicolon-separated line and exhausted 640 tokens with termination=2. The next candidate detects only three full repetitions of a 1-3 long-body cycle; frozen scoring is unchanged and full 100 remains NOT_RUN.
# Run35 raw-model repetition evidence

Q013 and Q073 are raw-model probes, not tool-assisted scores. Both terminated for repetition on phone and tablet without timeout or budget exhaustion, but both remain FAIL on content. This evidence must not be counted as a tool-chat PASS or used to overwrite the Run18 raw-model baseline.
