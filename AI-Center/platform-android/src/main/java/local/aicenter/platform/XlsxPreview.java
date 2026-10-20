package local.aicenter.platform;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import local.aicenter.core.LeaseBook;
import local.aicenter.core.StopController;
import local.aicenter.core.XlsxReader;

/** One selected URI, read-only descriptor, revocable task lease, no persisted grants/copy/network. */
public final class XlsxPreview {
    private XlsxPreview() {}
    public static String read(ContentResolver resolver,Uri uri,LeaseBook leases,StopController stop,StopController.Token token)throws Exception{
        return XlsxReader.read(new java.io.ByteArrayInputStream(load(resolver,uri,leases,stop,token)),token).preview();
    }
    public static byte[] load(ContentResolver resolver,Uri uri,LeaseBook leases,StopController stop,StopController.Token token)throws Exception{
        if(!"content".equals(uri.getScheme()))throw new IllegalArgumentException("请选择系统文件选择器中的xlsx文件");
        String task=java.util.UUID.randomUUID().toString(),lease=leases.grant(task,uri.toString(),token);CancellationSignal signal=new CancellationSignal();
        try(AutoCloseable cancel=stop.onStop(token,signal::cancel)){
            ParcelFileDescriptor descriptor=resolver.openFileDescriptor(uri,"r",signal);if(descriptor==null)throw new IOException("No document");
            try(InputStream raw=new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
                AutoCloseable close=stop.onStop(token,()->{try{raw.close();}catch(IOException ignored){}})){
                InputStream checked=new FilterInputStream(raw){
                    @Override public int read()throws IOException{leases.check(lease,uri.toString());return super.read();}
                    @Override public int read(byte[] b,int off,int len)throws IOException{leases.check(lease,uri.toString());return in.read(b,off,len);}
                };
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[8192];int n;
                while((n=checked.read(block))!=-1){token.check();if(n>XlsxReader.MAX_FILE-bytes.size())throw new IOException("文件超过10MiB");bytes.write(block,0,n);}
                byte[] result=bytes.toByteArray();XlsxReader.read(new java.io.ByteArrayInputStream(result),token);return result;
            }
        }finally{leases.revoke(lease);}
    }
}
