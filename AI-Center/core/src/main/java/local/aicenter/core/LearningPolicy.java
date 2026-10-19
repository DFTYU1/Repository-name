package local.aicenter.core;
/** Deterministic curriculum adaptation; no model training or generated facts. */
public final class LearningPolicy {
 public static final String CORRECTION_PREFIX="[你确认的纠错记录；未独立核实]\n";
 private LearningPolicy(){}
 public static String target(boolean chinese,int sessions,double accuracy){
  boolean advance=sessions>=3&&Double.isFinite(accuracy)&&accuracy>=97;
  if(chinese)return advance?"确认量具有效，分析过程波动，再验证改善效果。":"检查零件，记录不良。";
  return advance?"Verify the measurement system before assessing process capability.":"Inspect each part and record the defect.";
 }
 public static double accuracy(int typed,int errors){if(typed<0||errors<0||errors>typed)throw new IllegalArgumentException("invalid counters");return typed==0?0:100.0*(typed-errors)/typed;}
}
