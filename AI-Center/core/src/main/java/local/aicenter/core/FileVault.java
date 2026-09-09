package local.aicenter.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

/** Copies into an app-owned root. It never modifies a source URI or accepts absolute output paths. */
public final class FileVault {
    public static final class Imported {
        public final String storedName, sha256;
        public final long size;
        Imported(String name, String sha256, long size) { this.storedName = name; this.sha256 = sha256; this.size = size; }
    }
    private final Path root;
    public FileVault(Path root) throws IOException {
        Files.createDirectories(root);
        if (Files.isSymbolicLink(root)) throw new SecurityException("沙盒根目录不能是链接");
        this.root = root.toRealPath();
    }
    public Imported importCopy(InputStream source, String displayName, long maxBytes, StopController.Token token) throws IOException {
        if (source == null || maxBytes <= 0) throw new IllegalArgumentException("无效输入");
        String suffix = extension(displayName);
        if (!suffix.matches("txt|md|csv|pdf|docx|xlsx|pptx|png|jpg|jpeg|wav|mp3|mp4|gguf"))
            throw new SecurityException("当前文件类型不允许导入");
        token.check();
        Path temp = Files.createTempFile(root, "import-", ".part");
        Path target = root.resolve(UUID.randomUUID()+"_AI副本."+suffix);
        long total = 0;
        MessageDigest digest = sha256Digest();
        try {
            try (OutputStream out = Files.newOutputStream(temp, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[64*1024];
                int read;
                while ((read = source.read(buffer)) != -1) {
                    token.check();
                    if (read == 0) continue;
                    if (read > maxBytes-total) throw new IOException("文件超过当前导入上限");
                    out.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                    total += read;
                }
                out.flush();
            }
            token.check();
            try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(temp, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temp, target); // No REPLACE_EXISTING: originals are never overwritten.
            return new Imported(target.getFileName().toString(), hex(digest.digest()), total);
        } finally { Files.deleteIfExists(temp); }
    }
    public String readText(String storedName, int maxBytes, StopController.Token token) throws IOException {
        if (maxBytes <= 0 || !extension(storedName).matches("txt|md|csv")) throw new IllegalArgumentException("当前只提取文本内容");
        Path path = resolveExisting(storedName);
        if (Files.size(path) > maxBytes) throw new IOException("文本超过当前分析上限");
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (InputStream in = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[8192]; int size;
            while ((size = in.read(buffer)) != -1) {
                token.check();
                if (size > maxBytes-output.size()) throw new IOException("文本超过当前分析上限");
                output.write(buffer, 0, size);
            }
        }
        token.check();
        return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(output.toByteArray())).toString();
    }
    public Path resolveExisting(String storedName) throws IOException {
        if (storedName == null || storedName.isEmpty() || storedName.contains("/") || storedName.contains("\\") || storedName.equals(".") || storedName.equals(".."))
            throw new SecurityException("路径不在授权沙盒中");
        Path path = root.resolve(storedName).normalize();
        if (!path.getParent().equals(root) || Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new SecurityException("文件不存在或路径不安全");
        return path;
    }
    private static String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot+1).toLowerCase(Locale.ROOT);
    }
    private static MessageDigest sha256Digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String hex(byte[] data) {
        StringBuilder value = new StringBuilder();
        for (byte b : data) value.append(String.format(Locale.ROOT, "%02x", b & 255));
        return value.toString();
    }
}
