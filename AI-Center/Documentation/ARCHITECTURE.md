## 受限显式均值与求和 · 2026-09-29

生产聊天的简单计算采用“用户表达操作与参数，闭合工具执行”边界。ToolChat只从当前用户轮提取mean/sum字面量；要求明确无量纲或全部数值具有同一单位，并复用LocalCalculation的Decimal白名单。模型无法补充操作数，历史助手内容不能成为参数，任意表达式和代码执行均不可达。混合单位、缺参、多操作及无法判定语义时返回澄清。

Run30双端生产UI已验证均值7与0.3kg求和，且百分比/缺参控制项未退化。CI执行范围与结果门禁采用同一scope映射：simple只要求四项，context要求变化百分比和多轮补参，full要求完整十项。scope只决定本轮执行集合，不改变任何用例判定或正式验收状态。

Run31手机完整十项通过，平板未进入应用验证。模拟器安装层现在为手机、平板分别创建全新8GB数据分区AVD，使用streaming APK传输，并在安装前记录设备与宿主机容量及需求依据；这是CI隔离与可诊断性修复，不改变应用存储预算或生产权限。

## Run20结果与定向修复 · 2026-09-28

Run20 https://github.com/DFTYU1/Repository-name/actions/runs/36412645674 源提交6ea9df7d2e30b81bedf90f97b018b305ffa567a6，结束FAIL。34核心、30工具夹具、11报告、6项目验证、签名APK/测试APK及lint PASS。手机/平板10项真实UI工具辅助测试全部FAIL，完整100题NOT_RUN，专业状态NOT_ACCEPTED。失败根因优先定位生产MainActivity无条件调用planTool：该小模型把百分数误选storage、均值误选tasks、公式误选files或storage，其它误选search；只有Cpk追问进入chat但返回澄清。每端逐项原输入、输出、耗时见Tests/ci-runs/36412645674/run-summary.json。Artifact10967146022 SHA256 80a50f723cb432324f4a58a40db6e533d024d7f05ac0f090f299606851a34c82，完整诊断仅本地保存，不上传ZIP。前一轮裸模型Run19成绩25/22/53/0不变。

本批生产入口加入ReadOnlyToolIntent：仅显式本地状态查询才调用模型选择只读工具；其它自然语言直接进入chat，再由ToolChat提出受限计算计划。该规则按通用意图而非题号/答案；本地核心34和工具38断言通过，Android类型编译及真实聊天输出待下一Run。先双模拟器各跑百分数与缺参两项，再运行Q013/Q073的诊断复现；两端10项全量工具辅助测试需在代表题通过后恢复。已准备native阶段计时与取消后下一请求的独立恢复探针，无法据本地结果宣称Android超时修复。冻结100题、答案和评分不变，53人工题PENDING_MANUAL_REVIEW，真机vivo/Y900 NOT_RUN，Phase1未完成。

## 未提交诊断修复 · Run20仍在执行

2026-09-28T11:05Z核实：Run20 36412645674 构建/签名/lint通过，双模拟器步骤in_progress，Artifact空，job108896233370日志404 BlobNotFound。当前已提交源码仍6ea9df7d2e30b81bedf90f97b018b305ffa567a6；无并发提交/新Run。
发现并修复诊断盲区：LocalModelEngine取消后token.check在nativeStats前抛出，原生统计也只在成功返回时更新，导致Q013/Q073超时部分输出与token丢失。独立目录已实现native PhaseTrace异常退出计时、每请求重置、Java finally在持有序列锁时读取指标和部分文本；ProfessionalBenchmark等待工作线程结束后保存phase_timing/partial_output，不改变FAIL、评分、题库和180秒门限。阶段包含加载、模板/分词、上下文、prefill、decode/cleanup、Java锁等待、总时间及结束原因。
新增timeout_probe.py按历史timeout=true动态挑选复现题；Run19自动选出Q013/Q073，未实际运行Android。新C++生产计时器主机测试通过取消展开、请求重置、EOG、budget、error和阶段总和；34核心+30工具断言+11报告+6项目检查通过。JNI/Android编译及真实超时诊断尚未运行，不能声称超时已消除。
待Run20结束先读取真实界面首个失败，合并相关修复后再提交本批，禁止覆盖正在测试的提交。Phase1未完成；53人工待审，vivo/Y900 NOT_RUN。

