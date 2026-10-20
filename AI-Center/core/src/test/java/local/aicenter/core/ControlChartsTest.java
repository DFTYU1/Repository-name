package local.aicenter.core;
import java.util.*;
public final class ControlChartsTest {
 private static int checks;
 private static void near(double a,double b){checks++;if(Math.abs(a-b)>1e-8)throw new AssertionError(a+" != "+b);}
 private static void eq(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 private static void reject(String kind,String data){boolean bad=false;try{ControlCharts.calculate(kind,data,true);}catch(IllegalArgumentException e){bad=true;}eq(bad,true);}
 public static void main(String[] args){
  ControlCharts.Result r=ControlCharts.calculate("XBAR_R","1,3\n3,5\n5,7",true);eq(r.series.size(),2);near(r.series.get(0).center[0],4);near(r.series.get(0).lower[0],.24);near(r.series.get(0).upper[0],7.76);near(r.series.get(1).center[0],2);near(r.series.get(1).upper[0],6.534);eq(r.series.get(0).signals.size(),0);
  r=ControlCharts.calculate("XBAR_S","1,2,3\n2,3,4",true);near(r.series.get(0).center[0],2.5);near(r.series.get(0).lower[0],.5455899523883203);near(r.series.get(0).upper[0],4.454410047611679);near(r.series.get(1).center[0],1);near(r.series.get(1).upper[0],2.5681696026311895);near(r.series.get(1).lower[0],0);near(ControlCharts.c4(5),.9399856029866253);near(ControlCharts.c4(10),.9726592741215883);
  r=ControlCharts.calculate("P","10,100\n40,200",true);ControlCharts.Series s=r.series.get(0);near(s.center[0],1.0/6);near(s.values[0],.1);near(s.values[1],.2);near(s.lower[0],.05486326779167716);near(s.upper[0],.27847006554165615);eq(s.upper[0]>s.upper[1],true);eq(r.report().contains("逐组界限"),true);
  s=ControlCharts.calculate("NP","10,100\n20,100",true).series.get(0);near(s.center[0],15);near(s.lower[0],4.287857357185725);near(s.upper[0],25.712142642814275);
  s=ControlCharts.calculate("C","1\n2\n30",true).series.get(0);near(s.center[0],11);near(s.lower[0],1.0501256289338006);near(s.upper[0],20.9498743710662);eq(s.signals,Arrays.asList(1,3));
  s=ControlCharts.calculate("U","10,100\n40,200",true).series.get(0);near(s.center[0],1.0/6);near(s.lower[0],.04419217952750776);near(s.upper[0],.28914115380582556);eq(s.upper[0]>s.upper[1],true);
  r=ControlCharts.calculate("P","0,10\n0,20",true);eq(r.warning.contains("退化"),true);near(r.series.get(0).lower[0],0);near(r.series.get(0).upper[1],0);
  r=ControlCharts.calculate("P","10,10\n20,20",true);near(r.series.get(0).upper[0],1);eq(r.warning.contains("退化"),true);
  near(ControlCharts.calculate("U","3,0.5\n4,1",true).series.get(0).values[0],6);
  for(String[] bad:new String[][]{{"XBAR_R","1,2\n1,2,3"},{"XBAR_R","1,1\n2,2"},{"XBAR_S","1\n2"},{"P","11,10\n1,10"},{"P","1,2.5\n2,10"},{"NP","1,10\n1,20"},{"C","1.5\n2"},{"C","-1\n2"},{"C","1\n\n2"},{"U","1,0\n2,1"},{"U","1,NaN\n2,10"},{"P","1,,10\n2,10"},{"BAD","1\n2"}})reject(bad[0],bad[1]);
  boolean confirmed=false;try{ControlCharts.calculate("C","1\n2",false);}catch(IllegalArgumentException e){confirmed=true;}eq(confirmed,true);
  System.out.println("PASS six additional SPC chart types: "+checks+" assertions (host only; no full stability decision)");
 }
}
