package local.aicenter.core;

public interface ModelEngine {
    String id();
    boolean isLocal();
    boolean isPaid();
    boolean ready();
    String generate(String prompt, StopController.Token token) throws Exception;
}
