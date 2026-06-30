package com.tonepilot.repository.runtime;

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
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;






import com.tonepilot.domain.runtime.RuntimeDeviceRegistrationRequest;
import com.tonepilot.domain.runtime.RuntimeDeviceRegistrationResponse;
import com.tonepilot.domain.runtime.RuntimeEventRecord;
import com.tonepilot.domain.runtime.RuntimeEventRequest;

import java.util.List;

public interface RuntimeIngestRepository {

    RuntimeDeviceRegistrationResponse registerDevice(RuntimeDeviceRegistrationRequest request);

    RuntimeEventRecord recordEvent(RuntimeEventRequest request);

    List<RuntimeEventRecord> listEvents(String userId);
}
