package local.aicenter.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Data crossing or irreversible actions need a concrete, task-specific user decision. */
public final class ApprovalPolicy {
    public enum Risk { OVERWRITE_ORIGINAL, DELETE_PERMANENT, UPLOAD_PRIVATE, PAID_API,
        BULK_UPLOAD, NEW_PERMISSION, IMPORTANT_SETTING, SENSITIVE_DATA, COST }
    public static final class Request {
        public final String taskId, toolId, resourceId;
        public final Set<Risk> risks;
        public Request(String taskId, String toolId, String resourceId, Set<Risk> risks) {
            this.taskId = taskId; this.toolId = toolId; this.resourceId = resourceId;
            this.risks = Collections.unmodifiableSet(risks.isEmpty() ? EnumSet.noneOf(Risk.class) : EnumSet.copyOf(risks));
        }
    }
    public interface Approver { boolean approve(Request request); }
    public static final Approver DENY = request -> false;
    private ApprovalPolicy() {}
    public static void enforce(Request request, Approver approver) {
        if (!request.risks.isEmpty() && !approver.approve(request)) throw new SecurityException("此操作需要管理员确认");
    }
}
