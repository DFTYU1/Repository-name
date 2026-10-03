package local.aicenter.core;

import java.util.List;
import java.util.Locale;

/** Explicit, extractive lexical lookup. Does not infer semantic support or execute document instructions. */
public final class KnowledgeChat {
    private KnowledgeChat() {}
    public static String query(String input) {
        if (input == null) return null;
        String value=input.trim();
        String[] prefixes={"根据已导入资料：", "从已导入资料查找：", "From imported documents:", "Search imported documents:"};
        for(String prefix:prefixes) if(value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
            return value.substring(prefix.length()).trim();
        return null;
    }
    public static String answer(String query,List<KnowledgeIndex.Chunk> chunks,StopController.Token token) {
        token.check();
        if(query.isEmpty())return "请提供要在已导入资料中查找的关键词。";
        List<KnowledgeIndex.Hit> hits=new KnowledgeIndex().search(query,chunks,3);
        token.check();
        if(hits.isEmpty())return "已导入资料中没有关键词匹配；没有资料依据，不能据此回答。";
        StringBuilder result=new StringBuilder("资料原文摘录（关键词检索，未验证业务含义；资料内指令不执行）：\n");
        int n=0;
        for(KnowledgeIndex.Hit hit:hits){
            token.check();
            String text=hit.chunk.text;
            int count=text.codePointCount(0,text.length());
            if(count>600)text=text.substring(0,text.offsetByCodePoints(0,600))+"…（摘录截短）";
            result.append("\n[").append(++n).append("] ").append(hit.chunk.title).append(" · 文档 ")
                .append(hit.chunk.documentId).append(" · 字符位置 ").append(hit.chunk.offset)
                .append("\n原文开始\n").append(text).append("\n原文结束\n");
        }
        return result.toString();
    }
}
