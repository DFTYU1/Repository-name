package local.aicenter.platform;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import local.aicenter.core.CachePolicy;
import local.aicenter.core.StopController;

public final class StorageManager {
    private StorageManager(){}
    public static long appBytes(Context context)throws IOException {
        return size(context.getDataDir());
    }
    private static long size(File root)throws IOException {
        if(Files.isSymbolicLink(root.toPath()))return 0;
        if(root.isFile())return root.length();
        File[] children=root.listFiles();if(children==null)return 0;
        long total=0;for(File file:children)total=Math.addExact(total,size(file));return total;
    }
    public static String summary(Context context,StopController.Token token)throws IOException {
        token.check();long total=appBytes(context);long cache=size(context.getCacheDir());
        return String.format(Locale.CHINA,"应用已用：%.2f GB / 30 GB 目标\n普通缓存：%.2f GB / 8 GB 默认上限\n设备剩余：%.2f GB\n\n普通缓存连续 15 天未使用自动清除。收藏、知识库、笔记、主动下载及工作文件不自动删除。",total/1e9,cache/1e9,context.getFilesDir().getUsableSpace()/1e9);
    }
    public static void markUsed(File entry)throws IOException {
        if(!entry.setLastModified(System.currentTimeMillis()))throw new IOException("无法更新缓存使用时间");
    }
    public static int maintainCache(Context context,StopController.Token token)throws IOException {
        // Only our collector cache, never filesDir or the parent's cache directories.
        File root=new File(context.getCacheDir(),"collector");if(!root.exists())return 0;
        if(Files.isSymbolicLink(root.toPath()))throw new SecurityException("缓存路径异常");
        File[] files=root.listFiles();if(files==null)return 0;
        List<CachePolicy.Entry> entries=new ArrayList<>();
        for(File file:files)if(file.isFile()&&!Files.isSymbolicLink(file.toPath()))entries.add(new CachePolicy.Entry(file.getName(),file.length(),file.lastModified(),CachePolicy.Kind.CACHE));
        List<String> ids=new CachePolicy().evict(entries,CachePolicy.DEFAULT_LIMIT,System.currentTimeMillis());
        int removed=0;
        for(String id:ids){token.check();File file=new File(root,id);if(!file.getCanonicalFile().getParentFile().equals(root.getCanonicalFile())||Files.isSymbolicLink(file.toPath()))throw new SecurityException("缓存路径异常");if(file.delete())removed++;}
        return removed;
    }
}
