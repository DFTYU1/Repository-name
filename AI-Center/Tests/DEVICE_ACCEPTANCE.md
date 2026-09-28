## 当前有效断点 · 2026-09-28 Run13

project_id=personal-ai-center-20260909

- 最新提交`649c54c717974e460ec514227dd735a44707e96f`，Run14 `36341960326`当前QUEUED；新APK、代表题、手机/平板和100题均NOT_RUN。
- Run13 `36337351616`在Android测试源码编译阶段FAIL；手机、平板、manual代表题、conversation代表题和全量100题均NOT_RUN，不存在Run13的PASS/FAIL/MANUAL_REVIEW统计。
- 核心34项、专业报告11项、项目验证6项PASS；缺失`GenerationBudget`导入的最小修复待下一Run实际验证。
- 本轮无APK或测试APK；Artifact 10937503049，ZIP SHA-256 `a252b3c73c82a4df9c79df5b6fa8c7f3d7f39ae8434d1479e0ee2b0b0436f075`，证据保存于`Tests/ci-runs/36337351616/`。
- Run12双模拟器30/50/20/0仍仅是历史基线，不能当作Run13结果。vivo X300 Pro与Lenovo Y900仍NOT_RUN。

## 历史状态

## 当前有效断点 · 2026-09-28 Run12

project_id=personal-ai-center-20260909

- 最新提交`0ae280b8fa61558c96b5ef1ea6d94d0bdbdc6f89`，Run13 `36337351616`已排队；代表题与全量结果尚未取得。
- Run12 `36327194525` 已结束FAIL：API35 x86_64手机、平板模拟器均30 PASS / 50 FAIL / 20 MANUAL_REVIEW / 0 NOT_RUN，专业状态NOT_ACCEPTED；0超时、0崩溃。
- 基础Android、离线中文/英文/QE/Excel、真实工具、取消恢复、界面渲染均PASS；手机和平板截图已目视核对，无明显裁切或布局错位。
- 35项仍达到256 tokens，另15项为4公式、10数值、1连续对话错误。原因是远程提交遗漏调用方，Run12没有验证512-token预算或生产历史传递；不得声称修复无效或已通过。
- APK 1368423001字节，SHA-256 `789bb1e7db2a49c61b485a4ca20ac26e0ec99250487b293b16e76ef8a10431ad`，已签名，仅USE_BIOMETRIC、无INTERNET。Artifact 10936458455证据在`Tests/ci-runs/36327194525/`。
- 新批次先验证manual与conversation代表题，通过后才执行全量100题。vivo X300 Pro与Lenovo Y900仍NOT_RUN，人工题未经复核不得PASS。

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

# 当前设备验收

Run11于2026-09-27正确FAIL：API35 x86_64手机、平板模拟器均30 PASS / 50 FAIL / 20 MANUAL_REVIEW / 0 NOT_RUN，专业状态NOT_ACCEPTED；基础离线回归全部PASS，0超时、0崩溃。证据在`Tests/ci-runs/36312388759/`，Artifact 10932545265有效至2026-10-11。

Run12 `36327194525`正在验证生产聊天历史和512-token长回答预算；当前无新Android结果。vivo X300 Pro与Lenovo Y900仍NOT_RUN。

Run9基础功能验证通过；Run10真实100题已执行但未通过验收。Android构建、签名、安装、双布局、离线推理有真实模拟器证据；真机尚无证据。Run11尚在执行。

Run10 APK大小1368423001字节，SHA-256 c02d91cf4dc9a83fe292d6a37fe09736c99a55619929887f21afa9003d295f01。两ABI，已签名，无INTERNET。诊断产物AI-Center-diagnostics-10保留至2026-10-11。

仍需验证：真实设备、原位数据库升级、长期记忆、embedding及混合RAG、缓存配置与调度、数据库写失败恢复、磁盘不足与模型损坏恢复。禁止用模拟器结果代替真机。
## 当前有效断点 · 2026-09-28 Run14

project_id=personal-ai-center-20260909

