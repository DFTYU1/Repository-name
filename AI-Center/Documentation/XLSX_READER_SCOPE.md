# xlsx只读预览范围

入口：首页发送“读取Excel文件”或更多功能→Excel只读预览；系统选择单个文件，仅只读授权，结束立即撤销内部租约。不保存副本、预览或用户单元格内容，不接入聊天历史、知识库或API。原文件不变。

读取工作表名称/隐藏状态、已存单元格地址、共享及内嵌字符串、原始数值、公式文本、公式类型和缓存错误。预览前100格，每个长值/公式截到200字符；全部解析格数用于统计。公式不重新计算，缓存可能过期，不能根据缓存宣称工作簿当前计算正确；共享/数组公式不展开。日期仍是原始序列值，不解释样式/格式/图表/合并区域，不支持xls或加密文件。特殊结构可能拒绝；不是完整Excel模块。

边界：输入10MiB，最多200个ZIP项；单项2MiB、展开总量20MiB、32表、5000格/共享字符串、单值16KiB、XML深度64。内存中处理，ZIP不落地解压。拒绝危险路径、重复ZIP项/单元格、宏和嵌入对象、DOCTYPE/ENTITY声明，禁止外部实体解析及外部工作表关系。UTF-8 XML仅限，已取消任务不继续读。

参考结构（Microsoft官方，按结构独立实现；未复制示例）：
- https://learn.microsoft.com/en-us/office/open-xml/spreadsheet/structure-of-a-spreadsheetml-document
- https://learn.microsoft.com/en-us/office/open-xml/spreadsheet/working-with-the-shared-string-table
- https://learn.microsoft.com/en-us/office/open-xml/spreadsheet/working-with-formulas

主机30个合成文件断言通过；新增Android合成解析强制门禁，因公开提交审批拒绝尚未运行CI。系统文件选择器→实际文档提供程序→预览全链路尚未实机验收。编辑/重算/图表/修复继续待实现。
