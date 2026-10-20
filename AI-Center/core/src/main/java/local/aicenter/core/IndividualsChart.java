package local.aicenter.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Phase-I span-2 I-MR estimates, not a full stability or capability decision. */
public final class IndividualsChart {
 private IndividualsChart(){}
 public static final String SOURCE="https://www.itl.nist.gov/div898/handbook/pmc/section3/pmc322.htm";
 public static final class Result {
  public final double[] values,ranges;public final double mean,mrMean,lower,upper,mrUpper;public final List<Integer> individualSignals=new ArrayList<>(),rangeSignals=new ArrayList<>();
  Result(double[] x,double[] r,double m,double mr){values=x;ranges=r;mean=m;mrMean=mr;lower=m-3*mr/1.128;upper=m+3*mr/1.128;mrUpper=3.267*mr;for(int i=0;i<x.length;i++)if(x[i]<lower||x[i]>upper)individualSignals.add(i+1);for(int i=0;i<r.length;i++)if(r[i]>mrUpper)rangeSignals.add(i+2);}
  public String report(){return String.format(Locale.ROOT,"I图：CL≈%.6f；LCL≈%.6f；UCL≈%.6f\nMR图：CL≈%.6f；LCL=0；UCL≈%.6f\nI图越界点序号：%s\nMR图越界位置（后一个观测序号）：%s\n",mean,lower,upper,mrMean,mrUpper,individualSignals,rangeSignals)+"仅检查单点超出估计3σ界限；未执行趋势/连串等其他判异规则。界限从本批数据估计，不能直接证明过程稳定，也不能当规格上下限。时间顺序必须正确；测量、采样、分布与独立性需另行核实。";}
 }
 public static Result calculate(String input,boolean timeOrderConfirmed){
  if(!timeOrderConfirmed)throw new IllegalArgumentException("请确认数据按采样时间顺序且单位一致");
  if(input==null||input.length()>20000)throw new IllegalArgumentException("数据过长");String text=input.trim();if(text.isEmpty())throw new IllegalArgumentException("请输入观测值");
  if(text.matches("(?s).*[,，;；]\\s*[,，;；].*")||text.matches("(?s)^[,，;；].*|.*[,，;；]$"))throw new IllegalArgumentException("数据存在空项，不自动删除缺测");
  String[] items=text.split("[\\s,，;；]+",-1);if(items.length<2||items.length>500)throw new IllegalArgumentException("输入2至500个单值观测，不混入标题或缺测");double[] values=new double[items.length];double mean=0;
  for(int i=0;i<items.length;i++){if(!items[i].matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]{1,3})?"))throw new IllegalArgumentException("第"+(i+1)+"个值无效；不自动删除缺测或错误数据");values[i]=Double.parseDouble(items[i]);if(!Double.isFinite(values[i])||Math.abs(values[i])>1e100)throw new IllegalArgumentException("数值超出计算范围");mean+=(values[i]-mean)/(i+1);}
  double[] ranges=new double[values.length-1];double mr=0;for(int i=0;i<ranges.length;i++){ranges[i]=Math.abs(values[i+1]-values[i]);mr+=(ranges[i]-mr)/(i+1);}
  if(mr==0)throw new IllegalArgumentException("移动极差为零，无法用本批数据估计有效控制界限，请核对测量分辨率与采样");return new Result(values,ranges,mean,mr);
 }
}
