# 当前待办

## After Run35

- [x] Verify bounded 1-3-body cycle termination for Q073 on both real emulators without regressing the 10 production UI cases.
- [x] Keep Q013 and Q073 FAIL because their stopped outputs do not satisfy frozen content acceptance.
- [ ] Build a general offline quality-domain retrieval/structured-answer path with provenance and tests using varied wording; do not ingest frozen expected answers as production input.
- [ ] Keep raw-model and application-assisted scores separate.

1. 下一Run复测双端完整10项；核对平板安装前后存储日志以及10项逐项结果。
2. 双端完整10项通过后，再用新数值/表述扩展必要的生产聊天验收；失败只修首个共同根因。
3. 工具应用成绩与冻结裸模型100题分开；不得用工具结果改写Run18的25/22/53/0。
4. 继续人工审核53题；Q025不自动PASS。Q013/Q073重复生成仍是裸模型质量问题。
5. vivo X300 Pro、Lenovo Y900保持NOT_RUN；完成Phase1其余真实功能和设备验收后才进入Phase2。
6. 每批更新五份状态、模型/架构/验收说明，运行checkpoint并以Git持久保存公开源码和合成证据。
# Current gate after Run32

- [ ] Produce and privately deliver the verified ARM64 application APK; do not publish APK/model from this public repository.
- [ ] Confirm per-profile KVM proof and tablet 10-case execution on the next real Android run.
- [ ] Run vivo X300 Pro first, then Lenovo Y900 using `Documentation/REAL_DEVICE_TEST.md`; retain NOT_RUN until evidence exists.
- [ ] Fix Q013/Q073 numbered-body repetition without weakening answer acceptance.
# After Run33

- [x] Confirm isolated phone/tablet capacity, KVM recovery and 10 production tool-chat UI cases.
- [ ] Verify numbered-body repetition guard on real Android Q013/Q073; a controlled stop does not make an incorrect answer PASS.
- [ ] Privately deliver the verified ARM64 application APK and run vivo X300 Pro, then Lenovo Y900.
# After Run34

- [x] Verify that the original same-body guard controls Q013 resource use on both emulators without changing its FAIL grade.
- [ ] Verify the bounded 1-3 body cycle guard against Q073 on both emulators; require 10/10 tool UI to remain PASS.
- [ ] Preserve Q013/Q073 outputs as FAIL unless their content independently satisfies frozen acceptance; a repetition stop is not acceptance.
- [ ] Do not run the full 100 until the targeted Q073 behavior and regression controls are proven.