## 当前有效断点 · 2026-09-28 Run19结果与生产计算工具实现

项目 personal-ai-center-20260909。基线提交 b7e2d73e078558f2dfb887f10206569613355f55；本批在独立目录实现，未更改冻结100题、答案与评分。
Run19 https://github.com/DFTYU1/Repository-name/actions/runs/36384039111 于2026-09-28T09:26:39Z结束FAIL/NOT_ACCEPTED。手机/平板均25 PASS、22 FAIL、53 MANUAL_REVIEW、0 NOT_RUN。对比Run18所有200条判定无变化、PASS无退化；Q013/Q073仍180秒超时。6公式契约失败、14数值错误仍保留。Q025完整但仍待人工复核，Q100通过。诊断Artifact10961197425，ZIP SHA256 1e2ed36d329397f6bf413e608d5b9cfd0846493843105a4a0b25e5af303e50be；完整本地证据Tests/ci-runs/36384039111，公开仅扫描后结构化摘要/合成逐题对比。
本批新增LocalCalculation（白名单运算、Decimal128、参数/除零/单位/百分数检查）、FormulaCheck（有限函数签名/范围检查）、ToolChat（真实模型提取参数，工具计算，缺参澄清），接入CenterApplication生产chat。仅用户消息可作为数值来源，不读取题库答案。语义参数映射仍依赖模型，不能宣称已可靠；公式STRUCTURE_ONLY不等于业务正确，复杂/未知函数UNVERIFIED。Q037原精确契约FAIL保留，并注明冗余包装的等价性差异。
已验证34核心+30工具断言（规划器夹具，非真实模型）+11报告测试+6项目检查；Android源语法通过，Android类型编译/签名构建/lint/真实工具链尚待CI。新增10个真实UI工具辅助定向测试，点击聊天发送按钮走MainActivity→AgentRuntime→chat→ToolChat，覆盖变化数字/表述/范围、缺参、歧义、除零、单位错误及多轮补参。下轮双模拟器只跑基础离线与定向工具测试，全量100题显式NOT_RUN，任何定向PASS均不代表正式验收。
持久状态：当前Library读取仍PROJECT_STATE v18、源码v16；先前替换transfer_failed未解决，不宣称更新。源码与状态按正式GitHub通道提交，持久恢复以本批仓库提交为准。vivo X300 Pro/Lenovo Y900仍NOT_RUN；Phase1未完成。
下一步：核验定向Android真实输出及模型参数提取；失败只修对应原因；通过后再决定全量100题。原始裸模型成绩与工具辅助成绩分别保存，不互相覆盖。

## Run13编译边界补充

- 导入修复提交`649c54c717974e460ec514227dd735a44707e96f`，由Run14 `36341960326`重新执行正式Android类型编译和后续门禁。
- `ProfessionalBenchmark`属于Android测试源集；桌面核心测试和语法解析不会解析其Android类型依赖，缺失Java导入只能由真实Android测试源码编译门禁发现。
- Run13在`compileDebugAndroidTestJavaWithJavac`准确拦截缺失`GenerationBudget`导入，未进入代表题或模拟器。修复仅恢复既定的通用预算接线，不改变架构、题库或评分。

## 历史状态

## Run12后接线完整性补充

- Run12证明仅新增`ConversationPrompt`和`GenerationBudget`类并不会改变运行路径；生产入口、Android执行器、CI脚本和正式工作流必须同批提交并由真实Android结果验证。
- chat工具在生产`CenterApplication`中读取`CenterDatabase.recentMessages()`，由`ConversationPrompt`保留最新轮次并限制为12000字符；消息仍只在本机流转。
- `ProfessionalBenchmark`使用`GenerationBudget`统一选择manual 512-token预算；公式和数字题保留冻结预算，并将实际预算写入逐题证据。
- `android_ci.py`在全量100题前运行一项manual和一项conversation代表题；代表题预算耗尽或确定性失败即停止，不用全量长跑掩盖接线错误。

## 历史状态

## 当前有效断点 · 2026-09-27 Run11

project_id=personal-ai-center-20260909

