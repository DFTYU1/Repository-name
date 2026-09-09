package local.aicenter.platform;

import local.aicenter.core.ModelEngine;
import local.aicenter.core.StopController;

/** Explicit capability state until a licensed real model and its native runtime are integrated. */
public final class PendingLocalEngine implements ModelEngine {
    public String id(){return "local-model-pending";}
    public boolean isLocal(){return true;}
    public boolean isPaid(){return false;}
    public boolean ready(){return false;}
    public String generate(String prompt,StopController.Token token){
        token.check();throw new IllegalStateException("本地推理库与模型权重尚未接入，不能生成 AI 回答");
    }
}
