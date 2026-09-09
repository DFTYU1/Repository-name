package local.aicenter.core;

import java.util.ArrayList;
import java.util.List;

/** Local by default. Cloud enhancement is opt-in per request and approval never carries to another provider. */
public final class ModelRouter {
    public static final class Unavailable extends IllegalStateException {
        private static final long serialVersionUID = 1L;
        public Unavailable() { super("没有可用模型"); }
    }
    public interface CloudConsent { boolean allow(String provider, boolean paid, boolean privateContent); }
    public static final class Answer {
        public final String text, provider;
        public final List<String> failedProviders;
        Answer(String text, String provider, List<String> failures) {
            this.text = text; this.provider = provider;
            this.failedProviders = java.util.Collections.unmodifiableList(new ArrayList<>(failures));
        }
    }
    private final List<ModelEngine> engines;
    public ModelRouter(List<ModelEngine> engines) { this.engines = new ArrayList<>(engines); }
    public Answer answer(String prompt, boolean cloudEnhancement, boolean privateContent, CloudConsent consent, StopController.Token token) throws Exception {
        if (prompt == null || prompt.trim().isEmpty() || prompt.length() > 32_000) throw new IllegalArgumentException("问题为空或超过当前上下文限制");
        List<ModelEngine> ordered = new ArrayList<>();
        if (cloudEnhancement) for (ModelEngine engine : engines) if (!engine.isLocal()) ordered.add(engine);
        for (ModelEngine engine : engines) if (engine.isLocal()) ordered.add(engine);
        List<String> failures = new ArrayList<>();
        for (ModelEngine engine : ordered) {
            token.check();
            if (!engine.ready()) continue;
            if (!engine.isLocal() && !consent.allow(engine.id(), engine.isPaid(), privateContent)) continue;
            token.check();
            try {
                String text = engine.generate(prompt, token);
                token.check();
                if (text == null || text.trim().isEmpty()) throw new IllegalStateException("空响应");
                return new Answer(text, engine.id(), failures);
            } catch (StopController.Stopped e) { throw e; }
            catch (Exception e) { failures.add(engine.id()); }
        }
        throw new Unavailable();
    }
}
