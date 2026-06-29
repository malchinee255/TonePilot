package com.tonepilot.workflow.node;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.service.PhotoService;
import com.tonepilot.workflow.AgentNode;
import com.tonepilot.workflow.TonePilotAgentContext;
import org.springframework.stereotype.Component;

@Component
public class ImageAnalysisAgentNode implements AgentNode {

    private final PhotoService photoService;

    @Autowired
    public ImageAnalysisAgentNode(PhotoService photoService) {
        this.photoService = photoService;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public String stepName() {
        return "image-analysis";
    }

    @Override
    public String agentName() {
        return "ImageAnalysisAgent";
    }

    @Override
    public boolean shouldExecute(TonePilotAgentContext context) {
        return true;
    }

    @Override
    public void execute(TonePilotAgentContext context) {
        PhotoAnalysis analysis = photoService.latestAnalysisOrAnalyze(context.getPhotoId(), context.getProvider());
        context.setPhotoAnalysis(analysis);
    }
}


