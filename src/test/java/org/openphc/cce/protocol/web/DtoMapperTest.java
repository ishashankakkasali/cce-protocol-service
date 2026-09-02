package org.openphc.cce.protocol.web;

import org.openphc.cce.common.entity.ActionDefinition;
import org.openphc.cce.common.entity.ProtocolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openphc.cce.common.entity.*;
import org.openphc.cce.common.enums.*;
import org.openphc.cce.protocol.web.dto.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtoMapperTest {

    private DtoMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mapper = new DtoMapper();
    }

    @Test
    void toDto_protocolDefinition_mapsAllFields() {
        JsonNode definition = objectMapper.valueToTree(Map.of("resourceType", "PlanDefinition"));
        ProtocolDefinition entity = ProtocolDefinition.builder()
                .id(UUID.randomUUID())
                .url("http://example.org/PlanDefinition/anc-high-risk")
                .version("1.0.0")
                .status(ProtocolDefinitionStatus.ACTIVE)
                .loadedAt(OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC))
                .definition(definition)
                .build();

        ProtocolDefinitionDto dto = mapper.toDto(entity);

        assertEquals(entity.getId(), dto.getId());
        assertEquals("http://example.org/PlanDefinition/anc-high-risk", dto.getUrl());
        assertEquals("1.0.0", dto.getVersion());
        assertEquals("http://example.org/PlanDefinition/anc-high-risk|1.0.0", dto.getCanonical());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(entity.getLoadedAt(), dto.getLoadedAt());
        assertSame(definition, dto.getDefinition());
    }

    @Test
    void toDtoProtocolDefinitionList_mapsAll() {
        ProtocolDefinition entity = ProtocolDefinition.builder()
                .id(UUID.randomUUID())
                .url("http://example.org/pd")
                .version("1.0")
                .status(ProtocolDefinitionStatus.ACTIVE)
                .loadedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .definition(objectMapper.createObjectNode())
                .build();

        List<ProtocolDefinitionDto> dtos = mapper.toDtoProtocolDefinitionList(List.of(entity));

        assertEquals(1, dtos.size());
        assertEquals(entity.getId(), dtos.get(0).getId());
    }

    @Test
    void toDtoProtocolDefinitionList_nullReturnsEmpty() {
        List<ProtocolDefinitionDto> dtos = mapper.toDtoProtocolDefinitionList(null);
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    @Test
    void toDto_actionDefinition_mapsAllFields() {
        JsonNode definition = objectMapper.valueToTree(Map.of("resourceType", "ActivityDefinition"));
        ActionDefinition entity = ActionDefinition.builder()
                .id(UUID.randomUUID())
                .canonicalUrl("http://openphc.org/ActivityDefinition/escalation-alert")
                .version("1.0")
                .name("escalation-alert")
                .title("Escalation Alert")
                .status(ActionDefinitionStatus.ACTIVE)
                .actionType(ActionDefinitionKind.CommunicationRequest)
                .definition(definition)
                .createdAt(OffsetDateTime.of(2026, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.of(2026, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .build();

        ActionDefinitionDto dto = mapper.toDto(entity);

        assertEquals(entity.getId(), dto.getId());
        assertEquals("http://openphc.org/ActivityDefinition/escalation-alert", dto.getCanonicalUrl());
        assertEquals("1.0", dto.getVersion());
        assertEquals("http://openphc.org/ActivityDefinition/escalation-alert|1.0", dto.getCanonical());
        assertEquals("escalation-alert", dto.getName());
        assertEquals("Escalation Alert", dto.getTitle());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals("CommunicationRequest", dto.getActionType());
        assertSame(definition, dto.getDefinition());
        assertEquals(entity.getCreatedAt(), dto.getCreatedAt());
        assertEquals(entity.getUpdatedAt(), dto.getUpdatedAt());
    }

    @Test
    void toDto_actionDefinition_nullableFieldsHandled() {
        ActionDefinition entity = ActionDefinition.builder()
                .id(UUID.randomUUID())
                .canonicalUrl("http://openphc.org/test")
                .version("1.0")
                .status(ActionDefinitionStatus.ACTIVE)
                .actionType(ActionDefinitionKind.Task)
                .definition(objectMapper.createObjectNode())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        ActionDefinitionDto dto = mapper.toDto(entity);

        assertNull(dto.getName());
        assertNull(dto.getTitle());
    }

    @Test
    void toDtoActionDefinitionList_mapsAll() {
        ActionDefinition entity = ActionDefinition.builder()
                .id(UUID.randomUUID())
                .canonicalUrl("http://openphc.org/test")
                .version("1.0")
                .status(ActionDefinitionStatus.ACTIVE)
                .actionType(ActionDefinitionKind.Task)
                .definition(objectMapper.createObjectNode())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        List<ActionDefinitionDto> dtos = mapper.toDtoActionDefinitionList(List.of(entity));

        assertEquals(1, dtos.size());
        assertEquals(entity.getId(), dtos.get(0).getId());
    }

    @Test
    void toDtoActionDefinitionList_nullReturnsEmpty() {
        List<ActionDefinitionDto> dtos = mapper.toDtoActionDefinitionList(null);
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }
}
