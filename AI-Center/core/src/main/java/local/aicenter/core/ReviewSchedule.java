package local.aicenter.core;
/** Explicit local practice policy, not a validated estimate of mastery or model training. */
public final class ReviewSchedule {
 private ReviewSchedule(){}
 public static final long DAY=86_400_000L;
 private static final int[] DAYS={1,3,7,14,30,60};
 public static final class State {
  public final int attempts,errors,stage;public final long updated,due;public final boolean lastCorrect;
  public State(int attempts,int errors,int stage,long updated,long due,boolean lastCorrect){if(attempts<0||attempts>1000000||errors<0||errors>attempts||stage<0||stage>DAYS.length||updated<0||due<0)throw new IllegalArgumentException("无效复习记录");this.attempts=attempts;this.errors=errors;this.stage=stage;this.updated=updated;this.due=due;this.lastCorrect=lastCorrect;}
 }
 private static long add(long time,long interval){return time>Long.MAX_VALUE-interval?Long.MAX_VALUE:time+interval;}
 public static State answer(State old,boolean correct,long now){if(now<0)throw new IllegalArgumentException("时间无效");int attempts=old==null?1:Math.min(1000000,old.attempts+1),errors=old==null?0:old.errors;if(!correct)errors=Math.min(attempts,errors+1);
  if(!correct)return new State(attempts,errors,0,now,add(now,600_000),false);
  // Repeated early attempts never postpone a pending review or inflate its stage.
  if(old!=null&&now<old.due)return new State(attempts,errors,old.stage,now,old.due,true);
  int next=Math.min(DAYS.length,old==null?1:old.stage+1);return new State(attempts,errors,next,now,add(now,DAYS[next-1]*DAY),true);
 }
 public static boolean due(State state,long now){if(now<0)throw new IllegalArgumentException("时间无效");return state!=null&&now>=state.due;}
}
