## Run38已验证 · 2026-10-03

双端10项生产UI各10 PASS，无缺失或退化；快速校验全部小于0.30秒。专业探针与完整100题确实NOT_RUN。Run37误触发的Q013/Q073保留实际FAIL，不以范围声明覆盖实际结果。当前源码dc9b1857，正式Phase1仍未验收。

# 变更记录

## 2026-10-03 Run37定向门禁状态隔离

- 手机、平板完整10项生产工具聊天UI均PASS。
- 5项确定性校验真实耗时145–348ms，满足10秒契约。
- 关闭专业基准时不再继承旧timeout baseline或用Q013/Q073覆盖定向门禁状态。
- Q013/Q073和冻结100题继续作为独立裸模型证据。

## 2026-10-03 确定性校验绕过无必要模型等待

- `ToolChat`对缺参Cpk、明确除零、带歧义标记的百分比和单一显式公式检查直接使用受限本地判定；不能完全确定时仍走模型规划器。
- 公式只接受单一、无嵌套的显式候选；多个公式返回澄清，`STRUCTURE_ONLY`边界不变。
- Android UI给5项确定性用例增加10秒延迟契约；新增6项不同数值、英文和多公式主机回归。
- 保存Run36真实摘要；未改变冻结题库、答案、评分、Q013/Q073 FAIL或人工复核状态。

## 2026-10-03 Run35周期重复Android验证
- 双端10项生产工具聊天UI继续全部PASS，无缺失或退化。
- Q013双端143/640 tokens受控终止；Q073双端149/640 tokens受控终止，不再耗尽640预算。
- 两题内容仍错误，保持FAIL；未修改冻结题库、答案或评分，也未把资源控制写成内容通过。
- 保存Run35 Artifact、APK、容量及逐题阶段计时的结构化摘要；完整100题和真机本轮NOT_RUN。

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

# Run34 cycle repetition follow-up

- Preserve Run34 evidence: both profiles retained 10/10 production tool-chat UI PASS.
- Confirm Q013 is stopped at 143 tokens but remains FAIL because the answer is incorrect/incomplete.
- Classify Q073 as an inline three-body cycle separated by full-width semicolons; the previous identical-line guard did not match it.
- Parse numbered items across newline and ASCII/full-width semicolons, and stop only after three copies of a 1-3 body cycle with long bodies.
- Add Q073-shaped cycle, normal supplier-change list, two-cycle, short-list and interrupted-sequence regressions. Frozen questions, answers and grading are unchanged.
