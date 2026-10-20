package local.aicenter.app;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import local.aicenter.core.IndividualsChart;
/** Original local Canvas renderer; no external chart dependency or network. */
final class IndividualsChartView extends View {
 private final IndividualsChart.Result result;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 IndividualsChartView(Context context,IndividualsChart.Result result){super(context);this.result=result;setContentDescription("I与MR双图，越界点为红色；精确界限和序号见文字结果");setMinimumHeight((int)(420*getResources().getDisplayMetrics().density));}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawColor(Color.WHITE);float h=getHeight()/2f;plot(canvas,result.values,result.lower,result.mean,result.upper,0,h,"I图",1);plot(canvas,result.ranges,0,result.mrMean,result.mrUpper,h,h,"MR图（起点对应第2个观测）",2);}
 private void plot(Canvas c,double[] data,double low,double center,double high,float top,float height,String title,int first){
  float scale=getResources().getDisplayMetrics().density,left=50*scale,right=getWidth()-15*scale,yTop=top+30*scale,bottom=top+height-25*scale;double min=low,max=high;for(double x:data){min=Math.min(min,x);max=Math.max(max,x);}double span=max-min;if(span<=0)return;min-=span*.05;max+=span*.05;span=max-min;paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);paint.setColor(Color.DKGRAY);c.drawText(title,8*scale,top+18*scale,paint);paint.setStrokeWidth(scale);c.drawLine(left,yTop,left,bottom,paint);c.drawLine(left,bottom,right,bottom,paint);
  for(double limit:new double[]{low,center,high}){float y=bottom-(float)((limit-min)/span)*(bottom-yTop);paint.setColor(limit==center?0xff246f61:0xffc14444);c.drawLine(left,y,right,y,paint);}
  float px=0,py=0;for(int i=0;i<data.length;i++){float x=left+(right-left)*i/Math.max(1,data.length-1),y=bottom-(float)((data[i]-min)/span)*(bottom-yTop);paint.setColor(0xff246f61);if(i>0)c.drawLine(px,py,x,y,paint);paint.setColor(data[i]<low||data[i]>high?Color.RED:0xff246f61);c.drawCircle(x,y,2.5f*scale,paint);px=x;py=y;}paint.setColor(Color.DKGRAY);c.drawText(Integer.toString(first),left,bottom+17*scale,paint);c.drawText(Integer.toString(first+data.length-1),right-20*scale,bottom+17*scale,paint);
 }
}
