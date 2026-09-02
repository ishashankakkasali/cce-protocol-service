package org.openphc.cce.protocol.config;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.openphc.cce.common.enums.ActionDefinitionStatus;
import org.openphc.cce.common.enums.ProtocolDefinitionStatus;
import org.openphc.cce.common.repository.ActionDefinitionRepository;
import org.openphc.cce.protocol.domain.repository.ProtocolDefinitionRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObservabilityConfigTest {

    @Test
    void registersGaugesForTheActiveDefinitionalContent() {
        ProtocolDefinitionRepository protocols = mock(ProtocolDefinitionRepository.class);
        ActionDefinitionRepository actions = mock(ActionDefinitionRepository.class);
        when(protocols.countByStatus(ProtocolDefinitionStatus.ACTIVE)).thenReturn(3L);
        when(actions.countByStatus(ActionDefinitionStatus.ACTIVE)).thenReturn(11L);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterBinder binder = new ObservabilityConfig().protocolMetrics(protocols, actions);
        binder.bindTo(registry);

        assertEquals(3.0, registry.get("cce.protocol.definitions.active").gauge().value());
        assertEquals(11.0, registry.get("cce.action.definitions.active").gauge().value());
    }

    @Test
    void countsOnlyActiveDefinitions() {
        // A retired protocol is still a row but is no longer available for matching, so it must not
        // be reported as capacity this service is serving.
        ProtocolDefinitionRepository protocols = mock(ProtocolDefinitionRepository.class);
        ActionDefinitionRepository actions = mock(ActionDefinitionRepository.class);
        when(protocols.countByStatus(ProtocolDefinitionStatus.ACTIVE)).thenReturn(1L);
        when(actions.countByStatus(ActionDefinitionStatus.ACTIVE)).thenReturn(0L);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ObservabilityConfig().protocolMetrics(protocols, actions).bindTo(registry);

        assertEquals(1.0, registry.get("cce.protocol.definitions.active").gauge().value());
        assertEquals(0.0, registry.get("cce.action.definitions.active").gauge().value());
        verify(protocols, never()).countByStatus(ProtocolDefinitionStatus.RETIRED);
    }
}
