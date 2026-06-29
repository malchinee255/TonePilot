package com.tonepilot.persistence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tonepilot.persistence")
public class PersistenceProperties {

    private boolean enabled = true;
}
