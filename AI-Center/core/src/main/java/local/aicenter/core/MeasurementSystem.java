package local.aicenter.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.statistics.distribution.FDistribution;
import org.apache.commons.statistics.distribution.TDistribution;

/** Bounded, explicit statistical models; no automatic customer acceptance thresholds. */
public final class MeasurementSystem {
 private MeasurementSystem(){}
 public static final String SOURCE="https://support.minitab.com/en-us/minitab/help-and-how-to/quality-and-process-improvement/measurement-system-analysis/how-to/gage-study/crossed-gage-r-r-study/methods-and-formulas/gage-r-r-table/";
 private static double number(String s){try{if(!s.trim().matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]{1,3})?"))throw new NumberFormatException();double v=Double.parseDouble(s.trim());if(!Double.isFinite(v)||Math.abs(v)>1e50)throw new NumberFormatException();return v;}catch(NumberFormatException e){throw new IllegalArgumentException("数据须为有限数值，不能包含标题、缺测或无穷值");}}
 private static List<String[]> rows(String input,int columns){if(input==null||input.trim().isEmpty()||input.length()>100000)throw new IllegalArgumentException("请输入非空数据，最多100000字符");String[] lines=input.trim().replace("\r\n","\n").split("[\\r\\n;；]",-1);if(lines.length>10000)throw new IllegalArgumentException("最多10000行");List<String[]> out=new ArrayList<>();for(int i=0;i<lines.length;i++){String[] cells=lines[i].trim().split("[,，\\t]",-1);if(cells.length!=columns)throw new IllegalArgumentException("第"+(i+1)+"行须有"+columns+"列");for(String c:cells)if(c.trim().isEmpty())throw new IllegalArgumentException("第"+(i+1)+"行有空项");out.add(cells);}return out;}
 private static void confirmed(boolean confirmed){if(!confirmed)throw new IllegalArgumentException("请先确认试验设计、数据顺序、同单位及适用条件");}
 private static String f(double v){return Double.isFinite(v)?String.format(Locale.ROOT,"%.8g",v):"未定义";}
 private static String name(String s){String n=s.trim();if(n.length()>40||n.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("零件/操作员名称须为1至40字符");return n;}
 private static double fProbability(double numerator,double denominator,int ndf,int ddf){if(denominator==0)return numerator>0?0:Double.NaN;return FDistribution.of(ndf,ddf).survivalProbability(numerator/denominator);}
 public static final class Grr {
  public final int parts,operators,repeats,dfPart,dfOperator,dfInteraction,dfError;
  public final double ssPart,ssOperator,ssInteraction,ssError,interactionP,repeatability,operator,interaction,part,grr,total,ndc,tolerancePercent;
  public final boolean pooled;public final String warning;
  Grr(int p,int o,int r,double sp,double so,double si,double se,boolean autoPool,double tolerance){parts=p;operators=o;repeats=r;dfPart=p-1;dfOperator=o-1;dfInteraction=(p-1)*(o-1);dfError=p*o*(r-1);ssPart=sp;ssOperator=so;ssInteraction=si;ssError=se;double mp=sp/dfPart,mo=so/dfOperator,mi=si/dfInteraction,me=se/dfError;interactionP=fProbability(mi,me,dfInteraction,dfError);pooled=autoPool&&Double.isFinite(interactionP)&&interactionP>=.05;
   double residual=pooled?(se+si)/(dfError+dfInteraction):me;double rawO=(mo-(pooled?residual:mi))/(p*r),rawP=(mp-(pooled?residual:mi))/(o*r),rawI=pooled?0:(mi-me)/r;
   repeatability=residual;operator=Math.max(0,rawO);interaction=Math.max(0,rawI);part=Math.max(0,rawP);grr=repeatability+operator+interaction;total=grr+part;ndc=grr>0?Math.max(1,Math.floor(1.41*Math.sqrt(part/grr))):Double.NaN;tolerancePercent=tolerance>0?600*Math.sqrt(grr)/tolerance:Double.NaN;
   warning=(rawO<0||rawP<0||rawI<0?"负方差分量已截为0；边界估计需复核。\n":"")+(total==0||grr==0?"零变差/零GR&R为退化估计，不能证明无限分辨率。\n":"");
  }
  public double contribution(double variance){return total>0?100*variance/total:Double.NaN;}
  public double studyVariation(double variance){return total>0?100*Math.sqrt(variance/total):Double.NaN;}
  public String report(){StringBuilder b=new StringBuilder("交叉GR&R · 平衡双因素随机效应ANOVA\n零件="+parts+"，操作员="+operators+"，重复="+repeats+"\n交互项p="+f(interactionP)+"；模型="+(pooled?"α=0.05，合并交互项到误差":"保留交互项")+"\nSS/df：零件="+f(ssPart)+"/"+dfPart+"，操作员="+f(ssOperator)+"/"+dfOperator+"，交互="+f(ssInteraction)+"/"+dfInteraction+"，重复误差="+f(ssError)+"/"+dfError+"\n分量：方差 / %贡献 / %研究变差（6σ）\n");double[] v={repeatability,operator,interaction,grr,part,total};String[] n={"重复性","操作员","交互","总GR&R","零件间","总变差"};for(int i=0;i<v.length;i++)b.append(n[i]).append("：").append(f(v[i])).append(" / ").append(f(contribution(v[i]))).append("% / ").append(f(studyVariation(v[i]))).append("%\n");return b+"ndc="+f(ndc)+"（向下取整，最低显示1）\n%公差（6σGR&R/公差）="+f(tolerancePercent)+"\n"+warning+"不自动判合格；随机性、代表性、正态性与独立性需确认。不适用于破坏性/嵌套或不平衡设计。";}
 }
 public static Grr crossed(String input,boolean designConfirmed,boolean autoPool,double tolerance){confirmed(designConfirmed);if(!Double.isFinite(tolerance)||tolerance<0||tolerance>1e50)throw new IllegalArgumentException("公差须为正数；0表示未提供");List<String[]> rows=rows(input,4);Map<String,Integer> parts=new LinkedHashMap<>(),operators=new LinkedHashMap<>();int r=0;for(String[] row:rows){parts.computeIfAbsent(name(row[0]),k->parts.size());operators.computeIfAbsent(name(row[1]),k->operators.size());double trial=number(row[2]);if(trial!=Math.rint(trial)||trial<1||trial>20)throw new IllegalArgumentException("重复编号须为连续1至20整数");r=Math.max(r,(int)trial);number(row[3]);}int p=parts.size(),o=operators.size();if(p<2||p>50||o<2||o>10||r<2)throw new IllegalArgumentException("需要2至50零件、2至10操作员、每单元2至20重复");double[][][] values=new double[p][o][r];boolean[][][] seen=new boolean[p][o][r];for(String[] row:rows){int a=parts.get(name(row[0])),b=operators.get(name(row[1])),c=(int)number(row[2])-1;if(seen[a][b][c])throw new IllegalArgumentException("零件/操作员/重复编号存在重复");seen[a][b][c]=true;values[a][b][c]=number(row[3]);}double grand=0;double[] pm=new double[p],om=new double[o];double[][] cm=new double[p][o];for(int a=0;a<p;a++)for(int b=0;b<o;b++)for(int c=0;c<r;c++){if(!seen[a][b][c])throw new IllegalArgumentException("设计不平衡或缺少连续重复编号；不自动补值");double x=values[a][b][c];grand+=x/(p*o*r);pm[a]+=x/(o*r);om[b]+=x/(p*r);cm[a][b]+=x/r;}
  double sp=0,so=0,si=0,se=0;for(int a=0;a<p;a++)sp+=o*r*Math.pow(pm[a]-grand,2);for(int b=0;b<o;b++)so+=p*r*Math.pow(om[b]-grand,2);for(int a=0;a<p;a++)for(int b=0;b<o;b++){si+=r*Math.pow(cm[a][b]-pm[a]-om[b]+grand,2);for(int c=0;c<r;c++)se+=Math.pow(values[a][b][c]-cm[a][b],2);}return new Grr(p,o,r,sp,so,si,se,autoPool,tolerance);
 }
 public static final class Bias {
  public final int count;public final double reference,mean,sd,bias,se,t,p,lower,upper;
  Bias(double[] x,double ref){count=x.length;reference=ref;double m=0;for(int i=0;i<x.length;i++)m+=(x[i]-m)/(i+1);mean=m;bias=m-ref;double ss=0;for(double v:x)ss+=(v-m)*(v-m);sd=Math.sqrt(ss/(count-1));se=sd/Math.sqrt(count);t=se>0?bias/se:Double.NaN;p=se>0?Math.min(1,2*TDistribution.of(count-1).survivalProbability(Math.abs(t))):Double.NaN;double delta=se>0?TDistribution.of(count-1).inverseCumulativeProbability(.975)*se:Double.NaN;lower=bias-delta;upper=bias+delta;}
  public String report(){return "偏倚 · 单样本t模型\nn="+count+"；参考值="+f(reference)+"；均值="+f(mean)+"\n偏倚="+f(bias)+"；样本SD="+f(sd)+"；SE="+f(se)+"\nt="+f(t)+"；df="+(count-1)+"；双侧p="+f(p)+"\n偏倚95%CI=["+f(lower)+", "+f(upper)+"]\n"+(se==0?"零样本变差，t检验和区间未定义。\n":"")+"参考值视为准确常数，未合并其不确定度；须独立、代表性重复测量。p值不证明无偏或实用等效，不自动判合格。";}
 }
 public static Bias bias(String input,double reference,boolean designConfirmed){confirmed(designConfirmed);if(!Double.isFinite(reference)||Math.abs(reference)>1e50)throw new IllegalArgumentException("参考值无效");List<String[]> rows=rows(input,1);if(rows.size()<2||rows.size()>500)throw new IllegalArgumentException("需要2至500次测量，每行一个值");double[] x=new double[rows.size()];for(int i=0;i<x.length;i++)x[i]=number(rows.get(i)[0]);return new Bias(x,reference);}
 public static final class Linearity {
  public final double[] references,biases;public final double slope,intercept,residualSd,slopeSe,t,p,lower,upper;public final int df;
  Linearity(double[] x,double[] y){references=x;biases=y;df=x.length-2;double xm=0,ym=0;for(int i=0;i<x.length;i++){xm+=(x[i]-xm)/(i+1);ym+=(y[i]-ym)/(i+1);}double xx=0,xy=0;for(int i=0;i<x.length;i++){xx+=Math.pow(x[i]-xm,2);xy+=(x[i]-xm)*(y[i]-ym);}if(!(xx>0)||!Double.isFinite(xx))throw new IllegalArgumentException("参考水平数值间隔不足，无法可靠拟合");slope=xy/xx;intercept=ym-slope*xm;double ss=0;for(int i=0;i<x.length;i++)ss+=Math.pow(y[i]-intercept-slope*x[i],2);residualSd=Math.sqrt(ss/df);slopeSe=residualSd/Math.sqrt(xx);t=slopeSe>0?slope/slopeSe:Double.NaN;p=slopeSe>0?Math.min(1,2*TDistribution.of(df).survivalProbability(Math.abs(t))):Double.NaN;double delta=slopeSe>0?TDistribution.of(df).inverseCumulativeProbability(.975)*slopeSe:Double.NaN;lower=slope-delta;upper=slope+delta;}
  public String report(){return "线性 · 偏倚对参考值OLS回归\nn="+references.length+"；df="+df+"\n偏倚="+f(intercept)+" + "+f(slope)+" × 参考值\n残差SD="+f(residualSd)+"；斜率SE="+f(slopeSe)+"\nt="+f(t)+"；双侧p="+f(p)+"\n斜率95%CI=["+f(lower)+", "+f(upper)+"]\n"+(slopeSe==0?"零残差，t检验和区间未定义。\n":"")+"参考值视为准确；未检验残差正态/等方差、失拟或参考值不确定度。不外推，不自动判合格。";}
 }
 public static Linearity linearity(String input,boolean designConfirmed){confirmed(designConfirmed);List<String[]> rows=rows(input,2);if(rows.size()<6||rows.size()>500)throw new IllegalArgumentException("需要6至500行：参考值,测量值；至少3参考水平，每水平至少2次");double[] x=new double[rows.size()],y=new double[rows.size()];Map<Double,Integer> levels=new LinkedHashMap<>();for(int i=0;i<x.length;i++){x[i]=number(rows.get(i)[0]);if(x[i]==0)x[i]=0;y[i]=number(rows.get(i)[1])-x[i];levels.merge(x[i],1,Integer::sum);}if(levels.size()<3||levels.values().stream().anyMatch(n->n<2))throw new IllegalArgumentException("至少3个不同参考水平，每水平至少2次重复");return new Linearity(x,y);}
}
