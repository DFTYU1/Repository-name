package local.aicenter.core;
public final class ToolCalculationTest {
 static int checks;
 static void eq(String got,String want){checks++;if(!java.util.Objects.equals(got,want))throw new AssertionError(got+" != "+want);}
 static void invalid(Runnable r){checks++;try{r.run();}catch(IllegalArgumentException|ArithmeticException e){return;}throw new AssertionError("accepted invalid input");}
 static String calc(String op,String u,String out,String...v){return LocalCalculation.calculate(op,u,out,v);}
 public static void main(String[]args)throws Exception{
  eq(calc("cpk","mm","scalar","2","20","8","2"),"1");
  eq(calc("cp","mm","scalar","2","20","8","2"),"1.5");
  eq(calc("fraction","count","percent","7","560"),"1.25");
  eq(calc("fraction","count","ratio","7","560"),"0.0125");
  eq(calc("mean","scalar","scalar","-4","8","17"),"7");
  eq(calc("sum","kg","kg","0.1","0.2"),"0.3");
  eq(calc("sum","percent","ratio","5"),"0.05");
  invalid(()->calc("divide","scalar","scalar","8","0"));
  invalid(()->calc("cpk","mm","scalar","2","20","8","0"));
  invalid(()->calc("cpk","mm","scalar","20","2","8","1"));
  invalid(()->calc("cpk","mm","scalar","2","20","8"));
  invalid(()->calc("sum","mm","cm","5"));
  invalid(()->calc("fraction","count","percent","4","3"));
  invalid(()->calc("sum","scalar","scalar","NaN"));
  invalid(()->calc("exec","scalar","scalar","1"));
  for(String f:new String[]{"=AVERAGE(D4:D19)","=SUMIF(C3:C9,\"NG\",F3:F9)","=SUMIFS(G2:G7,C2:C7,\"OK\",D2:D7,\">2\")","=XLOOKUP(H8,C4:C12,D4:D12,\"none\")"}){checks++;if(!FormulaCheck.check(f).startsWith("STRUCTURE_ONLY"))throw new AssertionError(f);}
  for(String f:new String[]{"=SUMIF(C3:C9,\"NG\",F3:F8)","=SUM(XFE1:XFE4)","=SUM(B9:B2)","=MATCH(C1,A1:B2,0)"}){checks++;if(!FormulaCheck.check(f).startsWith("INVALID"))throw new AssertionError(f);}
  checks++;if(!FormulaCheck.check("=AVG(D2:D9)").startsWith("UNVERIFIED"))throw new AssertionError();
  StopController stop=new StopController();
  ToolChat chat=new ToolChat((p,t)->"CALC|fraction|count|percent|value|7,560");
  eq(chat.answer("检查560件，发现7件不良，求百分数数值",stop.begin()),"1.25");
  eq(chat.answer("Inspected 560 units; 7 rejected. Give percent only.",stop.begin()),"1.25");
  eq(chat.answer("user: 求不良率，总数560件\nassistant: 不良数？\nuser: 7件，仅数值",stop.begin()),"1.25");
  checks++;if(!chat.answer("总数560件，不良数量未知",stop.begin()).startsWith("需要澄清"))throw new AssertionError();
  eq(new ToolChat((p,t)->"CHAT").answer("你好",stop.begin()),null);
  checks++;if(!new ToolChat((p,t)->"CLARIFY").answer("长度有5mm也有5cm，怎么算？",stop.begin()).startsWith("需要澄清"))throw new AssertionError();
  eq(ReadOnlyToolIntent.candidate("请检查设备剩余存储空间"),"storage");
  eq(ReadOnlyToolIntent.candidate("请列出已经导入的文件"),"files");
  eq(ReadOnlyToolIntent.candidate("请查询已有任务执行记录"),"tasks");
  for(String query:new String[]{"检查560件，不良7件，计算不良百分比","检查Cpk，上限20mm，下限2mm","只检查这个Excel公式的结构：=SUMIF(C3:C9,\"NG\",F3:F9)","解释磁盘存储的定义","请计算现有任务的平均耗时"})eq(ReadOnlyToolIntent.candidate(query),"chat");
  System.out.println("PASS "+checks+" checks; planner fixtures only, NOT real-model validation");
 }
}