- Run14 `36341960326`结束FAIL：核心34项、专业报告11项、项目验证6项、签名APK/测试APK、lint及基础Android离线验证PASS；专业门禁NOT_ACCEPTED。
- 手机与平板API35 x86_64均31 PASS / 38 FAIL / 31 MANUAL_REVIEW / 0 NOT_RUN；0超时、0崩溃。人工题未经复核不计PASS。
- Q001代表manual以512预算生成314 tokens且未耗尽；Q100 conversation PASS。全量中24道manual仍恰好耗尽512并截断，另4公式、10数值题FAIL。
- 手机TTFT均值10190.25ms、4.411 tokens/s、峰值PSS 1439770KiB；平板10141.45ms、4.440 tokens/s、峰值PSS 1436653KiB。
- APK 1368423001字节，SHA-256 `3303280e0d0ce04e9f5f9bb2a3c5582e8a31f122285c8879f7c854dc8cc181ff`；Artifact 10944040093证据在`Tests/ci-runs/36341960326/`。
- 下一Run使用768通用manual预算并以最复杂manual先行验证；冻结题库/答案/评分不变。vivo X300 Pro与Lenovo Y900仍NOT_RUN。

## 历史状态
## 当前有效断点 · 2026-09-28 Run15

project_id=personal-ai-center-20260909

- Run15 `36357175571`结束FAIL：34核心、11报告、6验证、签名APK/测试APK、lint和基础手机/平板离线回归PASS。
- 最复杂manual代表题Q025在手机和平板均使用768预算、生成768 tokens后截断；状态FAIL，0超时、0应用崩溃。
- conversation代表题与全量100题均NOT_RUN；Run14的31/38/31/0只能作为历史基线，不能当作Run15结果。
- APK 1368423001字节，SHA-256 `3f41c729ae453ba24e12554229d88f35a11686e175bb938770964f69d64995fb`；Artifact 10944975450证据在`Tests/ci-runs/36357175571/`。
- 下一Run使用1024通用manual预算与紧凑多部分回答提示，仍先跑Q025和conversation代表题。vivo X300 Pro与Lenovo Y900仍NOT_RUN。

## 历史状态

## 当前有效断点 · 2026-09-28 Run16

project_id=personal-ai-center-20260909

- Run16 `36361478841`结束FAIL：34核心、11报告、6验证、签名APK/测试APK、lint及基础手机/平板离线回归PASS。
- 手机/平板Q025均使用1024预算并生成1024 tokens后截断；手机TTFT 10560.46ms、6.50 tokens/s、峰值1419031KiB，平板TTFT 10275.79ms、7.22 tokens/s、峰值1420525KiB；0超时、0崩溃。
- conversation代表题与全量100题NOT_RUN；不得把Run14的31/38/31/0当作Run16结果。
- APK 1368423001字节，SHA-256 `2b27603540bbbda4f21d9bdbc453f1a56d26a361ae1b4caaac23553f1d24f454`；Artifact 10946207474证据在`Tests/ci-runs/36361478841/`。
- 下一Run先验证双语紧凑提示下的Q025；vivo X300 Pro与Lenovo Y900仍NOT_RUN。

## 历史状态

## 当前有效断点 · 2026-09-28 Run17

project_id=personal-ai-center-20260909

- Run17 `36363582769`结束FAIL；构建、签名、lint、基础双模拟器与本地门禁PASS。
- Q025手机/平板均278 tokens完整结束、MANUAL_REVIEW；Q100手机PASS，平板实际含K73和42但被Unicode词边界误判。
- 手机100题11 PASS / 35 FAIL / 54 MANUAL_REVIEW / 0 NOT_RUN，1超时、0崩溃；平板100题NOT_RUN。
- APK 1368423001字节，SHA-256 `3f16fb4f15771e2e85391a355093b7333bdfeceb9ba1983e16d18a99ca1a50a0`；Artifact 10948562460。
- 下一Run验证精确输出、Q100边界及Q013超时风险；vivo X300 Pro与Lenovo Y900仍NOT_RUN。

## 历史状态
