package local.aicenter.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** User turns retain their boundaries; unrelated tasks cannot supply calculator operands. */
public final class CalculationRequest {
    public final String current;
    public final String source;
    public final String prompt;
    private CalculationRequest(String current, List<String[]> relevant) {
        this.current=current;
        StringBuilder values=new StringBuilder();
        for(String[] turn:relevant)values.append(turn[1]).append('\n');
        source=values.append(current).toString();
        prompt=ConversationPrompt.build(relevant,current,12000);
    }
    public static CalculationRequest from(String current,List<String[]> history) {
        if(current==null||current.trim().isEmpty())throw new IllegalArgumentException("当前问题为空");
        List<String[]> relevant=new ArrayList<>();
        if(followup(current)&&history!=null){
            List<String[]> users=new ArrayList<>();
            for(String[] turn:history)if(turn!=null&&turn.length>=2&&"user".equals(turn[0])&&turn[1]!=null)users.add(turn);
            // The production caller has already persisted the current user message.
            if(!users.isEmpty()&&users.get(users.size()-1)[1].equals(current))users.remove(users.size()-1);
            String requested=family(current);
            for(int i=users.size()-1;i>=0;i--){
                String prior=users.get(i)[1];
                String priorFamily=family(prior);
                if(!requested.isEmpty()&&!requested.equals(priorFamily))continue;
                relevant.add(0,users.get(i));
                if(!followup(prior))break;
                if(relevant.size()>=4)break;
            }
        }
        return new CalculationRequest(current,relevant);
    }
    private static boolean followup(String text){
        return Pattern.compile("^\\s*(?:补充|继续刚才|接着刚才|沿用|关于刚才|follow[ -]?up\\b|continuing\\b)",Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
    static String family(String text){
        String lower=text.toLowerCase(Locale.ROOT);
        if(Pattern.compile("\\bcpk\\b").matcher(lower).find()||lower.contains("cpk"))return "cpk";
        if(Pattern.compile("\\bcp\\b").matcher(lower).find())return "cp";
        if(Pattern.compile("百分比|百分数|不良率|缺陷率|\\b(?:percentage|percent|rate)\\b",Pattern.CASE_INSENSITIVE).matcher(text).find())return "fraction";
        return "";
    }
}
