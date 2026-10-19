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
  eq(FeatureCatalog.learningRequested("打开自主学习"),true);eq(FeatureCatalog.learningRequested("不要打开自主学习"),false);
  eq(LearningPolicy.target(false,0,100),LearningPolicy.target(false,2,100));
  eq(LearningPolicy.target(false,3,96),LearningPolicy.target(false,0,0));
  checks++;if(LearningPolicy.target(false,3,97).equals(LearningPolicy.target(false,0,0)))throw new AssertionError("No adaptation");
  eq(LearningPolicy.target(true,3,Double.NaN),LearningPolicy.target(true,0,0));
  eq(LearningPolicy.accuracy(10,2),80.0);eq(LearningPolicy.accuracy(0,0),0.0);
  boolean rejected=false;try{LearningPolicy.accuracy(1,2);}catch(IllegalArgumentException e){rejected=true;}eq(rejected,true);
  List<String[]> oldNotes=Arrays.asList(new String[]{"user","old question"},new String[]{"assistant",LearningPolicy.CORRECTION_PREFIX+"private-correction"},new String[]{"assistant","ordinary answer"});
  String filtered=ConversationPrompt.build(oldNotes,"new question",1000);eq(filtered.contains("private-correction"),false);eq(filtered.contains("ordinary answer"),true);
  List<String[]> longHistory=Arrays.asList(new String[]{"user","OLD".repeat(100)},new String[]{"assistant","OLDER".repeat(100)},new String[]{"user","recent"});
  String fitted=ConversationPrompt.fit(longHistory,"CURRENT😀",300,50,x->x.codePointCount(0,x.length()));
  eq(fitted.contains("CURRENT😀"),true);eq(fitted.contains("OLDER"),false);eq(fitted.codePointCount(0,fitted.length())<=250,true);
  boolean oversized=false;try{ConversationPrompt.fit(Collections.emptyList(),"超".repeat(500),300,50,String::length);}catch(IllegalArgumentException e){oversized=true;}eq(oversized,true);
  boolean badCount=false;try{ConversationPrompt.fit(Collections.emptyList(),"x",300,50,x->0);}catch(IllegalStateException e){badCount=true;}eq(badCount,true);
  for(CapabilityCourse.Lesson l:CapabilityCourse.LESSONS){int[] correct=new int[l.questions.length],wrong=new int[l.questions.length];for(int i=0;i<correct.length;i++){correct[i]=l.questions[i].answer;wrong[i]=(correct[i]+1)%l.questions[i].options.length;}eq(CapabilityCourse.grade(l,correct),correct.length);eq(CapabilityCourse.grade(l,wrong),0);checks++;if(!l.source.startsWith("https://www.itl.nist.gov/"))throw new AssertionError("Missing primary source");}
  eq(CapabilityCourse.calculate("1","13","10","1","mm",true).startsWith("Cp≈2；Cpk≈1"),true);
  eq(CapabilityCourse.calculate("1","13","14","1","mm",true).contains("Cpk≈-0.333333"),true);
  for(String[] bad:new String[][]{{"1","13","7","0"},{"13","1","7","1"},{"1","13","NaN","1"}}){boolean rejectedInput=false;try{CapabilityCourse.calculate(bad[0],bad[1],bad[2],bad[3],"mm",true);}catch(IllegalArgumentException e){rejectedInput=true;}eq(rejectedInput,true);}
  boolean unconfirmed=false;try{CapabilityCourse.calculate("1","13","7","1","mm",false);}catch(IllegalArgumentException e){unconfirmed=true;}eq(unconfirmed,true);
  boolean incomplete=false;try{CapabilityCourse.grade(CapabilityCourse.LESSONS[0],new int[]{-1,1});}catch(IllegalArgumentException e){incomplete=true;}eq(incomplete,true);
  System.out.println("PASS refactor regressions: "+checks+" assertions (host only; no APK/device claim)");
 }
}
