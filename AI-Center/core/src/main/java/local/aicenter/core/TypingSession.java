package local.aicenter.core;
/** Unicode code-point scoring. English WPM uses five characters; Chinese uses characters/minute. */
public final class TypingSession {
 public final String target;private final long started;
 public TypingSession(String target,long started){if(target==null||target.isEmpty())throw new IllegalArgumentException("练习文本为空");this.target=target;this.started=started;}
 public Result score(String input,long now){int[] e=target.codePoints().toArray(),a=input.codePoints().toArray();int correct=0;for(int i=0;i<a.length&&i<e.length;i++)if(a[i]==e[i])correct++;long elapsed=Math.max(0,now-started);double minutes=Math.max(1000,elapsed)/60000.0;return new Result(a.length,correct,a.length-correct,a.length==0?0:100.0*correct/a.length,correct/minutes,correct/5.0/minutes,input.equals(target),elapsed);}
 public static final class Result {
  public final int typed,correct,errors;public final double accuracy,charactersPerMinute,wpm;public final boolean complete;public final long elapsedMs;
  private Result(int t,int c,int e,double a,double cpm,double w,boolean done,long ms){typed=t;correct=c;errors=e;accuracy=a;charactersPerMinute=cpm;wpm=w;complete=done;elapsedMs=ms;}
 }
}
