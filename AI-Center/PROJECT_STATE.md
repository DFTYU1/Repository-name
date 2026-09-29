# 唯一当前断点 · 2026-09-29 · Run32与内部APK交付

当前证据源码：c650ec9a644dadc5cc41ab571f8afc3b4263e205。Run32 https://github.com/DFTYU1/Repository-name/actions/runs/36584118980 已FAIL。手机10项UI全部PASS，平板启动失败，10项全部NOT_RUN。tablet-emulator.log明确报告KVM权限不足；不是本轮安装空间错误。手机/data可用7522869248字节，需求4642565975字节；宿主机可用75804385280字节。平板未到安装阶段，无容量实测。Artifact11042316262，SHA256 e56bb2e11e92166a3ef7ad5ffec507989c237c680ecfa48d7fc50ce4ac1df701。

本批：每个profile启动前重新检查/修复临时runner KVM ACL并记录前后结果；新增应用APK交付校验与元数据、真机清单。实际APK必须含ARM64 libaicenter.so及哈希正确的内置模型；应用包与测试包分离。合成包3种场景及项目8项检查通过；真实Android修复效果NOT_RUN。

交付阻塞：Run32只保存诊断，无可下载APK。当前公开仓库此前被明确禁止公开APK/模型；已准备私有仓库自动保存流程，公开下载未授权，未上传APK。待确认下载访问权限后开启构建后独立保存。暂无实际APK链接，不宣称已交付。真机均NOT_RUN，53人工题待审核，Q013/Q073重复未解决，Phase1未完成。

下步：新Run确认KVM及双端10项；独立推进内部包下载权限与实际二进制校验。旧Library副本不代表当前Git断点。

## 历史：Run31

项目：personal-ai-center-20260909。分支：ai-center-build。Run31源码提交：`b401bfa0e9735127a743d3a063e492a1c3c7e963`。

## Run31真实结果
Run31 https://github.com/DFTYU1/Repository-name/actions/runs/36556762569 于2026-09-29T11:00:33Z结束FAIL。核心34项、工具夹具69项、报告11项、项目验证7项、签名APK/测试APK、lint及手机基础离线功能PASS。

手机完整10项生产聊天UI全部PASS：百分比、缺参Cpk、均值、小数求和、多轮Cpk、除零、歧义、混合单位、有效公式结构和无效范围均符合契约。平板在安装应用APK前失败，错误为`INSTALL_FAILED_INSUFFICIENT_STORAGE`，因此平板10项全部NOT_RUN；不得沿用手机结果或Run29平板结果。

已确认直接失败原因是平板阶段的PackageManager可用空间不足：1368488537字节APK已完整push，随后在`Performing Push Install`阶段返回`INSTALL_FAILED_INSUFFICIENT_STORAGE: Failed to override installation location`。Run31复用了手机阶段的同一AVD并使用`--no-streaming`；但Run31未记录`df /data`，因此不能虚构失败瞬间的精确剩余字节。当前修复为手机、平板分别新建隔离AVD，将数据分区设为8192MB，改用ADB streaming安装，并在安装前保存设备`/data`、宿主机空间、APK大小及保守需求依据；不足时在安装前明确失败。未改生产逻辑、题库或评分。

Artifact `AI-Center-diagnostics-31`，ID `11028806904`，ZIP SHA256 `e225fcaa0782b46527abda4cdfbc7c5244128d28e2c43f361aebaabe5cd0a342`，有效至2026-10-13T11:00:02Z。应用APK SHA256 `da6d98b8128f20d75c2b6aeb76b275809d85ba212ab1ad35635813401d8db5d8`；测试APK SHA256 `92caefb6b9598ab5829cdefcbf4f4b496acbf22edbadd5e1720f3858c76cee6f`；均signed，仅USE_BIOMETRIC、无INTERNET。

本批本地已通过34核心、69工具/公式断言、原生取消/计时测试、11报告测试和8项项目验证；新增验证确保每个profile使用独立新AVD、大APK使用streaming、AVD扩容且保存容量诊断。真实Android结果仍须由下一Run确认。

下一Run仍执行双端完整10项；重点确认平板安装成功并完成10项，手机已通过项不得退化。冻结裸模型100题基线仍为Run18双端各25 PASS / 22 FAIL / 53 MANUAL_REVIEW / 0 NOT_RUN，工具结果不得覆盖。53人工题保持PENDING_MANUAL_REVIEW；vivo X300 Pro与Lenovo Y900 NOT_RUN；Phase1未完成。

提交前复查远程SHA与活跃Run，单一写入者、仅快进、不强推。持久恢复以Git源码和本文件为准，旧文件副本不得覆盖当前工程。
