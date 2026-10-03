# 构建与验证状态

## Run36 final / next candidate

Run36构建、34核心、69工具夹具、11报告、项目验证、签名APK/测试APK、lint、ARM64/模型检查PASS；双端10项工具UI各10 PASS。Q013/Q073内容FAIL导致整体NOT_ACCEPTED。Artifact11258507146，ZIP SHA256 `4a56f8949894624fd95dbeecbcfc011cceb652fc1f2786686ec55f500bd19891`。候选性能修复的主机验证为34核心、75工具/公式、11报告、8项目验证、原生及内部包检查PASS；真实Android NOT_RUN。

## Run35 final

Run35 (`37071859617`, source `c071495412c8d48d45c443834a05a126be2e8c1f`) is FAIL/NOT_ACCEPTED because Q013 and Q073 remain content failures. Build, lint, signed APKs, ARM64/model verification and both profiles' ten production tool-chat UI cases passed. Q013 stopped at 143/640 tokens and Q073 at 149/640 tokens on both profiles with termination reason `repetitive_generation_stopped`; no timeout, crash or budget exhaustion occurred. Full 100 and physical devices were NOT_RUN.

- 最近完成CI：Run31 `36556762569`，结论FAIL；手机完整10项PASS，平板APK安装因模拟器存储不足而NOT_RUN。
- PASS：34核心、69工具夹具、11报告、7项目验证、签名应用/测试APK、lint、手机基础离线功能及手机10项工具UI。
- 平板：`INSTALL_FAILED_INSUFFICIENT_STORAGE`，基础与10项均NOT_RUN。
- 本批基础设施修复后本地：34核心、69工具/公式断言、原生测试、11报告、8项目验证PASS。
- 下一批Android类型编译、APK与双端完整10项：PENDING_CI；本地回归不得冒充平板Android通过。
- 完整裸模型100题、Q013/Q073、本批真机：NOT_RUN。Phase1未完成。
# Run32 / internal APK delivery · 2026-09-29

Run32 (`36584118980`, source `c650ec9a644dadc5cc41ab571f8afc3b4263e205`) built and signed successfully. Phone 10/10 tool-chat UI tests passed. Tablet 10/10 were NOT_RUN because the fresh tablet emulator exited before boot after reporting inaccessible `/dev/kvm`; it never reached capacity check or APK install. Phone `/data` available 7,522,869,248 bytes versus conservative requirement 4,642,565,975 bytes.

The next batch rechecks KVM ACL for each profile and stages a separately verified ARM64 application APK immediately after build. Public-repository artifact upload remains disabled because APK/model publication is not authorized. Physical devices and the produced delivery APK remain NOT_RUN/NOT_DELIVERED until an actual binary is built and privately saved.
# Run33 success / repetition guard candidate

Run33 is SUCCESS: both isolated phone and tablet profiles passed all ten production UI tool cases, capacity gates and KVM proof. The verified application metadata is preserved in `Tests/ci-runs/36620165787/summary.json`; binary download remains unavailable because public APK/model upload was skipped.

The next candidate adds a narrowly bounded numbered-body repetition guard and targeted real-Android Q013/Q073 probes. Host regression is PASS; actual model behavior remains NOT_RUN until that workflow completes.
# Run34 targeted Android result

Run34 (`36634979802`, source `b178f84a6cef79e404d8717c6a21fbf56540311e`) is FAIL/NOT_ACCEPTED. Build, lint, package checks and both profiles' ten production tool-chat UI cases passed. Q013 terminated for repetition at 143 tokens on both profiles but remains content FAIL. Q073 still exhausted 640 tokens because its output is an inline three-body cycle rather than consecutive identical newline bodies.

The next candidate recognizes only complete threefold repeats of 1-3 sufficiently long numbered bodies split by newline or semicolon. Host tests, 34 core, 69 tool fixtures, 11 report tests, project verification and synthetic package verification PASS. Android verification of this candidate is NOT_RUN.
