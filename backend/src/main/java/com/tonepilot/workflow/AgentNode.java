package com.tonepilot.workflow;

public interface AgentNode {

    int order();

    String stepName();

    String agentName();

    default int maxRetries() {
        return 1;
    }

    boolean shouldExecute(TonePilotAgentContext context);

    void execute(TonePilotAgentContext context);

    default String inputSummary(TonePilotAgentContext context) {
        return "photoId=%s, targetStyle=%s, provider=%s".formatted(
                context.getPhotoId(),
                blankToDefault(context.getTargetStyle(), "未指定"),
                blankToDefault(context.getProvider(), "默认配置")
        );
    }

    default String outputSummary(TonePilotAgentContext context) {
        if (context.finalAdjustment() != null) {
            return "style=%s, validationMessages=%d".formatted(
                    context.finalAdjustment().style(),
                    context.getValidationMessages().size()
            );
        }
        if (context.getRetrievedKnowledge() != null && !context.getRetrievedKnowledge().isEmpty()) {
            return "retrievedKnowledge=%d".formatted(context.getRetrievedKnowledge().size());
        }
        if (context.getPhotoAnalysis() != null) {
            return "scene=%s, subject=%s".formatted(context.getPhotoAnalysis().scene(), context.getPhotoAnalysis().subject());
        }
        return "尚无输出";
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
