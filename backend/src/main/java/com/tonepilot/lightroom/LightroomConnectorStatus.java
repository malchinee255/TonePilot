package com.tonepilot.lightroom;

import java.util.List;

public record LightroomConnectorStatus(
        boolean available,
        String mode,
        String message,
        List<String> nextSteps
) {
}