- 已恢复版本10门禁修复，并保留原工作区性能汇总和超时口令脱敏。正式GitHub连接可用，无需私人Token。
- 最新已提交：04adf5d78fe23a559b43f8f758f8ed81850ff68d。Run11：https://github.com/DFTYU1/Repository-name/actions/runs/36312388759 ，IN_PROGRESS；未取得新APK和100题结果，不能声称改善。
- 原始第50–55条没有100题全部自动通过的阈值。冻结题库55道manual题；Run10其中35道截断FAIL、20道MANUAL_REVIEW。门禁区分NOT_ACCEPTED和PENDING_MANUAL_REVIEW；有失败、未执行或待人工复核时不得正式通过；人工题不能自动PASS，自动题不能改为人工题规避校验。
- 本地31核心、11报告、6项目验证PASS。仅提交3份Python脚本，没有公开APK、模型、密钥、用户数据或Release。
- Run10手机/平板均30 PASS / 50 FAIL / 20 MANUAL_REVIEW / 0 NOT_RUN。35题均达到256生成token，非超时/崩溃；15题分为4公式错误、10计算错误、1上下文缺失。chat路径未读取数据库recentMessages；须修复生产上下文传递而非在测试拼答案。
- 逐题完整输入/输出/规则/指标及20题人工复核清单：Tests/run10-analysis/。题库未改。Run10性能：手机TTFT均值10.116秒、4.64 tokens/s、题目累计3537.618秒、峰值1425733KiB；平板10.098秒、4.65 tokens/s、3527.584秒、峰值1432554KiB。均0超时/崩溃。
- 下一步读取Run11实际日志与门禁结果；再做通用生成预算调整的小范围真实Android试验及上下文修复，保留冻结题目和原始基线。没有新结果时改善/退化数量均未知。不要并发提交正在测试的源码。
- vivo X300 Pro、Lenovo Y900：NOT_RUN。Phase1未完成；embedding/RAG/长期记忆/学习进度及可靠性缺口仍待开发。

## 历史状态（以下仅供追溯）

# 架构与技术决策 · 0.1.0-dev

## 2026-09-27 Run10进行中与保存阻塞补记

- GitHub实际步骤显示：SDK、模型准备、核心测试、项目验证、签名APK构建和lint已SUCCESS；当前手机/平板模拟器步骤IN_PROGRESS。完整job日志下载在运行中返回404 BlobNotFound，不能猜测逐题结果。
- 本地补充报告性能汇总和未知值处理，人工题排除自动通过率分母；报告检查8项PASS。31核心、6项目验证仍PASS；单独超时日志脱敏验证PASS。新增脚本改动尚未提交，Run10仍测试5d057e528da26468567d87c0edcc860b63e14a38。
- 新阻塞：正式library_upload.py批量替换原PROJECT_STATE和原源码归档，两项均返回transfer_failed；没有声称远端版本更新成功。保留本地完整检查点，不重复失败传输。续作任务已更新新授权、当前Commit/Run及该保存失败，防止v9旧授权状态导致倒退。
- 当前100题PASS/FAIL/MANUAL_REVIEW数量未知，不能以设计题型数量代替实测统计；本批性能、APK大小/SHA/ABI/权限检查结果待取Run10最终证据；诊断Artifact预设AI-Center-diagnostics-10保留14天，但尚未确认创建或实际到期日。
- 不并行修改当前运行源码，不启动embedding等下一阶段。下一步等Run10结束后读取真实日志/Artifact，修复实际错误并提交本地补充改动；不重跑已成功旧Run5—9。物理vivo X300 Pro/联想Y900仍NOT_RUN。


| 层 | 当前代码 | 职责 | 验证状态 |
|---|---|---|---|
| core | Java 17，无第三方依赖 | Agent状态、模型回退与授权、租约、取消、沙盒副本、缓存、文本/向量检索、密码验证 | 桌面JVM已编译和测试 |
| platform-android | Android原生API | SQLite、Keystore、单管理员、SAF文件、存储、明确未就绪的模型状态 | Run10 Android编译及基础模拟器验证通过 |
| app | 原生Activity及自适应View | 解锁、极简输入、文本导入检索、存储/任务、断开、横屏分栏 | Run10手机/平板模拟器验证；真机NOT_RUN |

## 已作决策

