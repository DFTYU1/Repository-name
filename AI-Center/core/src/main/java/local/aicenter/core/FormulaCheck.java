package local.aicenter.core;

import java.util.*;
/** Deliberately limited signature/range checks, never a claim of business correctness. */
public final class FormulaCheck {
    private FormulaCheck(){}
    public static String check(String formula){
        if(formula==null||formula.length()>2048)return "INVALID: 公式缺失或过长";
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("=([A-Za-z]+)\\((.*)\\)").matcher(formula.trim());
        if(!m.matches())return "UNVERIFIED: 仅支持单层函数调用";
        String fn=m.group(1).toUpperCase(Locale.ROOT);List<String>a=new ArrayList<>();boolean q=false;StringBuilder b=new StringBuilder();
        for(char c:m.group(2).toCharArray()){
            if(c=='\"')q=!q;
            if(!q&&(c=='('||c==')'))return "UNVERIFIED: 嵌套公式需进一步审核";
            if(c==','&&!q){a.add(b.toString().trim());b.setLength(0);}else b.append(c);
        }
        if(q)return "INVALID: 引号未闭合";a.add(b.toString().trim());
        try{
            switch(fn){
                case "SUM":case "AVERAGE":case "COUNT":
                    require(a.size()==1,"该校验器仅支持单个范围");range(a.get(0));break;
                case "SUMIF":
                    require(a.size()==3,"SUMIF需要条件范围、条件、求和范围");same(a.get(0),a.get(2));criterion(a.get(1));break;
                case "SUMIFS":
                    require(a.size()>=3&&a.size()%2==1,"SUMIFS需要求和范围和成对条件");
                    for(int i=1;i<a.size();i+=2){same(a.get(0),a.get(i));criterion(a.get(i+1));}break;
                case "MATCH":
                    require(a.size()==3&&a.get(2).equals("0"),"仅支持MATCH显式精确匹配");cell(a.get(0));oneDimension(a.get(1));break;
                case "XLOOKUP":
                    require(a.size()==4||a.size()==5,"仅支持4或5参数XLOOKUP");cell(a.get(0));oneDimension(a.get(1));same(a.get(1),a.get(2));criterion(a.get(3));
                    if(a.size()==5)require(a.get(4).equals("0"),"仅支持精确匹配");break;
                default:return "UNVERIFIED: 不支持或未知函数 "+fn;
            }
            return "STRUCTURE_ONLY: 函数签名和范围结构检查通过；未验证参数与业务意图是否一致、单元格类型及实际结果";
        }catch(IllegalArgumentException e){return "INVALID: "+e.getMessage();}
    }
    private static void criterion(String s){require(s.matches("\"[^\"]*\"|[+-]?[0-9]+(?:\\.[0-9]+)?"),"条件需为文本或数值字面量");}
    private static int[] cell(String s){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\$?([A-Za-z]{1,3})\\$?([1-9][0-9]{0,6})").matcher(s);
        require(m.matches(),"无效单元格引用");int col=0;for(char c:m.group(1).toUpperCase(Locale.ROOT).toCharArray())col=col*26+c-'A'+1;
        int row=Integer.parseInt(m.group(2));require(col<=16384&&row<=1048576,"引用超出Excel边界");return new int[]{col,row};
    }
    private static int[] range(String s){String[] p=s.split(":",-1);require(p.length==1||p.length==2,"无效范围");int[] l=cell(p[0]),r=cell(p[p.length-1]);require(r[0]>=l[0]&&r[1]>=l[1],"范围逆序");return new int[]{r[0]-l[0]+1,r[1]-l[1]+1};}
    private static void same(String a,String b){require(Arrays.equals(range(a),range(b)),"范围维度不一致");}
    private static void oneDimension(String a){int[]d=range(a);require(d[0]==1||d[1]==1,"查找范围必须为一维");}
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalArgumentException(reason);}
}
