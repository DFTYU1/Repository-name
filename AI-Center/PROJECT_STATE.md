# 唯一当前断点 · 2026-10-03 · Run36复核与确定性校验延迟修复候选

Run36 https://github.com/DFTYU1/Repository-name/actions/runs/37078519125 在文档证据提交`b17a1e3ee44b28a630d8032e4e3e30a178dc1235`上结束FAIL/NOT_ACCEPTED。它再次证明双端生产聊天工具UI各10 PASS；Q013为143/640、Q073为149/640，双端均因`repetitive_generation_stopped`受控结束但内容继续FAIL。完整100题NOT_RUN，Run18裸模型25/22/53/0基线不变。Artifact ID11258507146，ZIP SHA256 `4a56f8949894624fd95dbeecbcfc011cceb652fc1f2786686ec55f500bd19891`。

Run36暴露的当前候选修复是确定性校验延迟：缺参Cpk、除零、歧义百分比、有效/无效公式虽判定正确，但双端均等待本地模型约76–82秒。`ToolChat`现仅对可由当前用户字面输入完全判定的请求直接调用受限本地校验；未知语义仍回退规划器。新增不同数值、英文表述、多公式歧义6项回归，主机34核心、75工具/公式、11报告、8项目验证、原生与内部包检查PASS。真实Android尚NOT_RUN；下一提交需双端10项验证上述5项在10秒契约内且其余5项不退化。

两台真机与53道人工作答仍未验收，Phase1未完成。

---

# 历史断点 · Run35周期重复资源控制已验证

Run35 https://github.com/DFTYU1/Repository-name/actions/runs/37071859617 在提交`c071495412c8d48d45c443834a05a126be2e8c1f`上结束FAIL/NOT_ACCEPTED。构建、34核心、69工具夹具、11报告、项目验证、签名APK/测试APK、lint、ARM64与内置模型校验PASS；手机和平板生产聊天工具UI继续各10 PASS/0 FAIL/0 NOT_RUN。

Q013双端均在143/640 tokens因`repetitive_generation_stopped`结束，分别59.293/62.410秒；Q073双端均在149/640 tokens因同一原因结束，分别58.949/62.077秒。Run34的Q073曾耗尽640 tokens并用112–115秒，因此周期3、中文分号列表的资源失控已被修复。两题输出内容仍不符合冻结标准，继续FAIL；受控终止不是答案验收。完整100题本轮NOT_RUN，Run18裸模型基线25 PASS/22 FAIL/53 MANUAL_REVIEW/0 NOT_RUN不变。

Run35应用APK 1368570457字节，SHA256 `e9b6bfbcf4d4de02930d012de059dd8ae7c48c7ce6eee9da860b0f777054ca73`；测试APK 76484字节，SHA256 `970425ed311939a05dc1ba9c17b19ebb9c3d22b3cd98c8760e8816e979ca743d`，均签名。Artifact `AI-Center-diagnostics-35` ID11256570364，ZIP SHA256 `c76fcf283c461aad123810dff54181c3c305d4dbbe487d181e889adb3da647fc`，有效至2026-10-16T22:57:03Z。

下一步不再原样重跑或堆叠提示词。把Q013/Q073记录为当前2B裸模型内容能力失败，继续推进可泛化的离线知识检索/结构化质量知识能力；任何工具辅助结果与裸模型成绩分开。vivo X300 Pro、Lenovo Y900仍NOT_RUN，53人工题待审，Phase1未完成。

---

## Run34与修复背景

Run34 https://github.com/DFTYU1/Repository-name/actions/runs/36634979802 在提交`b178f84a6cef79e404d8717c6a21fbf56540311e`上结束FAIL。构建、34核心、69工具夹具、11报告、项目验证、签名APK/测试APK、lint、ARM64/内置模型校验均PASS；手机和平板生产聊天工具UI仍各10 PASS/0 FAIL/0 NOT_RUN，无退化。完整100题NOT_RUN，Run18裸模型基线25 PASS/22 FAIL/53 MANUAL_REVIEW/0 NOT_RUN不变。

Q013双端被新重复保护在143/640 tokens终止，分别40.376/44.138秒，结束原因为`repetitive_generation_stopped`；资源失控已改善，但内容不满足冻结rubric，仍FAIL。Q073双端仍640/640 tokens预算耗尽，分别112.177/114.583秒。真实输出从第11项起按“变更合同签署日期→地点→签署人”周期3重复；旧保护只识别连续相同正文且只按换行切项，因此漏掉同一行中文分号分隔的周期3循环。

本批候选修复把编号正文解析扩展到换行、ASCII分号和中文全角分号；仅在1–3项正文周期完整重复3轮、且每项至少20字节时终止。新增真实Q073形态、正常8项变更通知、仅重复2轮、短值和中断序列回归。主机重复保护、34核心、69工具夹具、11报告、项目验证及内部包合成检查PASS；真实Android结果仍NOT_RUN，不能宣称Q073已修复。

Run34应用APK `0.2.0-offline-dev`（versionCode 2）1368570457字节，SHA256 `3d0cd312427655da5b7394db5b6522a3630c45904e7fa4291631cf1268bf56ae`，签名通过，仅USE_BIOMETRIC、无INTERNET，含ARM64库和已校验内置模型；仍因公开仓库安全规则没有下载链接。Artifact11064304460，ZIP SHA256 `fb29a45331a597e88b5f1a52d72edcb40eef79b31676a74aa1128a1a7a4c884a`，有效至2026-10-13T22:12:12Z。vivo X300 Pro、Lenovo Y900仍NOT_RUN；53人工题仍待审核；正式状态NOT_ACCEPTED，Phase1未完成。

下一步：提交前再次确认远程仍为`b178f84...`且无活跃Run；提交本候选后只跑双端10项及Q013/Q073定向Android验证，不跑完整100题。受控停止仍不得把错误答案改为PASS。

Run33 https://github.com/DFTYU1/Repository-name/actions/runs/36620165787 在提交`c664379e7cb4bfed9761d4311d0b49ecb4731df6`上SUCCESS。手机/平板各自新建8GiB API35 x86_64 AVD，生产聊天工具UI均10 PASS/0 FAIL/0 NOT_RUN；基础离线中英、QE、Excel、工具、取消恢复也PASS。平板KVM权限由启动前不可用变为ACL修复后可用。手机/平板/data可用分别7503470592/7506661376字节，均高于4642565975字节安装门槛。

应用APK `0.2.0-offline-dev`（versionCode 2）1368488537字节，SHA256 `97e0b87f6337f7202cdeb8fe1d760023b9eb056740ddefc7b439b53002d8ee0a`，签名通过，含ARM64 `libaicenter.so`/`libc++_shared.so`及哈希正确的内置Qwen3.5-2B模型；建议真机空余5923172363字节。公开仓库的APK上传按规则跳过，因此没有下载链接，不宣称已交付。

Run33 Artifact11059338393，ZIP SHA256 `2315b9aeefdda3c674b0e49b8b55cbc3ca5c86229f42b051a3a8bdae0724e064`。裸模型100题、Q013/Q073、两台真机均NOT_RUN；53人工题待审，正式状态NOT_ACCEPTED，Phase1未完成。

本批针对Q013/Q073新增窄范围生产重复保护：仅当连续3个、至少20字节的编号项去除变化编号后正文完全相同才停止；普通不同正文列表、短值列表、中断序列不触发。原token精确周期检测保留。主机合成回归通过；真实模型/Android结果NOT_RUN。下一Run仅在双端10项后追加既有Q013/Q073定向探针，不执行完整100题。

## 历史：Run32与内部APK交付

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
