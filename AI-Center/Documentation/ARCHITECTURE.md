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
