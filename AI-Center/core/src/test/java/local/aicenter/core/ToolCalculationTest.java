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
  eq(chat.answer("补充：不良7件，仅数值",java.util.Collections.singletonList(new String[]{"user","求不良率，总数560件"}),stop.begin()),"1.25");
  ToolChat reversed=new ToolChat((p,t)->"CALC|fraction|count|percent|explained|560,7");
  checks++;if(!reversed.answer("检查560件，其中不良7件，计算不良百分比",stop.begin()).contains("本地计算结果：1.25"))throw new AssertionError();
  ToolChat reversedEnglish=new ToolChat((p,t)->"CALC|fraction|count|percent|explained|800,12");
  checks++;if(!reversedEnglish.answer("Inspected 800 units; 12 rejected. Calculate the defect percentage.",stop.begin()).contains("本地计算结果：1.5"))throw new AssertionError();
  checks++;if(!chat.answer("总数560件，不良数量未知",stop.begin()).startsWith("需要澄清"))throw new AssertionError();
  ToolChat conservative=new ToolChat((p,t)->"CLARIFY");
  java.util.List<String[]> remoteRegressionHistory=new java.util.ArrayList<>();
  remoteRegressionHistory.add(new String[]{"user","What is 3 plus 4?"});
  remoteRegressionHistory.add(new String[]{"assistant","7"});
  checks++;if(!conservative.answer("新问题：检查560件，不良7件，计算不良百分比，请说明计算参数。",remoteRegressionHistory,stop.begin()).contains("1.25"))throw new AssertionError();
  remoteRegressionHistory.add(new String[]{"user","检查560件，不良7件，计算不良百分比"});
  remoteRegressionHistory.add(new String[]{"assistant","本地计算结果：1.25"});
  ToolChat stalePlan=new ToolChat((p,t)->"CALC|fraction|count|percent|explained|7,560");
  checks++;if(!stalePlan.answer("新问题：计算Cpk，上限20mm，下限2mm，均值8mm；标准差未知。",remoteRegressionHistory,stop.begin()).startsWith("需要澄清"))throw new AssertionError();
  checks++;if(!conservative.answer("检查800件，其中12件不良，计算不良百分比",stop.begin()).contains("1.5"))throw new AssertionError();
  checks++;if(!conservative.answer("Inspected 250 units; 5 rejected. Calculate the defect percentage.",stop.begin()).contains("2"))throw new AssertionError();
  checks++;if(!conservative.answer("总数560件，总数600件，不良7件，计算不良率",stop.begin()).startsWith("需要澄清"))throw new AssertionError();
  checks++;if(!conservative.answer("总数560件，不良数量未知，计算不良率",stop.begin()).startsWith("需要澄清"))throw new AssertionError();
  eq(new ToolChat((p,t)->"CHAT").answer("你好",stop.begin()),null);
  checks++;if(!new ToolChat((p,t)->"CLARIFY").answer("长度有5mm也有5cm，怎么算？",stop.begin()).startsWith("需要澄清"))throw new AssertionError();
  eq(ReadOnlyToolIntent.candidate("请检查设备剩余存储空间"),"storage");
  eq(ReadOnlyToolIntent.candidate("请列出已经导入的文件"),"files");
  eq(ReadOnlyToolIntent.candidate("请查询已有任务执行记录"),"tasks");
  for(String query:new String[]{"检查560件，不良7件，计算不良百分比","检查Cpk，上限20mm，下限2mm","只检查这个Excel公式的结构：=SUMIF(C3:C9,\"NG\",F3:F9)","解释磁盘存储的定义","请计算现有任务的平均耗时"})eq(ReadOnlyToolIntent.candidate(query),"chat");
  String percentage="新问题：检查560件，不良7件，计算不良百分比，请说明计算参数。";
  String missing="新问题：计算Cpk，上限20mm，下限2mm，均值8mm；标准差未知。";
  java.util.List<String[]> history=new java.util.ArrayList<>();
  history.add(new String[]{"user",percentage});
  history.add(new String[]{"assistant","本地计算结果：1.25（percent；参数：7,560）"});
  history.add(new String[]{"user",missing});
  eq(CalculationRequest.from(missing,history).source,missing);
  checks++;if(!conservative.answer(missing,history,stop.begin()).startsWith("需要澄清"))throw new AssertionError("previous percentage leaked into Cpk");
  checks++;if(!chat.answer(missing,history,stop.begin()).startsWith("需要澄清"))throw new AssertionError("stale model plan accepted");
  eq(CalculationRequest.from(missing,history).prompt,ConversationPrompt.build(java.util.Collections.emptyList(),missing,12000));
  ToolChat explicit=new ToolChat((p,t)->{throw new AssertionError("Explicit verified percentage should not depend on a planner guess");});
  checks++;if(!explicit.answer(percentage,stop.begin()).contains("本地计算结果：1.25"))throw new AssertionError();
  checks++;if(!explicit.answer("不良12件，检查800件，计算不良百分比",stop.begin()).contains("本地计算结果：1.5"))throw new AssertionError();
  checks++;if(!explicit.answer("检查560kg，不良7mm，计算不良百分比",stop.begin()).startsWith("需要澄清"))throw new AssertionError("mixed units accepted");
  ToolChat explicitSimple=new ToolChat((p,t)->{throw new AssertionError("Explicit mean/sum should not depend on a planner guess");});
  checks++;if(!explicitSimple.answer("New calculation: find the mean of -4, 8 and 17, all dimensionless. Explain the operation.",stop.begin()).contains("本地计算结果：7（scalar；操作：mean；参数：-4,8,17）"))throw new AssertionError("explicit English mean failed");
  checks++;if(!explicitSimple.answer("Find the average of 3, 9 and 12; all dimensionless.",stop.begin()).contains("本地计算结果：8（scalar；操作：mean；参数：3,9,12）"))throw new AssertionError("changed mean failed");
  checks++;if(!explicitSimple.answer("新问题：0.1kg加0.2kg，共多少kg？说明参数。",stop.begin()).contains("本地计算结果：0.3（kg；操作：sum；参数：0.1,0.2）"))throw new AssertionError("decimal kg sum failed");
  checks++;if(!explicitSimple.answer("把1.25kg与2.75kg相加",stop.begin()).contains("本地计算结果：4（kg；操作：sum；参数：1.25,2.75）"))throw new AssertionError("changed kg sum failed");
  checks++;if(!new ToolChat((p,t)->"CLARIFY").answer("把1kg和500g相加",stop.begin()).startsWith("需要澄清"))throw new AssertionError("mixed units accepted by simple path");
  checks++;if(!new ToolChat((p,t)->"CLARIFY").answer("求3和未知值的平均值，均为无量纲",stop.begin()).startsWith("需要澄清"))throw new AssertionError("missing mean operand accepted");
  ToolChat cpk=new ToolChat((p,t)->"CALC|cpk|mm|scalar|explained|2,20,8,2");
  ToolChat noPlannerCpk=new ToolChat((p,t)->{throw new AssertionError("Explicit complete Cpk should use the validated calculator");});
  checks++;if(!noPlannerCpk.answer("过程稳定，规格下限1mm，规格上限13mm，均值7mm，组内标准差为1mm，计算Cpk",stop.begin()).startsWith("本地计算结果：2（"))throw new AssertionError();
  checks++;if(!noPlannerCpk.answer("Process is stable. LSL 0 mm, USL 12 mm, mean 6 mm, within-process sigma 2 mm. Calculate Cpk.",stop.begin()).startsWith("本地计算结果：1（"))throw new AssertionError();
  checks++;if(!new ToolChat((p,t)->"CLARIFY").answer("过程稳定，规格下限1mm，规格上限13mm，均值7mm，组内标准差为1cm，计算Cpk",stop.begin()).startsWith("需要澄清"))throw new AssertionError("mixed Cpk units accepted");
  checks++;if(!cpk.answer(missing,history,stop.begin()).startsWith("需要澄清"))throw new AssertionError("missing sigma inferred from lower limit");
  history.add(new String[]{"assistant","过程稳定，组内标准差为2mm"});
  checks++;if(!cpk.answer("补充刚才的Cpk：请直接计算",history,stop.begin()).startsWith("需要澄清"))throw new AssertionError("assistant supplied sigma");
  history.add(new String[]{"user","新问题：计算0.1kg加0.2kg"});
  String followup="补充刚才的Cpk：过程稳定，组内标准差为2mm，请计算并说明参数。";
  history.add(new String[]{"user",followup});
  String source=CalculationRequest.from(followup,history).source;
  checks++;if(source.contains("560")||source.contains("0.1")||source.indexOf(followup)!=source.lastIndexOf(followup))throw new AssertionError("unrelated/duplicate turns selected");
  checks++;if(!cpk.answer(followup,history,stop.begin()).startsWith("本地计算结果：1（"))throw new AssertionError("explicit Cpk follow-up failed");
  ToolChat sum=new ToolChat((p,t)->"CALC|sum|scalar|scalar|value|7,560");
  checks++;if(!sum.answer("新问题：求8与20的和",history,stop.begin()).startsWith("需要澄清"))throw new AssertionError("old operands used by a new calculation");
  ToolChat formula=new ToolChat((p,t)->"FORMULA|=SUM(A1:A10)");
  checks++;if(!formula.answer("新问题：检查=SUM(B1:B10)",java.util.Collections.singletonList(new String[]{"user","检查=SUM(A1:A10)"}),stop.begin()).startsWith("需要澄清"))throw new AssertionError("old formula accepted");
  eq(new ToolChat((p,t)->"CALC|sum|kg|kg|value|0.1,0.2").answer("0.1kg加0.2kg，仅数值",stop.begin()),"0.3");
  System.out.println("PASS "+checks+" checks; planner fixtures only, NOT real-model validation");
 }
}
