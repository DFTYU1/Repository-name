package local.aicenter.app;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import local.aicenter.core.MeasurementSystem;
/** Native display of computed variance components or observed bias and fitted line. */
final class MeasurementPlot extends View {
 private final MeasurementSystem.Grr grr;private final MeasurementSystem.Linearity linear;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 MeasurementPlot(Context context,MeasurementSystem.Grr result){super(context);grr=result;linear=null;setContentDescription("GR&R方差分量图");}
 MeasurementPlot(Context context,MeasurementSystem.Linearity result){super(context);linear=result;grr=null;setContentDescription("偏倚对参考值散点与OLS线");}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,left=48*d,right=getWidth()-16*d,top=35*d,bottom=getHeight()-45*d;if(right<=left||bottom<=top)return;c.drawColor(Color.WHITE);paint.setStrokeWidth(2*d);paint.setTextSize(12*d);paint.setColor(Color.DKGRAY);c.drawLine(left,top,left,bottom,paint);c.drawLine(left,bottom,right,bottom,paint);
  if(grr!=null){double[] v={grr.repeatability,grr.operator,grr.interaction,grr.part};String[] labels={"重复","人员","交互","零件"};double max=0;for(double x:v)max=Math.max(max,x);if(max==0)max=1;for(int i=0;i<v.length;i++){float x=left+(right-left)*(i+.1f)/4,w=(right-left)*.8f/4;paint.setColor(Color.rgb(30,120,90));c.drawRect(x,(float)(bottom-(bottom-top)*v[i]/max),x+w,bottom,paint);paint.setColor(Color.DKGRAY);c.drawText(labels[i],x,bottom+20*d,paint);}c.drawText("方差（单位²）；顶部="+String.format(java.util.Locale.ROOT,"%.4g",max),left,20*d,paint);
  }else{double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,minY=Double.POSITIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;for(int i=0;i<linear.references.length;i++){minX=Math.min(minX,linear.references[i]);maxX=Math.max(maxX,linear.references[i]);minY=Math.min(minY,linear.biases[i]);maxY=Math.max(maxY,linear.biases[i]);double fit=linear.intercept+linear.slope*linear.references[i];minY=Math.min(minY,fit);maxY=Math.max(maxY,fit);}if(maxY==minY){maxY+=1;minY-=1;}final double low=minY,high=maxY;paint.setColor(Color.rgb(30,120,90));for(int i=0;i<linear.references.length;i++)c.drawCircle((float)(left+(right-left)*(linear.references[i]-minX)/(maxX-minX)),(float)(bottom-(bottom-top)*(linear.biases[i]-low)/(high-low)),4*d,paint);paint.setColor(Color.BLUE);c.drawLine(left,(float)(bottom-(bottom-top)*(linear.intercept+linear.slope*minX-low)/(high-low)),right,(float)(bottom-(bottom-top)*(linear.intercept+linear.slope*maxX-low)/(high-low)),paint);paint.setColor(Color.DKGRAY);c.drawText("纵轴：偏倚；蓝线：OLS",left,20*d,paint);c.drawText(String.format(java.util.Locale.ROOT,"参考值：%.4g → %.4g",minX,maxX),left,bottom+20*d,paint);c.drawText(String.format(java.util.Locale.ROOT,"%.3g",high),0,top,paint);c.drawText(String.format(java.util.Locale.ROOT,"%.3g",low),0,bottom,paint);}
 }
}
