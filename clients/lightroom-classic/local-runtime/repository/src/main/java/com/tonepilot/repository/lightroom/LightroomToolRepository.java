package com.tonepilot.repository.lightroom;

import com.tonepilot.domain.agent.*;
import com.tonepilot.repository.lightroom.*;




import java.util.Map;

public interface LightroomToolRepository {

    Map<String, Object> applyDevelopSettings(Map<String, Object> developSettings);

    Map<String, Object> applyStatus(String jobId);
}