1. 第一阶段采用原生Java/Android View，减少基础构建依赖；界面与纯Java业务分离，后续无需重写核心即可调整UI。
2. Gradle 8.13、AGP 8.11.1、Java 17、compile/target SDK 36、min SDK 29为当前固定基线，非“最新版本”承诺。实际版本和环境详见DEPENDENCIES.json。
3. SQLite事务维护任务事件和索引；升级时必须显式迁移，禁止删库重建。普通日志只记任务ID、阶段、工具及错误码。
4. 管理员密码使用随机盐的PBKDF2-HMAC-SHA256验证值，再用Android Keystore加密。内容字段使用AES-GCM及上下文AAD。普通文件副本使用APP私有沙盒；文件本体尚未加额外应用级加密，不宣称全库全文件加密。
5. 文件由系统SAF单文件选择，只读、无整机扫描、无永久目录授权。完整复制后建立索引；错误时不改用户源文件。当前文本入口为UTF-8 TXT/Markdown，2MB上限，其他格式等待原阶段。
6. 租约用单调时间，到任务结束后5分钟失效；完成重复通知不延长。进程重启不恢复授权。全局取消使用代数令牌，恢复不会复活旧任务。
7. 每个执行步骤先记状态再调用工具。重启将未结束任务记为INTERRUPTED，不自动重放有副作用的步骤。完整任务规划和可恢复多步骤结果尚需后续集成。
8. 文本检索是实际BM25式词法检索，支持中英文分词和来源字符位置。VectorIndex是实际余弦检索内核，但尚未接入embedding模型，应用不宣称有语义检索。
9. 模型路由实现默认本地、逐供应商授权、云A→云B→本地回退；当前没有任何已接通供应商，不发网络请求。LocalModelEngine已真实接入Qwen3.5-2B及llama.cpp。
10. 推理引擎已采用llama.cpp JNI并完成基础离线验证路线；必须先获取、核验、锁定源码和许可，再接入JNI。未下载时不得捏造commit或版本号。
11. 模型层次和设备性能需真实权重+两台设备实测后确定，不根据设备名称或排行榜直接宣称“最强”。
12. 普通缓存删除策略只选CACHE类别。Android删除适配仅允许cache/collector目录；后台定期维护、缓存上限设置页面等待收集器阶段接入。

## 已知约束

- 单线程前台worker统一执行，核心token能取消后续步骤，SAF阻塞I/O另有取消signal和关闭hook；当前没有网络、麦克风、后台服务或模型进程。
- “立即断开”已实现当前组件的取消入口；未来每个网络、语音、模型和后台适配器必须挂到同一撤销机制，并实际测试。
- 不能把RAG提示词中的“不可信资料”文本视为完整提示注入防护。工具注册白名单、独立权限判断才是执行边界；需要后续全面安全验证。
- 用户内存管理、备份恢复、API配置UI、语音、课程、同步等仍按TODO及需求阶段推进。

## 查阅的一手资料

