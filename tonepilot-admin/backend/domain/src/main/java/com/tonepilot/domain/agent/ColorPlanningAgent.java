package com.tonepilot.domain.agent;

import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;







import com.tonepilot.domain.colorgrading.ColorAdjustment;
import com.tonepilot.domain.photo.PhotoAnalysis;
import com.tonepilot.domain.knowledge.RagSearchItem;

import java.util.List;

public interface ColorPlanningAgent {

    ColorAdjustment plan(Long photoId, String targetStyle, PhotoAnalysis analysis, List<RagSearchItem> knowledgeItems);
}
