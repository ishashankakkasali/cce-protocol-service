package org.openphc.cce.protocol.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.openphc.cce.common.entity.ActionDefinition;
import org.openphc.cce.common.enums.ActionDefinitionStatus;
import org.openphc.cce.common.enums.ActionDefinitionKind;
import org.openphc.cce.protocol.service.ActionDefinitionService;
import org.openphc.cce.protocol.web.DtoMapper;
import org.openphc.cce.common.exception.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = {ActionDefinitionController.class, DtoMapper.class, GlobalExceptionHandler.class})
class ActionDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ActionDefinitionService actionDefinitionService;

    private static final UUID ACTION_DEF_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

    private ActionDefinition buildActionDefinition() {
        ActionDefinition def = new ActionDefinition();
        def.setId(ACTION_DEF_ID);
        def.setCanonicalUrl("http://openphc.org/ActivityDefinition/escalation-alert");
        def.setVersion("1.0");
        def.setName("escalation-alert");
        def.setTitle("Escalation Alert");
        def.setStatus(ActionDefinitionStatus.ACTIVE);
        def.setActionType(ActionDefinitionKind.CommunicationRequest);
        def.setDefinition(objectMapper.createObjectNode().put("resourceType", "ActivityDefinition"));
        def.setCreatedAt(OffsetDateTime.of(2026, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        def.setUpdatedAt(OffsetDateTime.of(2026, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        return def;
    }

    // --- POST / (create) ---

    @Test
    void create_validJson_returns201() throws Exception {
        ActionDefinition actionDef = buildActionDefinition();
        when(actionDefinitionService.createActionDefinition(any())).thenReturn(actionDef);

        String requestBody = objectMapper.writeValueAsString(
                Map.of("definitionJson", "{\"resourceType\":\"ActivityDefinition\",\"url\":\"http://openphc.org/ActivityDefinition/escalation-alert\",\"version\":\"1.0\",\"kind\":\"CommunicationRequest\"}"));

        mockMvc.perform(post("/v1/protocol/action-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ACTION_DEF_ID.toString()))
                .andExpect(jsonPath("$.canonicalUrl").value("http://openphc.org/ActivityDefinition/escalation-alert"))
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.canonical").value("http://openphc.org/ActivityDefinition/escalation-alert|1.0"))
                .andExpect(jsonPath("$.name").value("escalation-alert"))
                .andExpect(jsonPath("$.title").value("Escalation Alert"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.actionType").value("CommunicationRequest"));

        verify(actionDefinitionService).createActionDefinition(any());
    }

    @Test
    void create_blankJson_returns400() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("definitionJson", ""));

        mockMvc.perform(post("/v1/protocol/action-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(actionDefinitionService);
    }

    @Test
    void create_duplicateUrlVersion_returns400() throws Exception {
        when(actionDefinitionService.createActionDefinition(any()))
                .thenThrow(new IllegalArgumentException(
                        "Action definition already exists for url=http://openphc.org/test, version=1.0"));

        String requestBody = objectMapper.writeValueAsString(
                Map.of("definitionJson", "{\"resourceType\":\"ActivityDefinition\"}"));

        mockMvc.perform(post("/v1/protocol/action-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Action definition already exists for url=http://openphc.org/test, version=1.0"));
    }

    // --- GET / (listAll) ---

    @Test
    void listAll_returnsDefinitions() throws Exception {
        ActionDefinition actionDef = buildActionDefinition();
        Page<ActionDefinition> page = new PageImpl<>(List.of(actionDef));
        when(actionDefinitionService.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/v1/protocol/action-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ACTION_DEF_ID.toString()))
                .andExpect(jsonPath("$.content[0].name").value("escalation-alert"));
    }

    @Test
    void listAll_empty_returnsEmptyList() throws Exception {
        Page<ActionDefinition> page = new PageImpl<>(List.of());
        when(actionDefinitionService.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/v1/protocol/action-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listAll_filterByStatus_returnsFiltered() throws Exception {
        ActionDefinition actionDef = buildActionDefinition();
        Page<ActionDefinition> page = new PageImpl<>(List.of(actionDef));
        when(actionDefinitionService.findByStatus(eq(ActionDefinitionStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/v1/protocol/action-definitions")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        verify(actionDefinitionService).findByStatus(eq(ActionDefinitionStatus.ACTIVE), any(Pageable.class));
        verify(actionDefinitionService, never()).findAll(any(Pageable.class));
    }

    // --- GET /{id} (getById) ---

    @Test
    void getById_found_returns200() throws Exception {
        ActionDefinition actionDef = buildActionDefinition();
        when(actionDefinitionService.findById(ACTION_DEF_ID)).thenReturn(actionDef);

        mockMvc.perform(get("/v1/protocol/action-definitions/{id}", ACTION_DEF_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTION_DEF_ID.toString()))
                .andExpect(jsonPath("$.canonical").value(
                        "http://openphc.org/ActivityDefinition/escalation-alert|1.0"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(actionDefinitionService.findById(ACTION_DEF_ID))
                .thenThrow(new EntityNotFoundException("Action definition not found: " + ACTION_DEF_ID));

        mockMvc.perform(get("/v1/protocol/action-definitions/{id}", ACTION_DEF_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Action definition not found: " + ACTION_DEF_ID));
    }

    // --- PUT /{id} (update) ---

    @Test
    void update_validJson_returns200() throws Exception {
        ActionDefinition updatedDef = buildActionDefinition();
        updatedDef.setTitle("Updated Alert");
        when(actionDefinitionService.updateActionDefinition(eq(ACTION_DEF_ID), any())).thenReturn(updatedDef);

        String requestBody = objectMapper.writeValueAsString(
                Map.of("definitionJson", "{\"resourceType\":\"ActivityDefinition\",\"url\":\"http://openphc.org/ActivityDefinition/escalation-alert\",\"version\":\"1.0\",\"kind\":\"CommunicationRequest\"}"));

        mockMvc.perform(put("/v1/protocol/action-definitions/{id}", ACTION_DEF_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Alert"));

        verify(actionDefinitionService).updateActionDefinition(eq(ACTION_DEF_ID), any());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(actionDefinitionService.updateActionDefinition(eq(ACTION_DEF_ID), any()))
                .thenThrow(new EntityNotFoundException("Action definition not found: " + ACTION_DEF_ID));

        String requestBody = objectMapper.writeValueAsString(
                Map.of("definitionJson", "{\"resourceType\":\"ActivityDefinition\"}"));

        mockMvc.perform(put("/v1/protocol/action-definitions/{id}", ACTION_DEF_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    // --- POST /{id}/retire ---

    @Test
    void retire_active_returns200() throws Exception {
        ActionDefinition actionDef = buildActionDefinition();
        actionDef.setStatus(ActionDefinitionStatus.RETIRED);
        when(actionDefinitionService.retireActionDefinition(ACTION_DEF_ID)).thenReturn(actionDef);

        mockMvc.perform(post("/v1/protocol/action-definitions/{id}/retire", ACTION_DEF_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETIRED"));
    }

    @Test
    void retire_alreadyRetired_returns409() throws Exception {
        when(actionDefinitionService.retireActionDefinition(ACTION_DEF_ID))
                .thenThrow(new IllegalStateException("Action definition is already retired: " + ACTION_DEF_ID));

        mockMvc.perform(post("/v1/protocol/action-definitions/{id}/retire", ACTION_DEF_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Action definition is already retired: " + ACTION_DEF_ID));
    }

    // --- DELETE /{id} ---

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(actionDefinitionService).deleteActionDefinition(ACTION_DEF_ID);

        mockMvc.perform(delete("/v1/protocol/action-definitions/{id}", ACTION_DEF_ID))
                .andExpect(status().isNoContent());

        verify(actionDefinitionService).deleteActionDefinition(ACTION_DEF_ID);
    }

    @Test
    void delete_serviceRejectsDeletion_returns409() throws Exception {
        doThrow(new IllegalStateException("Cannot delete action definition: " + ACTION_DEF_ID))
                .when(actionDefinitionService).deleteActionDefinition(ACTION_DEF_ID);

        mockMvc.perform(delete("/v1/protocol/action-definitions/{id}", ACTION_DEF_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Cannot delete action definition: " + ACTION_DEF_ID));
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        // definitionJson is carried as a string, so a syntax error survives request binding and is
        // only found when the controller reads it — it must surface as a 400, not a 500.
        String requestBody = objectMapper.writeValueAsString(
                Map.of("definitionJson", "{\"resourceType\": "));

        mockMvc.perform(post("/v1/protocol/action-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid JSON")));

        verifyNoInteractions(actionDefinitionService);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Action definition not found: " + ACTION_DEF_ID))
                .when(actionDefinitionService).deleteActionDefinition(ACTION_DEF_ID);

        mockMvc.perform(delete("/v1/protocol/action-definitions/{id}", ACTION_DEF_ID))
                .andExpect(status().isNotFound());
    }
}
