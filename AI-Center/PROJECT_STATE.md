# 唯一当前断点 · 2026-09-29 · 聊天参数归属修复

项目：personal-ai-center-20260909。分支：ai-center-build。
本批基于 a5247569593ec097734aef672279b7e822131379，以包含本文件的 Git 提交为准；未取得本批 Android 结果前不得声称通过。
历史记录保留于 Git：https://github.com/DFTYU1/Repository-name/tree/ba192dbb3233434e6c269f45276f2e0a9e8ef855/AI-Center 。旧提交中的“当前/等待/禁止并发”只描述当时状态，不作为当前指令。

## 最近已完成的 CI 与 Android 结果
Run26 https://github.com/DFTYU1/Repository-name/actions/runs/36516188083 已结束 FAIL。核心编译报 ToolChat.java:38 cannot find symbol: latest，Android NOT_RUN。本批保留该提交新增的两项回归场景，适配显式当前消息接口，消除未定义变量。

Run25 https://github.com/DFTYU1/Repository-name/actions/runs/36503692854 于 2026-09-29T01:11:03Z 结束 FAIL。核心、构建、签名、lint PASS。手机/平板百分比题返回澄清，随后缺参 Cpk 却返回旧题 1.25%，两项都 FAIL。
Artifact 11006893932，SHA256 cecad447ad4ec67ce0cc430555aef06f2bac84052667c12cf769f0a45374a72d，已读取并核对。
Q013/Q073 双端无超时、无崩溃、Java 锁等待 0；约159–162秒，640 tokens预算耗尽，重复内容仍 FAIL。不是正在等待 Run20；Run20 已于 2026-09-28T11:36:21Z 结束。

## 本批修改及验证范围
生产 chat 直接传入当前消息与结构化历史。CalculationRequest 默认只允许当前问题提供参数；明确补充才选择相关用户消息，不接收助手生成数字、不解析 user: 文本边界。
ToolChat 修复不良数角色正则误跨到总数、支持数字后单位；明确计数百分比走受限 Decimal 工具，不能冒充模型算术成绩。缺少明确组内标准差/稳定过程信息继续澄清。
本地：34 核心测试、60 工具断言、11 报告测试、6 项目验证 PASS；包括旧故障复现、串题拒绝、带单位数字、明确 Cpk 补参与混合单位拒绝。规划器夹具不代表真实模型。
本批 CI 保留双端基础检查，执行4项界面回归：百分比、随后缺参 Cpk、变数字百分比、跨题明确补充 Cpk。Q013/Q073 本批 NOT_RUN，保留已知 FAIL；完整100题 NOT_RUN。

## 接续规则
先查远程分支 SHA 与匹配该 SHA 的真实 Run，不凭旧文字认定仍在运行。Run完成立即读取产物；运行中可以独立准备修复，提交前复查远程、单一写入者、不强推。
同一修复假设连续两轮无改善，不原样重跑；记录否定证据再改变方案。失败、待人工审核、未执行分别记录，不降低冻结题库及验收要求。
下一步读取本批4项双端真实输出。全部通过后再恢复10项工具UI；失败只修首个可复现根因。重复生成独立处理，不增加预算掩盖。
53人工题 PENDING_MANUAL_REVIEW；vivo X300 Pro/Lenovo Y900 NOT_RUN；Phase1 未完成，不进入 Phase2。
持久恢复以 Git 源码提交为准；先前文件副本可能过期，不据此回退源码。正式Git归档可恢复完整已跟踪源码；本地检查点不宣称已上传。
