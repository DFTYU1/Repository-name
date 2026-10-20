package local.aicenter.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bounded Phase-I classical 3-sigma charts. Only a single-point rule; no automatic stability release. */
public final class ControlCharts {
 private ControlCharts(){}
 public static final String[] KINDS={"XBAR_R","XBAR_S","P","NP","C","U"};
 // NIST handbook pmc321 table, rounded factors as published, n=2..10.
 private static final java.util.regex.Pattern NUMBER=java.util.regex.Pattern.compile("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]{1,3})?");
 private static final double[] A2={1.880,1.023,.729,.577,.483,.419,.373,.337,.308};
 private static final double[] D3={0,0,0,0,0,.076,.136,.184,.223};
 private static final double[] D4={3.267,2.575,2.282,2.115,2.004,1.924,1.864,1.816,1.777};
 public static final class Series {
  public final String name;public final double[] values,lower,center,upper;public final List<Integer> signals=new ArrayList<>();
  Series(String name,double[] values,double[] lower,double[] center,double[] upper){this.name=name;this.values=values;this.lower=lower;this.center=center;this.upper=upper;for(int i=0;i<values.length;i++)if(values[i]<lower[i]||values[i]>upper[i])signals.add(i+1);}
 }
 public static final class Result {
  public final String kind;public final List<Series> series=new ArrayList<>();public String warning="";
  Result(String kind){this.kind=kind;}
  public String report(){String display=kind.equals("XBAR_R")?"X̄-R":kind.equals("XBAR_S")?"X̄-S":kind.toLowerCase(Locale.ROOT);StringBuilder b=new StringBuilder(display+" · 本批估计界限（约8位有效数字）\n");for(Series s:series){b.append(s.name).append("，越界组序号：").append(s.signals).append('\n');boolean fixed=true;for(int i=1;i<s.values.length;i++)if(s.lower[i]!=s.lower[0]||s.upper[i]!=s.upper[0])fixed=false;
    if(fixed)b.append("LCL≈").append(f(s.lower[0])).append("；CL≈").append(f(s.center[0])).append("；UCL≈").append(f(s.upper[0])).append('\n');
    else{b.append("每组样本量/机会量不同，使用逐组界限：\n");for(int i=0;i<Math.min(50,s.values.length);i++)b.append(i+1).append(": LCL≈").append(f(s.lower[i])).append("；CL≈").append(f(s.center[i])).append("；UCL≈").append(f(s.upper[i])).append('\n');if(s.values.length>50)b.append("文字只列前50组，图包含全部组。\n");}}
   return b.append(warning).append("\n仅单点超出3σ近似界限，不检查连串/趋势等其他判异规则。界限从本批估计，不是规格线；不能直接证明稳定或决定产品接收。需核实时间顺序、合理子组、独立性及二项/泊松/测量分布等适用条件。低计数时需另用精确分布方法。\nR图常数使用NIST发布的3位近似值；S图使用c4校正。").toString();}
 }
 public static Result calculate(String kind,String input,boolean confirmed){
  if(!confirmed)throw new IllegalArgumentException("请确认数据顺序、分组、单位及图种适用条件");
  if(!java.util.Arrays.asList(KINDS).contains(kind))throw new IllegalArgumentException("未知控制图种");
  double[][] rows=parse(input);Result result=new Result(kind);int m=rows.length;
  if(kind.startsWith("XBAR_")){
   int n=rows[0].length;if(n<2||n>(kind.equals("XBAR_R")?10:100))throw new IllegalArgumentException("X̄-R每组2–10值，X̄-S每组2–100值");
   double[] means=new double[m],dispersion=new double[m];double grand=0,average=0;
   for(int i=0;i<m;i++){if(rows[i].length!=n)throw new IllegalArgumentException("子组大小必须相同，不自动补齐或删值");double mean=0,m2=0,min=rows[i][0],max=min;for(int j=0;j<n;j++){double x=rows[i][j],delta=x-mean;mean+=delta/(j+1);m2+=delta*(x-mean);min=Math.min(min,x);max=Math.max(max,x);}means[i]=mean;dispersion[i]=kind.equals("XBAR_R")?max-min:Math.sqrt(Math.max(0,m2)/(n-1));grand+=(mean-grand)/(i+1);average+=(dispersion[i]-average)/(i+1);}
   if(average==0)throw new IllegalArgumentException("组内变异为零，不能从本批估计有效界限，请检查分辨率与分组");
   double a,bLow,bHigh;if(kind.equals("XBAR_R")){a=A2[n-2];bLow=D3[n-2];bHigh=D4[n-2];}else{double c4=c4(n),factor=3*Math.sqrt(Math.max(0,1-c4*c4))/c4;a=3/(c4*Math.sqrt(n));bLow=Math.max(0,1-factor);bHigh=1+factor;}
   result.series.add(fixed("X̄",means,grand-a*average,grand,grand+a*average));result.series.add(fixed(kind.equals("XBAR_R")?"R":"S",dispersion,bLow*average,average,bHigh*average));return result;
  }
  double[] count=new double[m],size=new double[m];double totalCount=0,totalSize=0;
  for(int i=0;i<m;i++){
   int expected=kind.equals("C")?1:2;if(rows[i].length!=expected)throw new IllegalArgumentException("c图每行1个缺陷数；p/np/u图每行计数和样本量/机会量2值");
   count[i]=rows[i][0];if(count[i]<0||count[i]>1e9||count[i]!=Math.rint(count[i]))throw new IllegalArgumentException("计数须为0至10亿的整数");size[i]=expected==1?1:rows[i][1];
   if(size[i]<1e-9||size[i]>1e9)throw new IllegalArgumentException("样本量/机会量必须为正且不超限");
   if((kind.equals("P")||kind.equals("NP"))&&(size[i]!=Math.rint(size[i])||count[i]>size[i]))throw new IllegalArgumentException("p/np图样本量须为正整数，不合格件数不得超过样本量");
   if(kind.equals("NP")&&size[i]!=size[0])throw new IllegalArgumentException("np图必须固定样本量，变化样本量请使用p图");totalCount+=count[i];totalSize+=size[i];
  }
  double rate=totalCount/totalSize;double[] y=new double[m],lo=new double[m],cl=new double[m],hi=new double[m];
  for(int i=0;i<m;i++){
   double sigma;
   if(kind.equals("P")){y[i]=count[i]/size[i];cl[i]=rate;sigma=Math.sqrt(rate*(1-rate)/size[i]);lo[i]=Math.max(0,rate-3*sigma);hi[i]=Math.min(1,rate+3*sigma);}
   else if(kind.equals("NP")){y[i]=count[i];cl[i]=size[i]*rate;sigma=Math.sqrt(size[i]*rate*(1-rate));lo[i]=Math.max(0,cl[i]-3*sigma);hi[i]=Math.min(size[i],cl[i]+3*sigma);}
   else if(kind.equals("C")){y[i]=count[i];cl[i]=rate;sigma=Math.sqrt(rate);lo[i]=Math.max(0,rate-3*sigma);hi[i]=rate+3*sigma;}
   else{y[i]=count[i]/size[i];cl[i]=rate;sigma=Math.sqrt(rate/size[i]);lo[i]=Math.max(0,rate-3*sigma);hi[i]=rate+3*sigma;}
  }
  if(rate==0||((kind.equals("P")||kind.equals("NP"))&&rate==1))result.warning="界限退化：本批全为零或全不合格等；这种结果不证明过程稳定。\n";
  result.series.add(new Series(kind,y,lo,cl,hi));return result;
 }
 private static double[][] parse(String input){
  if(input==null||input.length()>100000)throw new IllegalArgumentException("数据缺失或超过100000字符");String text=input.trim().replace('；','\n').replace(';','\n');if(text.isEmpty())throw new IllegalArgumentException("请输入数据");String[] lines=text.split("\\r?\\n",-1);if(lines.length<2||lines.length>200)throw new IllegalArgumentException("输入2至200个按时间排列的组");double[][] result=new double[lines.length][];
  for(int i=0;i<lines.length;i++){String[] cells=lines[i].replace('，',',').replace('\t',',').split(",",-1);if(cells.length>100)throw new IllegalArgumentException("每组最多100值");result[i]=new double[cells.length];for(int j=0;j<cells.length;j++){String value=cells[j].trim();if(!NUMBER.matcher(value).matches())throw new IllegalArgumentException("第"+(i+1)+"组第"+(j+1)+"值无效，不自动删除缺测");double x=Double.parseDouble(value);if(!Double.isFinite(x)||Math.abs(x)>1e100)throw new IllegalArgumentException("数值超出范围");result[i][j]=x;}}
  return result;
 }
 static double c4(int n){double c=n%2==0?Math.sqrt(2/Math.PI):Math.sqrt(Math.PI)/2;for(int k=n%2==0?2:3;k<n;k+=2)c*=k/Math.sqrt((double)(k-1)*(k+1));return c;}
 private static Series fixed(String name,double[] values,double lo,double center,double hi){double[] l=new double[values.length],c=l.clone(),h=l.clone();java.util.Arrays.fill(l,lo);java.util.Arrays.fill(c,center);java.util.Arrays.fill(h,hi);return new Series(name,values,l,c,h);}
 private static String f(double x){return String.format(Locale.ROOT,"%.8g",x);}
}
