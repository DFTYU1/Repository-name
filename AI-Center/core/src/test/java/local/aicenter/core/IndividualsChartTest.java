package local.aicenter.core;
import java.util.*;
public final class IndividualsChartTest {
 private static int checks;
 private static void near(double a,double b){checks++;if(Math.abs(a-b)>1e-9)throw new AssertionError(a+" != "+b);}
 private static void eq(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  IndividualsChart.Result r=IndividualsChart.calculate("1,2,3",true);near(r.mean,2);near(r.mrMean,1);near(r.lower,2-3/1.128);near(r.upper,2+3/1.128);near(r.mrUpper,3.267);eq(r.individualSignals.size(),0);eq(r.rangeSignals.size(),0);
  r=IndividualsChart.calculate("1,1,1,1,100",true);near(r.mean,20.8);near(r.mrMean,24.75);eq(r.individualSignals,Arrays.asList(5));eq(r.rangeSignals,Arrays.asList(5));
  r=IndividualsChart.calculate("-3，-2\n-1",true);near(r.mean,-2);near(r.mrMean,1);eq(r.values.length,3);
  for(String bad:new String[]{"","1","1,1,1","1,,2","1, ,2","1,2,","NaN,1","1,Infinity","1,NA,3","1,1e101"}){boolean rejected=false;try{IndividualsChart.calculate(bad,true);}catch(IllegalArgumentException e){rejected=true;}eq(rejected,true);}
  boolean confirmed=false;try{IndividualsChart.calculate("1,2,3",false);}catch(IllegalArgumentException e){confirmed=true;}eq(confirmed,true);
  System.out.println("PASS I-MR calculations: "+checks+" assertions (host only; only single-point limit rule)");
 }
}
