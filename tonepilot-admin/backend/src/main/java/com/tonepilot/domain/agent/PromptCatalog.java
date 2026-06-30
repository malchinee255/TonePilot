package com.tonepilot.domain.agent;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.observability.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.common.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.config.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.shared.security.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.server.dto.*;


public final class PromptCatalog {

    public static final String PHOTO_ANALYSIS_PROMPT = """
            你是一个专业摄影后期分析助手。请分析用户上传或 Lightroom 当前选中的照片。
            请只输出 JSON，不要输出多余解释。
            输出字段：scene, subject, exposureIssues, whiteBalanceIssues, colorIssues, recommendedStyles, summary。
            """;

    public static final String COLOR_PLANNING_PROMPT = """
            你是一个专业摄影后期调色师，擅长 Lightroom 参数化调色。
            你不能生成图片，只能生成 Lightroom 风格调色参数。
            输出必须是严格 JSON，并包含 style, reason, basic, hsl, effects, extended, steps。
            basic 包含曝光、对比度、高光、阴影、白色色阶、黑色色阶、色温、色调、纹理、清晰度、去朦胧、自然饱和度、饱和度。
            hsl 包含红、橙、黄、绿、浅绿、蓝、紫、洋红的色相、饱和度、明亮度。
            effects 包含 grain 和 vignette。
            extended 只在确实需要时填写 Lightroom 其它 Develop Settings，例如曲线、细节、降噪、颜色分级、镜头校正、变换、暗角细项、颗粒细项和相机校准。
            不要修改用户没有要求、也不是当前风格必需的参数，尤其不要随意改白平衡、裁剪、透视和镜头校正。
            """;

    public static final String STYLE_ANALYSIS_PROMPT = """
            你是一个专业摄影后期分析师。
            请分析输入摄影作品的整体调色风格，但不能声称知道真实 Lightroom 参数。
            只输出 JSON，不要输出多余解释。
            """;

    public static final String KNOWLEDGE_GENERATION_PROMPT = """
            你是一个摄影调色知识库构建助手。
            请根据 AI 风格分析结果生成可用于 RAG 检索的调色知识。
            不要声称复刻某个摄影师，只沉淀通用调色方法论。
            """;

    private PromptCatalog() {
    }
}
