package local.aicenter.core;
/** Original short lessons and fixtures; references are public NIST statistical handbook pages. */
public final class CapabilityCourse {
 private CapabilityCourse(){}
 public static final class Question {
  public final String text,explanation;public final String[] options;public final int answer;
  Question(String text,String[] options,int answer,String explanation){this.text=text;this.options=options;this.answer=answer;this.explanation=explanation;}
 }
 public static final class Lesson {
  public final String id,title,body,source;public final Question[] questions;
  Lesson(String id,String title,String body,String source,Question... questions){this.id=id;this.title=title;this.body=body;this.source=source;this.questions=questions;}
 }
 public static final Lesson[] LESSONS={
  new Lesson("stability","1 先判断过程是否稳定",
   "一句话：合格率高，不等于过程稳定。\n何时用：在用能力指数评价过程之前。\n案例：零件尺寸都在规格内，但按生产顺序持续上升；只看合格率可能漏掉漂移。\n操作：按时间保留测量值→检查量具与采样→看时间图和适用控制图→调查异常→确认稳定后再谈能力。\n本课不代替实际控制图判异或MSA。",
   "https://www.itl.nist.gov/div898/handbook/ppc/section4/ppc45.htm",
   new Question("尺寸全部在规格内，就能证明过程稳定吗？",new String[]{"能，只需合格率100%","不能，还要观察过程随时间的表现"},1,"规格判产品是否符合要求；稳定性还涉及过程随时间的变化。"),
   new Question("计算能力前优先做什么？",new String[]{"把异常点删掉","确认测量与采样合理并判断稳定性"},1,"先确认数据和过程，不能为得到好看的指数任意删点。")),
  new Lesson("centering","2 分清Cp和Cpk",
   "一句话：Cp看规格宽度相对波动；Cpk还看均值偏向哪一侧。\n何时用：过程稳定、数据模型和标准差估计适用时。\n原创例子：LSL=1、USL=13、均值=7、组内σ=1，则Cp=2、Cpk=2；仅均值移到10，Cp仍为2，Cpk降至1。\n操作：打开计算工具→输入两组参数→比较结果。不能单凭一个指数替代客户判定、稳定性或分布检查。",
   "https://www.itl.nist.gov/div898/handbook/ppc/section4/ppc46.htm",
   new Question("均值偏向上限、波动不变时，哪个指数体现偏移？",new String[]{"Cp","Cpk"},1,"Cpk使用均值到较近规格边界的距离。"),
   new Question("例中均值从7移到10，Cpk是多少？",new String[]{"1","2","3"},0,"min((13−10)/3,(10−1)/3)=1。Cp仍为(13−1)/6=2。")),
  new Lesson("inputs","3 正确使用能力计算器",
   "一句话：先确认数据的含义，再代入公式。\n何时用：双侧规格的基础Cp/Cpk估算。\n操作：统一单位→确认LSL<USL→确认正的组内σ与过程条件→输入均值→核对近侧规格距离。\n限制：本工具不估计组内σ，不检验正态性，不计算置信区间或由指数推算实际不良率；客户门槛须另行确认。单侧规格不能硬凑另一个边界。",
   "https://www.itl.nist.gov/div898/handbook/pmc/section1/pmc16.htm",
   new Question("缺少σ，应当怎样做？",new String[]{"取σ=1作为默认","补充适用的标准差估计与数据依据"},1,"σ是波动信息，不能由规格或均值随意推算。"),
   new Question("计算出较高Cpk后，能省略过程稳定性判断吗？",new String[]{"能","不能"},1,"能力评价有使用条件；计算器代入成功并不证明这些条件成立。"))
 };
 public static Lesson lesson(String id){for(Lesson l:LESSONS)if(l.id.equals(id))return l;throw new IllegalArgumentException("未知课程");}
 public static int grade(Lesson lesson,int[] selections){if(selections==null||selections.length!=lesson.questions.length)throw new IllegalArgumentException("请完成所有题目");int score=0;for(int i=0;i<selections.length;i++){if(selections[i]<0||selections[i]>=lesson.questions[i].options.length)throw new IllegalArgumentException("请完成所有题目");if(selections[i]==lesson.questions[i].answer)score++;}return score;}
 public static String calculate(String lower,String upper,String mean,String sigma,String unit,boolean conditions){
  if(!conditions)throw new IllegalArgumentException("请先确认过程条件与组内标准差含义");
  String[] values={lower.trim(),upper.trim(),mean.trim(),sigma.trim()};
  String cp=LocalCalculation.calculate("cp",unit,"scalar",values),cpk=LocalCalculation.calculate("cpk",unit,"scalar",values);
  return "Cp≈"+rounded(cp)+"；Cpk≈"+rounded(cpk)+"\nCp=(USL−LSL)/(6σ)\nCpk=min(USL−均值,均值−LSL)/(3σ)\n输入单位："+unit+"；结果无量纲，显示至小数后6位。\n本工具仅代入计算，不证明过程稳定、分布适用或满足客户要求。";
 }
 private static String rounded(String s){return new java.math.BigDecimal(s).setScale(6,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
}
