package team.anonyms.athletelink.coreservice.controllers;

import java.util.List;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import team.anonyms.athletelink.coreservice.dto.CompletionAcceptedDto;
import team.anonyms.athletelink.coreservice.dto.RecordRoundResultRequestDto;
import team.anonyms.athletelink.coreservice.dto.RoundSubmissionAcceptedDto;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.exceptions.ForbiddenOperationException;
import team.anonyms.athletelink.coreservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.coreservice.services.ActivityResultsService;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityResultsController.class)
class ActivityResultsControllerTests {

    private static final UUID REQUEST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORGANIZER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WINNER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID LOSER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID EVENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;
    @MockitoBean ActivityResultsService service;

    @Test
    void submitRoundReturnsAcceptedCommandAndPassesPlayers() throws Exception {
        RecordRoundResultRequestDto request = validRound();
        when(service.recordRoundResult(eq(REQUEST_ID), eq(2), eq(ORGANIZER_ID), any(RecordRoundResultRequestDto.class)))
                .thenReturn(new RoundSubmissionAcceptedDto(EVENT_ID, REQUEST_ID, 2));

        mockMvc.perform(roundPost(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.roundNumber").value(2));
        verify(service).recordRoundResult(REQUEST_ID, 2, ORGANIZER_ID, request);
    }

    @Test
    void completeReturnsConfirmationStatus() throws Exception {
        when(service.completeRequest(REQUEST_ID, ORGANIZER_ID))
                .thenReturn(new CompletionAcceptedDto(REQUEST_ID, RequestStatus.CONFIRMATION));

        mockMvc.perform(MockMvcRequestBuilders.post("/requests/{requestId}/complete", REQUEST_ID)
                        .header("X-User-Id", ORGANIZER_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMATION"));
        verify(service).completeRequest(REQUEST_ID, ORGANIZER_ID);
    }

    static Stream<Arguments> invalidRoundBodies() {
        return Stream.of(
                Arguments.of(new RecordRoundResultRequestDto(List.of(), List.of(LOSER_ID)), "winners"),
                Arguments.of(new RecordRoundResultRequestDto(List.of(WINNER_ID), List.of()), "losers"),
                Arguments.of(new RecordRoundResultRequestDto(null, List.of(LOSER_ID)), "winners"),
                Arguments.of(new RecordRoundResultRequestDto(List.of(WINNER_ID), null), "losers"),
                Arguments.of(new RecordRoundResultRequestDto(Arrays.asList((UUID) null), List.of(LOSER_ID)), "winners[0]"),
                Arguments.of(new RecordRoundResultRequestDto(List.of(WINNER_ID), Arrays.asList((UUID) null)), "losers[0]"));
    }

    @ParameterizedTest(name = "submit round rejects invalid {1}")
    @MethodSource("invalidRoundBodies")
    void submitRoundRejectsInvalidBody(RecordRoundResultRequestDto request, String field) throws Exception {
        mockMvc.perform(roundPost(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details['" + field + "']").exists());
        verifyNoInteractions(service);
    }

    @Test
    void submitRoundRejectsMalformedJson() throws Exception {
        mockMvc.perform(roundPost("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void submitRoundRejectsMalformedRoundNumber() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/{requestId}/rounds/no/result", REQUEST_ID)
                        .header("X-User-Id", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRound())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void submitRoundPassesNumericZeroToServiceForBusinessValidation() throws Exception {
        when(service.recordRoundResult(eq(REQUEST_ID), eq(0), eq(ORGANIZER_ID), any(RecordRoundResultRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Round number must be positive"));
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/{requestId}/rounds/0/result", REQUEST_ID)
                        .header("X-User-Id", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRound())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Round number must be positive"));
        verify(service).recordRoundResult(REQUEST_ID, 0, ORGANIZER_ID, validRound());
    }

    @Test
    void submitRoundRequiresOrganizerHeader() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/{requestId}/rounds/2/result", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRound())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void completeRequiresOrganizerHeader() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/{requestId}/complete", REQUEST_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void completeRejectsMalformedRequestId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/not-a-uuid/complete")
                        .header("X-User-Id", ORGANIZER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void submitRoundRejectsMalformedOrganizerId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/{requestId}/rounds/2/result", REQUEST_ID)
                        .header("X-User-Id", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validRound())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    static Stream<Arguments> roundFailures() {
        return Stream.of(
                Arguments.of(new IllegalArgumentException("Invalid player selection"), 400, "BAD_REQUEST"),
                Arguments.of(new ForbiddenOperationException("Not organizer"), 403, "FORBIDDEN"),
                Arguments.of(new ResourceNotFoundException("Request not found"), 404, "NOT_FOUND"),
                Arguments.of(new IllegalStateException("Request not active"), 409, "CONFLICT"));
    }

    @ParameterizedTest(name = "submit round maps service failure to HTTP {1}")
    @MethodSource("roundFailures")
    void submitRoundMapsServiceFailure(RuntimeException failure, int httpStatus, String code) throws Exception {
        when(service.recordRoundResult(eq(REQUEST_ID), eq(2), eq(ORGANIZER_ID), any(RecordRoundResultRequestDto.class)))
                .thenThrow(failure);
        mockMvc.perform(roundPost(jsonMapper.writeValueAsString(validRound())))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(failure.getMessage()))
                .andExpect(jsonPath("$.details").isEmpty());
        verify(service).recordRoundResult(REQUEST_ID, 2, ORGANIZER_ID, validRound());
    }

    static Stream<Arguments> completionFailures() {
        return Stream.of(
                Arguments.of(new ForbiddenOperationException("Not organizer"), 403, "FORBIDDEN"),
                Arguments.of(new ResourceNotFoundException("Request not found"), 404, "NOT_FOUND"),
                Arguments.of(new IllegalStateException("Request not active"), 409, "CONFLICT"));
    }

    @ParameterizedTest(name = "complete maps service failure to HTTP {1}")
    @MethodSource("completionFailures")
    void completeMapsServiceFailure(RuntimeException failure, int httpStatus, String code) throws Exception {
        when(service.completeRequest(REQUEST_ID, ORGANIZER_ID)).thenThrow(failure);
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/{requestId}/complete", REQUEST_ID)
                        .header("X-User-Id", ORGANIZER_ID))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(failure.getMessage()));
        verify(service).completeRequest(REQUEST_ID, ORGANIZER_ID);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder roundPost(String body) {
        return MockMvcRequestBuilders.post("/requests/{requestId}/rounds/2/result", REQUEST_ID)
                .header("X-User-Id", ORGANIZER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static RecordRoundResultRequestDto validRound() {
        return new RecordRoundResultRequestDto(List.of(WINNER_ID), List.of(LOSER_ID));
    }
}
