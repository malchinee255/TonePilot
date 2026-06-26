package com.tonepilot.agent;

import com.tonepilot.domain.Photo;
import com.tonepilot.domain.PhotoAnalysis;

public interface ImageAnalysisAgent {

    PhotoAnalysis analyze(Photo photo);
}
