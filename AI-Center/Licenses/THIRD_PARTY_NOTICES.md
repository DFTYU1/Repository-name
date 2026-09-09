# 依赖与许可证清单

当前工程core没有第三方运行时库，也未分发任何模型权重或llama.cpp源码。当前代码为本项目编写；没有通过此文件替用户擅自选择源码公开许可证。

| 项目 | 用途 | 当前分发状态 | 后续义务 |
|---|---|---|---|
| Android framework | SQLite、SAF、Keystore、UI、生物识别 | 引用设备系统API，没有复制系统源码 | 遵守Android SDK构建工具使用条款 |
| OpenJDK | 当前桌面编译和测试 | 不包含JDK二进制 | 若未来分发JDK，保存实际发行版许可证 |
| Gradle8.13 Wrapper | 官方构建入口 | 已包含官方Wrapper JAR、POSIX/Windows启动脚本，SHA-256已校验 | 全文见Gradle-Wrapper-LICENSE.txt；脚本保留上游版权头 |
| Android Gradle Plugin 8.11.1 / Gradle8.13发行包 | 构建工具 | 尚未下载或分发这些发行包 | 获取正式包后记录许可证/校验值 |
| llama.cpp | 候选本地推理路线 | 尚未下载、复制、编译或分发 | 锁定实际commit，检查该版本LICENSE并保留全文 |
| 本地LLM / embedding / ASR / TTS | 后续模型 | 尚未分发 | 逐模型确认权重许可、再分发权、使用限制与来源 |

禁止把“开源”当作无需检查许可。任何新增GPL/AGPL等依赖必须先检查分发及源码提供义务并记录，不得静默混入不兼容组件。
