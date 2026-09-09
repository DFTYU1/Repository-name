package local.aicenter.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Bounded, journaled tool execution. The planner and inference provider are separate components. */
public final class AgentRuntime {
    public enum State { PLANNED, RUNNING, SUCCEEDED, FAILED, CANCELLED, INTERRUPTED }
    public interface Journal { void record(String taskId, State state, String toolId, String code); }
    public interface Tool {
        String id();
        Set<ApprovalPolicy.Risk> risks();
        String execute(String input, StopController.Token token) throws Exception;
    }
    public static final class Step {
        public final String toolId, input;
        public Step(String toolId, String input) { this.toolId = toolId; this.input = input; }
    }
    public static final class Result {
        public final String id, output, failedTool;
        public final State state;
        Result(String id, State state, String output, String failedTool) {
            this.id = id; this.state = state; this.output = output; this.failedTool = failedTool;
        }
    }
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final Journal journal;
    private final ApprovalPolicy.Approver approver;
    public AgentRuntime(Journal journal, ApprovalPolicy.Approver approver) { this.journal = journal; this.approver = approver; }
    public synchronized void register(Tool tool) {
        if (tool.id() == null || !tool.id().matches("[a-z][a-z0-9_.]{0,63}") || tools.containsKey(tool.id()))
            throw new IllegalArgumentException("工具 ID 无效或重复");
        tools.put(tool.id(), tool);
    }
    public Result execute(List<Step> plan, StopController.Token token) {
        if (plan == null || plan.isEmpty() || plan.size() > 16) throw new IllegalArgumentException("任务步骤必须在 1–16 步之间");
        List<Step> steps = new ArrayList<>(plan);
        String id = UUID.randomUUID().toString(), current = "";
        StringBuilder output = new StringBuilder();
        try {
            journal.record(id, State.PLANNED, "", "created");
            for (Step step : steps) {
                token.check();
                current = step.toolId;
                Tool tool;
                synchronized (this) { tool = tools.get(current); }
                if (tool == null) throw new IllegalArgumentException("未知工具");
                // The resource identifier is scoped to this step; no persistent blanket approval.
                ApprovalPolicy.enforce(new ApprovalPolicy.Request(id, current, step.input, tool.risks()), approver);
                token.check();
                journal.record(id, State.RUNNING, current, "started");
                String value = tool.execute(step.input, token);
                token.check();
                if (value == null || value.length() > 100_000) throw new IllegalStateException("工具输出无效或过长");
                if (output.length() > 0) output.append('\n');
                output.append(value);
            }
            token.check();
            journal.record(id, State.SUCCEEDED, current, "finished");
            return new Result(id, State.SUCCEEDED, output.toString(), "");
        } catch (StopController.Stopped e) {
            safeRecord(id, State.CANCELLED, current, "stopped");
            return new Result(id, State.CANCELLED, "任务已停止，已完成的本地内容保留。", current);
        } catch (Exception e) {
            if (e instanceof ModelRouter.Unavailable) {
                safeRecord(id, State.FAILED, current, "model_unavailable");
                return new Result(id, State.FAILED, "本地模型尚未接入或暂不可用，当前无法生成 AI 回答。仍可查看文件、空间或查找已导入的资料。", current);
            }
            safeRecord(id, State.FAILED, current, e instanceof SecurityException ? "approval_required" : "tool_failed");
            return new Result(id, State.FAILED, "步骤「"+current+"」未完成。"+(e instanceof SecurityException ? "需要明确授权。" : "请查看任务状态后重试。"), current);
        }
    }
    private void safeRecord(String id, State state, String tool, String code) {
        try { journal.record(id, state, tool, code); } catch (RuntimeException ignored) { /* Do not crash error handling. */ }
    }
    public synchronized List<String> availableTools() { return Collections.unmodifiableList(new ArrayList<>(tools.keySet())); }
}
