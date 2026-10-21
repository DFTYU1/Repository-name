package local.aicenter.app;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import local.aicenter.core.FactorialDesign;
/** Original signed effect bars, response units, no automatic significance threshold. */
final class FactorialPlot extends View {
 private final FactorialDesign.Result result;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 FactorialPlot(Context c,FactorialDesign.Result r){super(c);result=r;setContentDescription("DOE有符号效应图，绿色正效应，蓝色负效应");}
 @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.WHITE);float d=getResources().getDisplayMetrics().density,left=56*d,right=getWidth()-14*d,top=38*d,bottom=getHeight()-25*d,zero=(left+right)/2;if(right<=left||bottom<=top)return;double max=0;for(FactorialDesign.Term t:result.terms)max=Math.max(max,Math.abs(t.effect));if(max==0)max=1;float row=(bottom-top)/result.terms.size();p.setTextSize(12*d);p.setStrokeWidth(d);p.setColor(Color.DKGRAY);c.drawText("效应（"+result.unit+"）；左负/右正",8*d,18*d,p);c.drawLine(zero,top,zero,bottom,p);for(int i=0;i<result.terms.size();i++){FactorialDesign.Term t=result.terms.get(i);float y=top+row*i,w=(float)(t.effect/max)*(right-left)/2;p.setColor(Color.DKGRAY);c.drawText(t.name,4*d,y+row*.65f,p);p.setColor(t.effect>=0?0xff246f61:Color.BLUE);c.drawRect(Math.min(zero,zero+w),y+row*.15f,Math.max(zero,zero+w),y+row*.8f,p);}p.setColor(Color.DKGRAY);c.drawText(String.format(java.util.Locale.ROOT,"−%.4g",max),left,bottom+18*d,p);c.drawText("0",zero,bottom+18*d,p);c.drawText(String.format(java.util.Locale.ROOT,"%.4g",max),Math.max(zero+20*d,right-65*d),bottom+18*d,p);}
}
