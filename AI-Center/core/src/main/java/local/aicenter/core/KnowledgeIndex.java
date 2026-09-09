package local.aicenter.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Source-preserving lexical retrieval. Vector embeddings are a separate pending provider. */
public final class KnowledgeIndex {
    public static final class Chunk {
        public final String documentId, title, text;
        public final int offset;
        public Chunk(String documentId, String title, String text, int offset) {
            this.documentId = documentId; this.title = title; this.text = text; this.offset = offset;
        }
    }
    public static final class Hit {
        public final Chunk chunk;
        public final double score;
        Hit(Chunk chunk, double score) { this.chunk = chunk; this.score = score; }
    }
    public static List<Chunk> chunk(String id, String title, String text, int codePoints, int overlap) {
        if (id == null || title == null || text == null || codePoints < 16 || overlap < 0 || overlap >= codePoints)
            throw new IllegalArgumentException("分块参数无效");
        List<Chunk> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int length = Math.min(codePoints, text.codePointCount(start, text.length()));
            int end = text.offsetByCodePoints(start, length);
            result.add(new Chunk(id, title, text.substring(start, end), start));
            if (end == text.length()) break;
            start = text.offsetByCodePoints(end, -overlap);
        }
        return result;
    }
    public List<Hit> search(String query, List<Chunk> chunks, int limit) {
        if (query == null || query.length() > 2000 || limit < 1 || limit > 20) throw new IllegalArgumentException("检索条件无效");
        Set<String> terms = new LinkedHashSet<>(tokens(query));
        if (terms.isEmpty() || chunks.isEmpty()) return Collections.emptyList();
        List<List<String>> documents = new ArrayList<>();
        Map<String, Integer> df = new HashMap<>();
        double totalLength = 0;
        for (Chunk chunk : chunks) {
            List<String> words = tokens(chunk.text);
            documents.add(words); totalLength += words.size();
            for (String term : new LinkedHashSet<>(words)) df.put(term, df.getOrDefault(term, 0)+1);
        }
        double average = Math.max(1, totalLength/chunks.size());
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String,Integer> tf = new HashMap<>();
            for (String word : documents.get(i)) tf.put(word, tf.getOrDefault(word, 0)+1);
            double score = 0;
            for (String term : terms) {
                int count = tf.getOrDefault(term, 0);
                if (count == 0) continue;
                int matches = df.getOrDefault(term, 0);
                double idf = Math.log(1+(chunks.size()-matches+0.5)/(matches+0.5));
                score += idf*count*2.2/(count+1.2*(0.25+0.75*documents.get(i).size()/average));
            }
            if (score > 0) hits.add(new Hit(chunks.get(i), score));
        }
        hits.sort(Comparator.comparingDouble((Hit h) -> h.score).reversed()
                .thenComparing(h -> h.chunk.documentId).thenComparingInt(h -> h.chunk.offset));
        return new ArrayList<>(hits.subList(0, Math.min(limit, hits.size())));
    }
    private static List<String> tokens(String input) {
        String text = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        int previousHan = -1;
        for (int pos = 0; pos < text.length();) {
            int c = text.codePointAt(pos); pos += Character.charCount(c);
            boolean han = Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
            if (han) {
                if (word.length() > 0) { result.add(word.toString()); word.setLength(0); }
                if (previousHan >= 0) result.add(new String(Character.toChars(previousHan))+new String(Character.toChars(c)));
                result.add(new String(Character.toChars(c)));
                previousHan = c;
            } else {
                previousHan = -1;
                if (Character.isLetterOrDigit(c)) word.appendCodePoint(c);
                else if (word.length() > 0) { result.add(word.toString()); word.setLength(0); }
            }
        }
        if (word.length() > 0) result.add(word.toString());
        return result;
    }
}
