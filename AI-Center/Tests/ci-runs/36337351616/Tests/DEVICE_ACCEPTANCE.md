## 当前有效断点 · 2026-09-28 Run12

project_id=personal-ai-center-20260909

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
