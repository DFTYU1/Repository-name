package local.aicenter.core;
/** Code-owned capability facts; model output is never proof of implemented tools. */
public final class FeatureCatalog {
 private FeatureCatalog(){}
 public static boolean typingRequested(String input){String t=input.trim().toLowerCase(java.util.Locale.ROOT);return (t.matches("(?:请|帮我|我要|我想|现在|开始|打开|调用|进入|进行|中文|英文|中英文|练习|一下|的|功能|模块|训练|打字|输入|[\\s，。！!])+")&&t.contains("打字"))||t.matches("(?:please )?(?:open|start|practice) (?:english |chinese )?typing(?: practice| training)?[.!]?");}
 public static boolean capabilitiesRequested(String input){return input.trim().matches("(?:你(?:现在)?(?:能做什么|会什么|支持什么(?:功能)?|有什么功能)|(?:查看|显示|打开)?功能(?:清单|列表|中心))[？?。]?");}
 public static String summary(){return "当前可用：本地模型文字对话（回答需核实）、基础算式计算、部分质量计算、Excel公式结构检查、TXT/Markdown资料导入与关键词摘录、中英文打字练习、本地文件/任务/存储管理。\n尚未完成：完整QE/QC与六西格玛课程考试、SPC/MSA/AQL/DOE工具、Excel文件编辑与图表、英语完整课程、语音、中越语音翻译、多格式知识库与语义RAG、长期记忆、学习计划、超级收集器、视频学习、双设备同步、完整API兼容性验证及模型下载中心。\nAPI A/B设置与单次授权请求已接线，实际服务连接尚待验证。\n这是开发版本。生成一段文字不代表已执行上述未完成功能。";}
}
