package com.tonepilot.repository.lightroom;

import com.tonepilot.domain.agent.*;
import com.tonepilot.repository.lightroom.*;




import java.util.List;
import java.util.Map;

public interface LightroomToolRepository {

    Map<String, Object> applyAdjustments(
            Map<String, Object> developSettings,
            List<Map<String, Object>> localAdjustments
    );

    default Map<String, Object> applyDevelopSettings(Map<String, Object> developSettings) {
        return applyAdjustments(developSettings, List.of());
    }

    Map<String, Object> applyStatus(String jobId);
}
