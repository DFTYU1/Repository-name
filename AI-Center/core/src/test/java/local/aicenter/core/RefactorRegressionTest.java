package local.aicenter.core;
import java.util.*;
import java.math.BigDecimal;
public final class RefactorRegressionTest {
 private static int checks;
 private static void eq(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args)throws Exception{
  StopController stop=new StopController();
  ToolChat direct=new ToolChat((p,t)->{throw new AssertionError("Unexpected model planning");});
  for(String[] v:new String[][]{{"1+1等于几","2"},{"请计算（2＋3）×4","20"},{"Calculate 7-2*3?","1"},{"0.1+0.2","0.3"},{"-5--2","-3"},{"18/(2+1)","6"},{"1+-2","-1"}})eq(direct.answer(v[0],stop.begin()),v[1]);
  eq(direct.answer("9999999999999999999999999999999999999999+1",stop.begin()),"10000000000000000000000000000000000000000");
  checks++;if(!direct.answer("1/3",stop.begin()).contains("约"))throw new AssertionError("Silent rounding");
  Random random=new Random(20261019);
  for(int i=0;i<200;i++){int a=random.nextInt(20000)-10000,b=random.nextInt(20000)-10000;eq(direct.answer(a+"+("+b+")",stop.begin()),BigDecimal.valueOf((long)a+b).toPlainString());}
  for(String bad:new String[]{"1/0","1+","(2+3","1..2+3","((((((((((((((((((((((((((((((((((1+2"}){checks++;if(!direct.answer(bad,stop.begin()).startsWith("算式无法计算"))throw new AssertionError(bad);}
  for(String other:new String[]{"调用打字功能","打开打字练习","解释1+1为什么是2","1kg+2kg","删除1个文件","=SUM(A1:A2)"})eq(ArithmeticExpression.answer(other),null);
  List<String[]> history=Arrays.asList(new String[]{"user","1+1"},new String[]{"assistant","2"});
  eq(direct.answer("调用打字功能",history,stop.begin()),null);
  for(String text:new String[]{"调用打字功能","我要练习打字","打开中文打字练习","start english typing practice"})eq(FeatureCatalog.typingRequested(text),true);
  for(String text:new String[]{"打字功能是什么","不打开打字功能","1+1","请计算打字速度20除以4"})eq(FeatureCatalog.typingRequested(text),false);
  eq(FeatureCatalog.capabilitiesRequested("你能做什么？"),true);
  eq(direct.answer("你好",stop.begin()),null);
  TypingSession typing=new TypingSession("abcde",1000);
  TypingSession.Result r=typing.score("abcde",61000);eq(r.wpm,1.0);eq(r.accuracy,100.0);eq(r.complete,true);
  r=typing.score("abXde!",61000);eq(r.errors,2);eq(r.complete,false);
  r=new TypingSession("中😀文",0).score("中😀文",60000);eq(r.correct,3);eq(r.charactersPerMinute,3.0);
  System.out.println("PASS refactor regressions: "+checks+" assertions (host only; no APK/device claim)");
 }
}