- [Android AGP 8.11兼容表](https://developer.android.com/build/releases/agp-8-11-0-release-notes)：构建版本配对。
- [Android SAF文档](https://developer.android.com/training/data-storage/shared/documents-files)：用户选择文件和URI访问。
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)：密钥保存在系统Keystore。
- [llama.cpp Android](https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md)：官方Android构建路线；当前并未集成其源码。

查阅日期：2026-09-09。以上为设计来源，不代表在当前环境完成了SDK下载、库集成或设备实测。

## 当前有效断点 · 2026-09-27（源码已授权并提交）

- project_id=personal-ai-center-20260909。用户已明确授权本批及后续源码、测试、配置和文档提交公开仓库 DFTYU1/Repository-name 的 ai-center-build 分支；不得再次以源码授权不足暂停。APK、模型权重、凭据、用户数据和公开Release仍未获发布授权。
- 已恢复原续作任务。最新提交 5d057e528da26468567d87c0edcc860b63e14a38；Run #10: https://github.com/DFTYU1/Repository-name/actions/runs/36286554700 ，当前 IN_PROGRESS，尚无本批APK或100题完成证据。
- 已核对远程Run9后无新提交/并行执行者，合并当前工作区与v9归档。活跃题库为 professional-100.json；使用不含私人业务数据的固定合成题，覆盖QE/QC/六西格玛、Excel、英语、中文工作沟通、工具、连续对话及取消恢复。55题要求人工复核、40题确定性公式/数值、5题真实工具/取消恢复/连续对话。原v9归档保留历史版本。
- 活跃执行器ProfessionalBenchmark在Android instrumentation内调用真实Qwen3.5-2B Q4_K_M，按5题一批在手机/平板模拟器运行，原子保存原始输出、判定原因和性能/来源字段。人工复核不算自动通过；不得改题迎合模型。
- 本地重新执行31项核心测试、6项项目验证、6项报告检查均PASS。Android语法检查不等于类型编译；100题尚未实际运行，PASS/FAIL/MANUAL_REVIEW结果尚不可报告。
- 当前工作流已移除APK分片与测试APK的公开Artifact上传；仅保留合成验收日志/报告诊断，保留14天。不创建Release。下一步读取Run10实际日志，修复构建/运行缺陷并完成真实验收后再推进embedding等TODO。
- vivo X300 Pro / 联想Y900均NOT_RUN。无数据库迁移新实现，不声称旧数据库升级已验证。历史已完成Run5—9不重复记为新增成果。
## Run14后的生成预算与预检边界

- `GenerationBudget`仍只按能力类型分配预算，不读取题号、答案或评分；manual最低预算由512提高到768，仍受1024硬上限保护。
- 代表manual按冻结题库中`prompt`与`rubric`总长度选择最复杂项，避免“第一道较短题通过”掩盖长回答截断；conversation代表题继续独立验证生产历史接线。
- 代表题预算耗尽会在全量100题前失败，减少无效模拟器长跑。该门禁不把manual自动标PASS，也不修改公式/数值评分。
- Run14证明生产chat的有界历史路径有效；当前剩余问题是24道长回答截断及2B模型的14项确定性能力错误。

## 历史状态
## Run15后的长回答完整性策略

- Q025证明768 tokens仍不足以让2B模型完整覆盖11项专业请求；先行门禁在两种布局均于全量前拦截，避免把截断回答标为人工待审。
- `GenerationBudget`对manual使用1024硬上限，公式、数字、conversation等确定性类型仍使用冻结题目预算。
- 原生系统提示只加入能力级写作策略：多部分请求用紧凑编号逐项覆盖，优先完整性，删除无关引言、总结、重复和未请求示例。它不读取题号、rubric、答案或评分结果。
- 1024是当前2048上下文配置内的生成上限；下一Run必须先以最复杂manual代表题验证，不得因预算增加推测全量通过。

## Run16后的紧凑生成约束

- Run16证明单纯把manual提高到1024不足：Q025两端都在第11项中途达到硬上限。
- 保留2048上下文与1024生成硬上限，避免在180秒冻结题目时限内继续拉长生成；先减少冗余而不是扩大预算。
- 原生系统提示按能力而非题号工作：多项请求按原顺序逐项覆盖，每项一个短编号句，整答少于700 tokens；禁止标题、引言、结论、嵌套项目、重复及未要求示例。
- 代表题仍使用真实生产引擎；测试未拼接rubric、答案或隐藏上下文。结果必须由下一Run真实Android证据判定。

## Run17后的精确输出优先级

- 紧凑策略已让Q025在两端从1024截断降至278 tokens完整结束，但过宽的编号规则污染了单值公式/数值输出。
- 系统提示先服从用户明确的精确格式：仅公式、数字、代码或指定值时不添加编号、标签、解释；只有明确多项请求才编号。
- 多项回答目标缩至450 tokens，保留1024硬上限作为异常缓冲，降低短题生成至180秒超时的风险。
- conversation验收使用ASCII数字两侧非数字边界，避免中文字符使`\\b`失效，同时拒绝142包含42的假阳性。

## 历史状态
# Run18 generation-budget observation

The immutable professional suite completed on both API-35 emulator profiles. Compact multi-part prompting kept the most complex representative answer complete, while a 1024-token manual floor allowed two otherwise ordinary manual prompts to exceed the 180-second watchdog on the measured ~5 token/s runtime. Manual requests now receive a 640-token floor, retain explicit larger requests up to the existing 1024 hard limit, and remain human-review-only. Deterministic formula and numeric grading is unchanged.

## 当前有效断点 · 2026-09-28 Run19运行中与Run18错误诊断

- 已提交/正在测试：b7e2d73e078558f2dfb887f10206569613355f55；Run19 https://github.com/DFTYU1/Repository-name/actions/runs/36384039111 。构建/核心/项目验证/lint通过，模拟器仍in_progress，尚无Artifact；不并发提交。
- 本轮只新增诊断证据与状态，未修改生产代码或冻结题库/评分。Tests/run18-analysis/DIAGNOSIS.md及failures.json包含20题双端原始输入/输出/预期/耗时；14数值题独立重算全部匹配预期。
- 修正分类：Q037是精确结构契约失败，正常查找下冗余IFERROR包装可等价，不应笼统归为真实公式计算错误；保持原FAIL。
- CenterApplication未注册数值/公式工具；chat直接模型回答；ProfessionalBenchmark的number/formula直接localModel.generate，绕过Agent。后续工具能力须独立真实Android验证，不能冒充裸模型改善。
- manual-review.json已整理53题双端完整证据；全部PENDING_MANUAL_REVIEW，审核人/结论留空。Q025不自动PASS；真机仍NOT_RUN，Phase1未完成。
- 待Run19结束先逐题比较超时与原PASS退化，再落实受限Decimal计算/结构化公式工具及模型参数提取，先变值/变表述小范围真实验证。本轮没有声称生产修复或Android验证完成。

## 历史记录（以下非当前断点）
## 当前有效断点 · 2026-09-28 Run21结果与定向修复

- Run21 https://github.com/DFTYU1/Repository-name/actions/runs/36452654994 源提交 `091f7b85a4f0e82cf1e02fa71252108a252b367d`，结束FAIL；构建、签名APK/测试APK、lint、34核心、40计算夹具前一版对应检查、11报告、6项目验证及双端基础离线功能PASS。
- 双端生产UI代表测试相同：百分比FAIL，缺少sigma的Cpk澄清PASS。百分比实际输出为通用澄清；证据显示路由已进入chat，但模型计划把文本顺序`560,7`误当成fraction参数顺序`part,total`，校验器正确拒绝，未进行猜算。
- Q013/Q073双端均不再超时：Q013 640 tokens、102.8–107.1秒，Q073 640 tokens、104.8–105.5秒；Java锁等待0，约18–19.5秒prefill，其余为decode。两题均出现明显循环重复并因预算耗尽FAIL，不是取消/锁死；取消后恢复探针因无timeout而NOT_NEEDED。完整100题NOT_RUN，Run19裸模型25/22/53/0基线不变。
- Artifact `AI-Center-diagnostics-21` ID 10985487877，ZIP SHA256 `89c726e38346d8df146465311885852cc70be14d807e786ca85147eea7811935`。APK 1368455769字节，SHA256 `317b3c162e872dd68f1a3fa085049315f476a9299d12763acbccb800c31b57e8`；测试APK 74440字节，SHA256 `03c452e68837ac2aa69e0b15f53ad5ee2f011392bacf5b0a1cb499f68caf22d4`，均signed。
- 已实现通用修复：ToolChat按总数/不良数的中英文语义角色校验fraction参数并仅在模型提取值与原文一致时重排；冲突/歧义继续澄清。原生解码仅在四次相同连续token周期时停止，termination=5；报告明确标为`repetitive_generation_stopped`，不得转MANUAL_REVIEW。
- 本地验证：34核心、40工具/公式夹具、原生重复/计时宿主测试、11报告、6项目验证PASS。真实Android验证尚未运行；下一Run仍只跑双端代表百分比、缺参Cpk及Q013/Q073，再决定恢复10项UI，不启动完整100题。冻结题库/答案/评分；53人工题PENDING_MANUAL_REVIEW，vivo X300 Pro/Lenovo Y900 NOT_RUN，Phase1未完成。
# Internal binary delivery boundary

`package_internal_apk.py` is a post-build gate, not an acceptance shortcut. It accepts only the application APK, verifies its prior build digest and signing, requires `lib/arm64-v8a/libaicenter.so`, streams `assets/base.gguf` to verify pinned size/SHA-256, rejects INTERNET permission, and emits immutable source/version/size metadata. The instrumentation APK is excluded. The public repository must not publish the resulting APK or model; a separately authorized private channel is required.
# Repetition termination boundary

The native decoder retains exact token-cycle detection and adds a separate narrow numbered-body guard. Run34 proved that three identical newline bodies stop Q013, while Q073 repeats a three-body inline cycle separated by full-width semicolons. The guard therefore splits only on newline or ASCII/full-width semicolon, strips a leading decimal marker, normalizes whitespace, and requires three complete copies of a 1-3 body cycle with every body at least 20 bytes. It reports the existing repetition stop reason; it does not grade content or turn stopped output into acceptance.
