package org.openphc.cce.protocol.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.openphc.cce.common.enums.ActionDefinitionStatus;
import org.openphc.cce.common.enums.ProtocolDefinitionStatus;
import org.openphc.cce.common.repository.ActionDefinitionRepository;
import org.openphc.cce.protocol.domain.repository.ProtocolDefinitionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics for the definitional plane.
 *
 * <p>Only what this service can actually observe: how much active definitional content it is
 * serving. Event-processing, matching and intelligence counters belong to the services that do that
 * work and are registered there, so a scrape of this service never reports a metric it cannot move.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public MeterBinder protocolMetrics(ProtocolDefinitionRepository protocolDefinitionRepository,
                                       ActionDefinitionRepository actionDefinitionRepository) {
        return registry -> {
            Gauge.builder("cce.protocol.definitions.active",
                            protocolDefinitionRepository,
                            repo -> repo.countByStatus(ProtocolDefinitionStatus.ACTIVE))
                    .description("Number of ACTIVE protocol definitions available for matching")
                    .register(registry);

            Gauge.builder("cce.action.definitions.active",
                            actionDefinitionRepository,
                            repo -> repo.countByStatus(ActionDefinitionStatus.ACTIVE))
                    .description("Number of ACTIVE action definitions available for resolution")
                    .register(registry);
        };
    }
}
