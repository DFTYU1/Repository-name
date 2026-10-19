package local.aicenter.core;
import java.math.BigDecimal;
import java.math.MathContext;
/** Bounded decimal expression parser: no eval, no prose operand extraction. */
public final class ArithmeticExpression {
 private static final MathContext MC=MathContext.DECIMAL128;
 private final String text;private int pos,depth,operations;private boolean approximate;
 private ArithmeticExpression(String text){this.text=text;}
 public static String answer(String input){
  String text=input.trim().replace('＋','+').replace('－','-').replace('×','*').replace('÷','/').replace('（','(').replace('）',')').replace('＝','=');
  text=text.replaceFirst("(?i)^(?:请帮我计算|请计算|计算|算一下|what is|calculate)\\s*[:：]?\\s*","");
  text=text.replaceFirst("\\s*(?:等于多少|等于几|是多少|是几|=\\s*多少|=\\s*几|=)?[?？。]*$","").trim();
  if(text.length()>256||!text.matches("[0-9.\\s+*/()\\-]+")||!text.matches(".*[+*/\\-].*"))return null;
  try{ArithmeticExpression p=new ArithmeticExpression(text);BigDecimal v=p.expression();p.space();if(p.pos!=text.length())throw new IllegalArgumentException("算式格式不完整");return v.stripTrailingZeros().toPlainString()+(p.approximate?"（约，34位有效数字）":"");}
  catch(ArithmeticException|IllegalArgumentException e){return "算式无法计算："+e.getMessage()+"。";}
 }
 private BigDecimal expression(){BigDecimal v=term();while(true){if(take('+')){count();v=v.add(term());}else if(take('-')){count();v=v.subtract(term());}else return v;}}
 private BigDecimal term(){BigDecimal v=factor();while(true){if(take('*')){count();v=v.multiply(factor());}else if(take('/')){count();BigDecimal d=factor();if(d.signum()==0)throw new ArithmeticException("除数不能为0");try{v=v.divide(d);}catch(ArithmeticException nonTerminating){v=v.divide(d,MC);approximate=true;}}else return v;}}
 private BigDecimal factor(){if(++depth>32)throw new IllegalArgumentException("括号或符号嵌套过深");try{if(take('+'))return factor();if(take('-'))return factor().negate();if(take('(')){BigDecimal v=expression();if(!take(')'))throw new IllegalArgumentException("括号未闭合");return v;}space();int start=pos;while(pos<text.length()&&(Character.isDigit(text.charAt(pos))||text.charAt(pos)=='.'))pos++;if(start==pos||pos-start>64)throw new IllegalArgumentException("数值缺失或过长");try{return new BigDecimal(text.substring(start,pos));}catch(NumberFormatException e){throw new IllegalArgumentException("数值格式错误");}}finally{depth--;}}
 private void count(){if(++operations>64)throw new IllegalArgumentException("算式过长");}
 private void space(){while(pos<text.length()&&Character.isWhitespace(text.charAt(pos)))pos++;}
 private boolean take(char c){space();if(pos<text.length()&&text.charAt(pos)==c){pos++;return true;}return false;}
}
