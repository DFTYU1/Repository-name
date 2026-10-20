# 测量系统分析基础实现（开发候选）

入口：更多功能 → 测量系统分析，或输入“打开MSA工具”。全部计算离线，不调用模型，不自动上传数据。固定停止/首页；管理员及停止状态检查；日志仅工具和状态，不记录测量内容。

- 交叉GR&R：2–50零件，2–10操作员，每组合2–20重复，完整平衡、连续重复编号。双因素随机效应ANOVA，可选p≥0.05合并交互项到误差。负分量截0并告警；分别给方差/%贡献/%研究变差（6σ），可选公差宽度、ndc和分量柱图。非破坏性、独立/随机/代表性设计须用户确认。没有不平衡、嵌套、破坏性或属性一致性分析。
- 偏倚：2–500独立测量，参考值视为准确常数，样本SD、SE、t和双侧p、偏倚95%CI。零样本变差明确检验未定义。未传播参考值不确定度，不是等效性证明。
- 线性：6–500行，至少3参考水平、每水平至少2重复；偏倚对参考值OLS、斜率/intercept、残差SD、斜率t/p/95%CI，实际散点与拟合直线。不外推、不自动检验失拟/等方差，不给未经提供过程变差支持的%线性。
- 稳定性：已知参考标准按时间测量的I-MR，显示平均偏倚与双图。仅单点3σ提示；未检验趋势/长期漂移，不能证明长期稳定。

所有模式不自动按10%/30%或其他客户门槛判合格。空项、无穷值、表头、重复/缺失组合拒绝，不自动补齐。数值限幅，完整数据只在屏幕内，不自动进知识库。

统计分布使用Apache Commons Statistics Distribution 1.3，8个Apache依赖约525046字节；ci/statistics-lock.json固定SHA256。主机下载器与Gradle编译前均校验，LICENSES/statistics保存各依赖完整许可证/NOTICE；APK合并许可证资源。未自行实现不完整beta或近似t/F算法。

主机53项断言包括独立SciPy1.17.0黄金数值：完整/合并GR&R、交互项F检验、偏倚t/p/CI、线性斜率SE/p、非法设计。安卓门禁measurement_system_four_modes_real_ui逐一走实际选择、未确认拦截、示例计算、完整结果、真实绘图与停止撤销；提交候选时尚未取得该门禁运行结果，不提前写PASS。

方法依据：
https://support.minitab.com/en-us/minitab/help-and-how-to/quality-and-process-improvement/measurement-system-analysis/how-to/gage-study/crossed-gage-r-r-study/methods-and-formulas/gage-r-r-table/
https://support.minitab.com/en-us/minitab/help-and-how-to/quality-and-process-improvement/measurement-system-analysis/how-to/gage-study/crossed-gage-r-r-study/methods-and-formulas/method-of-analysis/
https://www.itl.nist.gov/div898/handbook/eda/section3/eda3664.htm
https://commons.apache.org/proper/commons-statistics/commons-statistics-distribution/dependencies.html
