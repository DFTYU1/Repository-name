package local.aicenter.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/** A closed operation set. No expressions, scripts, reflection or file access. */
public final class LocalCalculation {
    private static final MathContext MC = MathContext.DECIMAL128;
    private LocalCalculation() {}
    public static String calculate(String op, String unit, String outputUnit, String[] values) {
        if(values==null||values.length==0||values.length>128)throw new IllegalArgumentException("需要1至128个明确数值");
        if(!Arrays.asList("scalar","count","mm","cm","m","g","kg","ratio","percent").contains(unit))
            throw new IllegalArgumentException("单位不明确或暂不支持；请统一单位");
        List<BigDecimal> v=new ArrayList<>();
        for(String s:values){
            if(s==null||s.length()>48||!s.matches("[+-]?[0-9]+(?:\\.[0-9]+)?"))throw new IllegalArgumentException("数值格式无效");
            BigDecimal number=new BigDecimal(s);
            if(unit.equals("count")&&(number.signum()<0||number.stripTrailingZeros().scale()>0))throw new IllegalArgumentException("件数必须为非负整数");
            v.add(number);
        }
        BigDecimal result;
        switch(op){
            case "sum": case "mean":
                result=BigDecimal.ZERO;for(BigDecimal x:v)result=result.add(x,MC);
                if(op.equals("mean"))result=result.divide(BigDecimal.valueOf(v.size()),MC);break;
            case "add":case "subtract":case "multiply":case "divide":
                arity(v,2);result=op.equals("add")?v.get(0).add(v.get(1),MC):op.equals("subtract")?v.get(0).subtract(v.get(1),MC):op.equals("multiply")?v.get(0).multiply(v.get(1),MC):v.get(0).divide(nonzero(v.get(1)),MC);
                if((op.equals("multiply")||op.equals("divide"))&&!unit.equals("scalar"))throw new IllegalArgumentException("乘除暂只支持无量纲数；不能猜测复合单位");break;
            case "fraction":
                arity(v,2);if(!unit.equals("count"))throw new IllegalArgumentException("比例需要同单位计数");
                if(v.get(0).signum()<0||v.get(1).signum()<=0||v.get(0).compareTo(v.get(1))>0)throw new IllegalArgumentException("部分数量必须在0与总数之间且总数大于0");
                for(BigDecimal x:v)if(x.stripTrailingZeros().scale()>0)throw new IllegalArgumentException("件数必须为整数");
                result=v.get(0).divide(v.get(1),MC);unit="ratio";break;
            case "cp":case "cpk":
                // Named protocol order: lower, upper, mean, within-process sigma.
                arity(v,4);BigDecimal l=v.get(0),u=v.get(1),m=v.get(2),sd=v.get(3);
                if(u.compareTo(l)<=0||sd.signum()<=0||unit.equals("percent")||unit.equals("ratio"))throw new IllegalArgumentException("请确认上下限、正标准差及一致单位");
                result=op.equals("cp")?u.subtract(l).divide(sd.multiply(new BigDecimal("6")),MC):u.subtract(m).min(m.subtract(l)).divide(sd.multiply(new BigDecimal("3")),MC);
                unit="scalar";break;
            default:throw new IllegalArgumentException("不支持的计算操作");
        }
        if(unit.equals("ratio")&&outputUnit.equals("percent"))result=result.multiply(new BigDecimal("100"),MC);
        else if(unit.equals("percent")&&outputUnit.equals("ratio"))result=result.divide(new BigDecimal("100"),MC);
        else if(!unit.equals(outputUnit))throw new IllegalArgumentException("不支持隐式单位换算，请明确统一单位");
        return result.stripTrailingZeros().toPlainString();
    }
    private static BigDecimal nonzero(BigDecimal x){if(x.signum()==0)throw new IllegalArgumentException("分母不能为零");return x;}
    private static void arity(List<BigDecimal> v,int n){if(v.size()!=n)throw new IllegalArgumentException("缺少参数或参数数量不符");}
}
