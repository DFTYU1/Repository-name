# 构建与验证状态

- 最近完成CI：Run31 `36556762569`，结论FAIL；手机完整10项PASS，平板APK安装因模拟器存储不足而NOT_RUN。
- PASS：34核心、69工具夹具、11报告、7项目验证、签名应用/测试APK、lint、手机基础离线功能及手机10项工具UI。
- 平板：`INSTALL_FAILED_INSUFFICIENT_STORAGE`，基础与10项均NOT_RUN。
- 本批基础设施修复后本地：34核心、69工具/公式断言、原生测试、11报告、8项目验证PASS。
- 下一批Android类型编译、APK与双端完整10项：PENDING_CI；本地回归不得冒充平板Android通过。
- 完整裸模型100题、Q013/Q073、本批真机：NOT_RUN。Phase1未完成。
# Run32 / internal APK delivery · 2026-09-29

Run32 (`36584118980`, source `c650ec9a644dadc5cc41ab571f8afc3b4263e205`) built and signed successfully. Phone 10/10 tool-chat UI tests passed. Tablet 10/10 were NOT_RUN because the fresh tablet emulator exited before boot after reporting inaccessible `/dev/kvm`; it never reached capacity check or APK install. Phone `/data` available 7,522,869,248 bytes versus conservative requirement 4,642,565,975 bytes.

The next batch rechecks KVM ACL for each profile and stages a separately verified ARM64 application APK immediately after build. Public-repository artifact upload remains disabled because APK/model publication is not authorized. Physical devices and the produced delivery APK remain NOT_RUN/NOT_DELIVERED until an actual binary is built and privately saved.
