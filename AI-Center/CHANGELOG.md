# 变更记录

## 2026-09-29 Run31平板安装存储修复
- 手机完整10项生产聊天UI全部PASS；平板在APK安装阶段因模拟器存储不足而NOT_RUN。
- 手机、平板各自新建隔离AVD，数据分区显式设为8192MB；大APK改用ADB streaming安装，避免设备端临时保存第二份1.368GB APK。
- 安装前输出设备`/data`、宿主机空间、APK大小和保守容量需求；不足时在安装前失败并保留证据。
- 新增每个profile的`df /data`和卸载结果日志，并加入项目验证，生产逻辑、冻结题库及评分未改。

## 2026-09-29 Run30定向Android与CI范围修复
- 双端4项真实生产UI全部PASS：百分比、缺参Cpk、无量纲均值、同单位小数求和。
- Run30整体失败只因simple模式未选择followup而解析器仍无条件要求；未发生生产功能失败。
- android_ci按simple/context/full分别构造预期测试集，并加入项目验证回归，防止再次出现“所选用例全过但范围门禁失败”。
- 完整10项与裸模型100题本轮NOT_RUN；冻结题库、答案、评分及53题人工状态不变。

## 2026-09-29 Run29均值与同单位求和
- 读取Run29完整Artifact：双端10项各8 PASS / 2 FAIL；均值和0.1kg+0.2kg均被模型保守返回澄清。
- ToolChat在模型规划前增加闭合的显式mean/sum路径，只读取当前消息的字面量与单位。
- 无量纲必须明确声明；物理数值必须全部带相同单位；混合单位、缺参、Cpk/百分比等其它族不进入此路径。
- 新增变数、不同表述、混合单位和缺参测试，工具断言增至69；冻结题库、答案和评分未改。
- CI缩小为两项失败加两个控制项的双端Android门禁；通过后才恢复10项。

项目：personal-ai-center-20260909。分支：ai-center-build。
本批基于 a5247569593ec097734aef672279b7e822131379，以包含本文件的 Git 提交为准；未取得本批 Android 结果前不得声称通过。
历史记录保留于 Git：https://github.com/DFTYU1/Repository-name/tree/ba192dbb3233434e6c269f45276f2e0a9e8ef855/AI-Center 。旧提交中的“当前/等待/禁止并发”只描述当时状态，不作为当前指令。

## 2026-09-29 聊天请求归属修复
- 用 CalculationRequest 传递当前消息与相关用户历史，删除错误的 latestTurn 字符串查找。
- 当前问题默认隔离；明确补参保留相应任务，助手回答不提供计算参数。
- 百分比角色提取不跨越角色标签，明确计数由受限工具执行；数字允许携带单位，混合计量单位继续拒绝。
- 补真实生产序列化边界/参数来源回归，工具断言44→60；双端定向UI 2→4。
- 本轮停止重跑与此修复无关的超时探针，显式NOT_RUN并保留旧FAIL；冻结100题/评分/预算不变。
- 五份状态改为当前摘要，旧记录全部保留在父提交历史，避免过期“运行中/未提交”被当成当前任务。
# Run32 delivery preparation

- Preserved Run32 evidence: phone 10 UI PASS; tablet 10 NOT_RUN due to KVM permission failure before boot.
- Recheck ephemeral-runner KVM ACL before every isolated emulator profile.
- Add application-only internal delivery staging with signature, version, SHA-256, ARM64 library and pinned model validation.
- Add first-use storage basis and physical-device checklist; no APK/model is published by this change.
# Run33 follow-up

- Record successful double-emulator 10-case production UI result and verified ARM64 application metadata.
- Stop three consecutive long numbered items only when their normalized bodies are identical despite changing numbers.
- Preserve the existing exact-token cycle guard and add normal-list/short-list/interruption regression cases.
- Add Q013/Q073 targeted probes; no frozen question, answer or grading change.
