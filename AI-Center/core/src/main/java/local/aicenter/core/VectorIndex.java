package local.aicenter.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Exact cosine retrieval for real embedding vectors. No hash vectors or fabricated embeddings. */
public final class VectorIndex {
    public interface Embedder { float[] embed(String text, StopController.Token token) throws Exception; }
    public static final class Entry {
        public final KnowledgeIndex.Chunk chunk;
        private final float[] vector;
        public Entry(KnowledgeIndex.Chunk chunk, float[] vector) { this.chunk = chunk; this.vector = normalize(vector); }
    }
    public static final class Match {
        public final KnowledgeIndex.Chunk chunk;
        public final double similarity;
        Match(KnowledgeIndex.Chunk chunk, double similarity) { this.chunk = chunk; this.similarity = similarity; }
    }
    private final int dimensions;
    public VectorIndex(int dimensions) {
        if (dimensions < 1 || dimensions > 8192) throw new IllegalArgumentException("向量维度无效");
        this.dimensions = dimensions;
    }
    public List<Match> search(float[] query, List<Entry> entries, int limit, StopController.Token token) {
        if (query.length != dimensions || limit < 1 || limit > 20) throw new IllegalArgumentException("向量检索参数无效");
        float[] q = normalize(query);
        List<Match> result = new ArrayList<>();
        for (Entry entry : entries) {
            token.check();
            if (entry.vector.length != dimensions) throw new IllegalArgumentException("不能混用不同向量模型");
            double dot = 0;
            for (int i = 0; i < dimensions; i++) dot += q[i]*entry.vector[i];
            result.add(new Match(entry.chunk, dot));
        }
        result.sort(Comparator.comparingDouble((Match match) -> match.similarity).reversed());
        return new ArrayList<>(result.subList(0, Math.min(limit, result.size())));
    }
    private static float[] normalize(float[] vector) {
        if (vector == null || vector.length == 0) throw new IllegalArgumentException("空向量");
        double sum = 0;
        for (float value : vector) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("无效向量数值");
            sum += (double)value*value;
        }
        if (sum == 0) throw new IllegalArgumentException("零向量");
        float[] result = vector.clone();
        double norm = Math.sqrt(sum);
        for (int i = 0; i < result.length; i++) result[i] /= norm;
        return result;
    }
}
