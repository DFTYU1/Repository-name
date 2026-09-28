package local.aicenter.core;

import java.util.Locale;

/** Only explicit requests to inspect local app state are candidates for the read-only tool planner. */
public final class ReadOnlyToolIntent {
    private ReadOnlyToolIntent() {}
    public static String candidate(String input) {
        if(input==null)return "chat";
        String s=input.toLowerCase(Locale.ROOT).trim();
        boolean inspect=has(s,"查看","检查","显示","列出","查询","看一下","inspect","show","list","check");
        if(!inspect)return "chat";
        if(has(s,"设备剩余","可用存储","磁盘","存储空间","剩余空间","disk space","free space"))return "storage";
        if(has(s,"导入的文件","已导入文件","本地文件","导入资料","imported files","local files"))return "files";
        if(has(s,"任务记录","执行记录","历史任务","task history","task log"))return "tasks";
        return "chat";
    }
    private static boolean has(String text,String...words){for(String word:words)if(text.contains(word))return true;return false;}
}
