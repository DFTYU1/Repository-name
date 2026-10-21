package local.aicenter.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.apache.commons.statistics.distribution.FDistribution;

/** Balanced, replicated fixed-effects 2^k factorial analysis; no fractional or blocked designs. */
public final class FactorialDesign {
 private FactorialDesign(){}
 public static final String SOURCE="https://www.itl.nist.gov/div898/handbook/pri/section3/pri3332.htm";
 public static final class Term {
  public final String name;public final double effect,coefficient,ss,f,p;
  Term(String name,double b,int n,double mse,int df){this.name=name;coefficient=b;effect=2*b;ss=n*b*b;f=mse>0?ss/mse:Double.NaN;p=mse>0?FDistribution.of(1,df).survivalProbability(f):Double.NaN;}
 }
 public static final class Result {
  public final String unit;public final int factors,repeats,runs,errorDf;public final double mean,sse,mse,sst;public final List<Term> terms;
  Result(int k,int r,double m,double e,double total,List<Term> terms,String unit){this.unit=unit;factors=k;repeats=r;runs=(1<<k)*r;errorDf=runs-(1<<k);mean=m;sse=e;mse=e/errorDf;sst=total;this.terms=Collections.unmodifiableList(new ArrayList<>(terms));}
  public String report(){StringBuilder b=new StringBuilder("两水平全因子DOE · 固定效应完整模型\nk="+factors+"；每组合独立重复="+repeats+"；总运行="+runs+"\n响应单位："+unit+"；SS/MSE单位为响应单位²\n总体均值="+f(mean)+"\n效应=正号组均值−负号组均值；编码回归系数=效应/2。\n项：效应 / 系数 / SS（df=1） / F / p\n");for(Term t:terms)b.append(t.name).append(": ").append(f(t.effect)).append(" / ").append(f(t.coefficient)).append(" / ").append(f(t.ss)).append(" / ").append(f(t.f)).append(" / ").append(f(t.p)).append('\n');b.append("重复误差：SS=").append(f(sse)).append("；df=").append(errorDf).append("；MSE=").append(f(mse)).append("\n总变差：SS=").append(f(sst)).append("；df=").append(runs-1).append('\n');if(mse==0)b.append("重复误差为零或数值分辨率不足，F/p未定义；不能宣称效应已被确定证明。\n");return b.append("未做多重比较校正；p值不表示工程重要性或因果证明。需独立重复、随机运行、合理量具、等方差和误差模型适用；重复读同一件不等于独立试验。保留全部交互项；存在交互时不能只看主效应。两水平无中心点，不能检验曲率、外推或直接给出最优工艺。无分区/区组/混料/分数因子分析。").toString();}
 }
 private static void bounds(int k,int r){if(k<2||k>4||r<2||r>10)throw new IllegalArgumentException("支持2至4因素，每组合2至10次独立重复");}
 private static String header(int k){StringBuilder h=new StringBuilder("run");for(int i=0;i<k;i++)h.append(',').append((char)('A'+i));return h.append(",Y").toString();}
 /** Deterministic replay of a random order; blank Y must be replaced with actual observations. */
 public static String plan(int factors,int repeats,long seed){bounds(factors,repeats);List<Integer> cells=new ArrayList<>();for(int r=0;r<repeats;r++)for(int c=0;c<(1<<factors);c++)cells.add(c);Collections.shuffle(cells,new Random(seed));StringBuilder out=new StringBuilder(header(factors)).append('\n');for(int i=0;i<cells.size();i++){out.append(i+1);for(int j=0;j<factors;j++)out.append(',').append((cells.get(i)&(1<<j))==0?-1:1);out.append(",\n");}return out.toString();}
 private static double number(String s){s=s.trim();if(!s.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]{1,3})?"))throw new IllegalArgumentException("运行编号、因素编码与Y须为有限数值，不接受缺测");double x=Double.parseDouble(s);if(!Double.isFinite(x)||Math.abs(x)>1e50)throw new IllegalArgumentException("数值超出范围");return x;}
 public static Result analyze(int k,int r,String input,String unit,boolean confirmed){bounds(k,r);if(unit==null||unit.trim().isEmpty()||unit.trim().length()>24||unit.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("请填写1至24字符的响应单位（无量纲填scalar）");unit=unit.trim();if(!confirmed)throw new IllegalArgumentException("请先确认DOE设计、独立重复、随机运行与响应单位");if(input==null||input.length()>100000||input.trim().isEmpty())throw new IllegalArgumentException("请输入DOE结果，最多100000字符");String[] rows=input.trim().replace("\r\n","\n").replace('；','\n').replace(';','\n').split("[\\r\\n]",-1);int first=rows[0].trim().equals(header(k))?1:0,n=(1<<k)*r,cells=1<<k;if(rows.length-first!=n)throw new IllegalArgumentException("运行数须为"+n+"；不能缺行、重复表头或合并不同试验");int[] mask=new int[n],count=new int[cells];boolean[] ids=new boolean[n];double[] y=new double[n],means=new double[cells];double mean=0;
  for(int i=0;i<n;i++){String[] fields=rows[i+first].replace('，',',').replace('\t',',').split(",",-1);if(fields.length!=k+2)throw new IllegalArgumentException("第"+(i+1)+"运行须有编号、"+k+"个因素编码、Y");double id=number(fields[0]);if(id!=Math.rint(id)||id<1||id>n||ids[(int)id-1])throw new IllegalArgumentException("运行编号须为1至"+n+"且不可重复");ids[(int)id-1]=true;int c=0;for(int j=0;j<k;j++){double x=number(fields[j+1]);if(x!=-1&&x!=1)throw new IllegalArgumentException("因素只接受-1和+1编码；不自动换算实际水平");if(x==1)c|=1<<j;}mask[i]=c;y[i]=number(fields[k+1]);count[c]++;means[c]+=(y[i]-means[c])/count[c];mean+=(y[i]-mean)/(i+1);}
  for(int c:count)if(c!=r)throw new IllegalArgumentException("每个因素组合必须恰有"+r+"次独立重复；设计不平衡");double sse=0,sst=0;for(int i=0;i<n;i++){sse+=Math.pow(y[i]-means[mask[i]],2);sst+=Math.pow(y[i]-mean,2);}List<Term> terms=new ArrayList<>();for(int t=1;t<cells;t++){double coefficient=0;for(int i=0;i<n;i++){int sign=1;for(int j=0;j<k;j++)if((t&(1<<j))!=0&&(mask[i]&(1<<j))==0)sign=-sign;coefficient+=sign*(y[i]-mean)/n;}StringBuilder label=new StringBuilder();for(int j=0;j<k;j++)if((t&(1<<j))!=0)label.append((char)('A'+j));terms.add(new Term(label.toString(),coefficient,n,sse/(n-cells),n-cells));}return new Result(k,r,mean,sse,sst,terms,unit);
 }
 private static String f(double x){return Double.isFinite(x)?String.format(Locale.ROOT,"%.8g",x):"未定义";}
}
