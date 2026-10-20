package local.aicenter.app;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import local.aicenter.core.ControlCharts;
/** Original Canvas plot of point-specific control limits; all results remain local. */
final class ControlChartsView extends View {
 private final ControlCharts.Result result;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 ControlChartsView(Context c,ControlCharts.Result result){super(c);this.result=result;setContentDescription("SPC图：绿色中心线、暗红界限、红色越界点；数字与序号见结果");}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawColor(Color.WHITE);float h=(float)getHeight()/result.series.size();for(int i=0;i<result.series.size();i++)plot(canvas,result.series.get(i),i*h,h);}
 private void plot(Canvas c,ControlCharts.Series s,float top,float height){float d=getResources().getDisplayMetrics().density,left=55*d,right=getWidth()-16*d,yt=top+30*d,bottom=top+height-28*d;double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;for(int i=0;i<s.values.length;i++){min=Math.min(min,Math.min(s.values[i],s.lower[i]));max=Math.max(max,Math.max(s.values[i],s.upper[i]));}double range=max-min;if(range==0)range=Math.max(1,Math.abs(max)*.1);min-=range*.05;max+=range*.05;range=max-min;
  paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);paint.setColor(Color.DKGRAY);paint.setStrokeWidth(d);c.drawText(s.name+"（组序号）",8*d,top+18*d,paint);c.drawLine(left,yt,left,bottom,paint);c.drawLine(left,bottom,right,bottom,paint);c.drawText("1",left,bottom+18*d,paint);c.drawText(Integer.toString(s.values.length),right-25*d,bottom+18*d,paint);
  double[][] lines={s.lower,s.center,s.upper,s.values};for(int k=0;k<lines.length;k++){double[] line=lines[k];paint.setColor(k==1?0xff6f897b:k==3?0xff246f61:0xffc14444);float px=0,py=0;for(int i=0;i<line.length;i++){float x=left+(right-left)*i/Math.max(1,line.length-1),y=bottom-(float)((line[i]-min)/range)*(bottom-yt);if(i>0)c.drawLine(px,py,x,y,paint);if(k==3){paint.setColor(line[i]<s.lower[i]||line[i]>s.upper[i]?Color.RED:0xff246f61);c.drawCircle(x,y,2.5f*d,paint);paint.setColor(0xff246f61);}px=x;py=y;}}
 }
}
