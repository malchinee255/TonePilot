package com.tonepilot.repository.lightroom;

import com.tonepilot.domain.agent.*;
import com.tonepilot.repository.lightroom.*;




import java.nio.file.Path;
import java.util.Map;

public interface LightroomStateRepository {

    Map<String, Object> status();

    Map<String, Object> selectedPhoto();

    Path resultFile(String fileName);
}
