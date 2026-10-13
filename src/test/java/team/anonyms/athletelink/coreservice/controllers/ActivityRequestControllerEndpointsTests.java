package team.anonyms.athletelink.coreservice.controllers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestDetailsDto;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestFeedDto;
import team.anonyms.athletelink.coreservice.dto.CreateActivityRequestDto;
import team.anonyms.athletelink.coreservice.dto.UpdateActivityRequestDto;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.exceptions.ForbiddenOperationException;
import team.anonyms.athletelink.coreservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.coreservice.services.ActivityRequestService;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityRequestController.class)
class ActivityRequestControllerEndpointsTests {

    private static final UUID REQUEST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TARGET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant EVENT_DATE = Instant.now().plus(30, ChronoUnit.DAYS);

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;
    @MockitoBean ActivityRequestService service;

    private enum Action {
        JOIN("join", 200), LEAVE("leave", 200), CANCEL("cancel", 200),
        KICK("kick/" + TARGET_ID, 200), CLOSE("registration/close", 204),
        OPEN("registration/open", 204), START("start", 204);

        final String suffix;
        final int successStatus;

        Action(String suffix, int successStatus) {
            this.suffix = suffix;
            this.successStatus = successStatus;
        }
    }

    private MockHttpServletRequestBuilder actionRequest(Action action) {
        return MockMvcRequestBuilders.post("/requests/{id}/" + action.suffix, REQUEST_ID)
                .header("X-User-Id", USER_ID);
    }

    private void verifyAction(Action action) {
        switch (action) {
            case JOIN -> verify(service).joinRequest(REQUEST_ID, USER_ID);
            case LEAVE -> verify(service).leaveRequest(REQUEST_ID, USER_ID);
            case CANCEL -> verify(service).cancelRequest(REQUEST_ID, USER_ID);
            case KICK -> verify(service).kickParticipant(REQUEST_ID, TARGET_ID, USER_ID);
            case CLOSE -> verify(service).closeRegistration(REQUEST_ID, USER_ID);
            case OPEN -> verify(service).openRegistration(REQUEST_ID, USER_ID);
            case START -> verify(service).startRequest(REQUEST_ID, USER_ID);
        }
    }

    private void failAction(Action action, RuntimeException exception) {
        switch (action) {
            case JOIN -> doThrow(exception).when(service).joinRequest(REQUEST_ID, USER_ID);
            case LEAVE -> doThrow(exception).when(service).leaveRequest(REQUEST_ID, USER_ID);
            case CANCEL -> doThrow(exception).when(service).cancelRequest(REQUEST_ID, USER_ID);
            case KICK -> doThrow(exception).when(service).kickParticipant(REQUEST_ID, TARGET_ID, USER_ID);
            case CLOSE -> doThrow(exception).when(service).closeRegistration(REQUEST_ID, USER_ID);
            case OPEN -> doThrow(exception).when(service).openRegistration(REQUEST_ID, USER_ID);
            case START -> doThrow(exception).when(service).startRequest(REQUEST_ID, USER_ID);
        }
    }

    @ParameterizedTest(name = "{0} delegates and returns its documented success status")
    @EnumSource(Action.class)
    void actionsDelegateAndReturnSuccess(Action action) throws Exception {
        mockMvc.perform(actionRequest(action))
                .andExpect(status().is(action.successStatus))
                .andExpect(content().string(""));
        verifyAction(action);
    }

    static Stream<Arguments> actionFailures() {
        return Stream.of(
                Arguments.of(Action.JOIN, 404), Arguments.of(Action.JOIN, 409),
                Arguments.of(Action.LEAVE, 404), Arguments.of(Action.LEAVE, 409),
                Arguments.of(Action.CANCEL, 403), Arguments.of(Action.CANCEL, 404), Arguments.of(Action.CANCEL, 409),
                Arguments.of(Action.KICK, 400), Arguments.of(Action.KICK, 403),
                Arguments.of(Action.KICK, 404), Arguments.of(Action.KICK, 409),
                Arguments.of(Action.CLOSE, 403), Arguments.of(Action.CLOSE, 404), Arguments.of(Action.CLOSE, 409),
                Arguments.of(Action.OPEN, 403), Arguments.of(Action.OPEN, 404), Arguments.of(Action.OPEN, 409),
                Arguments.of(Action.START, 403), Arguments.of(Action.START, 404), Arguments.of(Action.START, 409));
    }

    @ParameterizedTest(name = "{0} maps service failure to HTTP {1}")
    @MethodSource("actionFailures")
    void actionsMapServiceFailures(Action action, int httpStatus) throws Exception {
        RuntimeException exception = switch (httpStatus) {
            case 400 -> new IllegalArgumentException("Invalid action");
            case 403 -> new ForbiddenOperationException("Only the organizer may do this");
            case 404 -> new ResourceNotFoundException("Request not found");
            case 409 -> new IllegalStateException("Request state conflicts with action");
            default -> throw new IllegalArgumentException("Unexpected status: " + httpStatus);
        };
        String code = switch (httpStatus) {
            case 400 -> "BAD_REQUEST";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            default -> throw new IllegalArgumentException("Unexpected status: " + httpStatus);
        };
        failAction(action, exception);

        mockMvc.perform(actionRequest(action))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(exception.getMessage()))
                .andExpect(jsonPath("$.details").isEmpty());
        verifyAction(action);
    }

    @ParameterizedTest(name = "{0} rejects missing caller ID")
    @EnumSource(Action.class)
    void actionsRequireCallerHeader(Action action) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/{id}/" + action.suffix, REQUEST_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "{0} rejects malformed request ID")
    @EnumSource(Action.class)
    void actionsRejectMalformedRequestId(Action action) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/not-a-uuid/" + action.suffix)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void kickRejectsMalformedTargetId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/{id}/kick/not-a-uuid", REQUEST_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void joinRejectsMalformedCallerId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests/{id}/join", REQUEST_ID)
                        .header("X-User-Id", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    static Stream<Arguments> invalidCreateBodies() {
        return Stream.of(
                Arguments.of(createDto(" ", null, 3, "Moscow", 55.7, 37.6), "title"),
                Arguments.of(createDto("x".repeat(101), null, 3, "Moscow", 55.7, 37.6), "title"),
                Arguments.of(createDto("Match", "x".repeat(501), 3, "Moscow", 55.7, 37.6), "description"),
                Arguments.of(createDto("Match", null, 0, "Moscow", 55.7, 37.6), "numberOfRounds"),
                Arguments.of(createDto("Match", null, 11, "Moscow", 55.7, 37.6), "numberOfRounds"),
                Arguments.of(createDto("Match", null, 3, " ", 55.7, 37.6), "addressText"),
                Arguments.of(createDto("Match", null, 3, "Moscow", -90.1, 37.6), "latitude"),
                Arguments.of(createDto("Match", null, 3, "Moscow", 90.1, 37.6), "latitude"),
                Arguments.of(createDto("Match", null, 3, "Moscow", 55.7, -180.1), "longitude"),
                Arguments.of(createDto("Match", null, 3, "Moscow", 55.7, 180.1), "longitude"));
    }

    @ParameterizedTest(name = "create rejects invalid {1}")
    @MethodSource("invalidCreateBodies")
    void createRejectsInvalidBody(CreateActivityRequestDto dto, String field) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details." + field).exists());
        verifyNoInteractions(service);
    }

    @Test
    void createRejectsMalformedCallerId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validCreate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void createMapsUnknownSportToNotFound() throws Exception {
        CreateActivityRequestDto dto = validCreate();
        when(service.createRequest(dto, USER_ID))
                .thenThrow(new ResourceNotFoundException("Sport not found"));

        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Sport not found"));
        verify(service).createRequest(dto, USER_ID);
    }

    @Test
    void updateReturnsAllDetailsAndPassesBodyToService() throws Exception {
        UpdateActivityRequestDto update = validUpdate();
        ActivityRequestDetailsDto details = details();
        when(service.updateRequest(eq(REQUEST_ID), eq(USER_ID), any(UpdateActivityRequestDto.class)))
                .thenReturn(details);

        mockMvc.perform(MockMvcRequestBuilders.put("/requests/{id}", REQUEST_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.title").value("Updated match"))
                .andExpect(jsonPath("$.maxPlayers").value(8))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.registrationOpen").value(true))
                .andExpect(jsonPath("$.organizerId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.participants").isArray());
        verify(service).updateRequest(REQUEST_ID, USER_ID, update);
    }

    static Stream<Arguments> invalidUpdates() {
        return Stream.of(
                Arguments.of(new UpdateActivityRequestDto(" ", null, EVENT_DATE, 2, 8), "title"),
                Arguments.of(new UpdateActivityRequestDto("x".repeat(101), null, EVENT_DATE, 2, 8), "title"),
                Arguments.of(new UpdateActivityRequestDto("Match", null, null, 2, 8), "eventDate"),
                Arguments.of(new UpdateActivityRequestDto("Match", null, Instant.parse("2020-01-01T00:00:00Z"), 2, 8), "eventDate"),
                Arguments.of(new UpdateActivityRequestDto("Match", null, EVENT_DATE, null, 8), "numberOfRounds"),
                Arguments.of(new UpdateActivityRequestDto("Match", null, EVENT_DATE, 0, 8), "numberOfRounds"),
                Arguments.of(new UpdateActivityRequestDto("Match", null, EVENT_DATE, 11, 8), "numberOfRounds"),
                Arguments.of(new UpdateActivityRequestDto("Match", null, EVENT_DATE, 2, null), "maxPlayers"),
                Arguments.of(new UpdateActivityRequestDto("Match", null, EVENT_DATE, 2, 1), "maxPlayers"));
    }

    @ParameterizedTest(name = "update rejects invalid {1}")
    @MethodSource("invalidUpdates")
    void updateRejectsInvalidBody(UpdateActivityRequestDto update, String field) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/requests/{id}", REQUEST_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details." + field).exists());
        verifyNoInteractions(service);
    }

    @Test
    void updateRejectsMissingBody() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/requests/{id}", REQUEST_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void updateRequiresCallerHeader() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/requests/{id}", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validUpdate())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @MethodSource("updateFailures")
    void updateMapsServiceFailures(RuntimeException failure, int httpStatus, String code) throws Exception {
        when(service.updateRequest(eq(REQUEST_ID), eq(USER_ID), any(UpdateActivityRequestDto.class)))
                .thenThrow(failure);
        mockMvc.perform(MockMvcRequestBuilders.put("/requests/{id}", REQUEST_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(validUpdate())))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(failure.getMessage()));
        verify(service).updateRequest(REQUEST_ID, USER_ID, validUpdate());
    }

    static Stream<Arguments> updateFailures() {
        return Stream.of(
                Arguments.of(new IllegalArgumentException("Invalid capacity"), 400, "BAD_REQUEST"),
                Arguments.of(new ForbiddenOperationException("Not organizer"), 403, "FORBIDDEN"),
                Arguments.of(new ResourceNotFoundException("Request not found"), 404, "NOT_FOUND"),
                Arguments.of(new IllegalStateException("Request not planned"), 409, "CONFLICT"));
    }

    @Test
    void searchUsesDefaultRadiusAndNoOptionalFilters() throws Exception {
        ActivityRequestFeedDto item = feed();
        when(service.searchNearbyRequests(55.7, 37.6, 5000.0, null, null, null))
                .thenReturn(List.of(item));
        mockMvc.perform(MockMvcRequestBuilders.get("/requests")
                        .param("lat", "55.7").param("lon", "37.6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$[0].title").value("Nearby match"))
                .andExpect(jsonPath("$[0].sportName").value("Football"))
                .andExpect(jsonPath("$[0].currentPlayers").value(3))
                .andExpect(jsonPath("$[0].registrationOpen").value(true))
                .andExpect(jsonPath("$[0].latitude").value(55.7));
        verify(service).searchNearbyRequests(55.7, 37.6, 5000.0, null, null, null);
    }

    @Test
    void searchPassesAllFiltersAndCanReturnEmptyFeed() throws Exception {
        Instant start = Instant.parse("2030-01-01T00:00:00Z");
        Instant end = Instant.parse("2030-01-02T00:00:00Z");
        when(service.searchNearbyRequests(-90.0, 180.0, 1000.0, 7L, start, end))
                .thenReturn(List.of());
        mockMvc.perform(MockMvcRequestBuilders.get("/requests")
                        .param("lat", "-90").param("lon", "180")
                        .param("radius", "1000").param("sportId", "7")
                        .param("startDate", start.toString()).param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(service).searchNearbyRequests(-90.0, 180.0, 1000.0, 7L, start, end);
    }

    static Stream<Arguments> invalidSearchParameters() {
        return Stream.of(
                Arguments.of("lat", "-90.1"), Arguments.of("lat", "90.1"),
                Arguments.of("lon", "-180.1"), Arguments.of("lon", "180.1"),
                Arguments.of("radius", "0"), Arguments.of("radius", "-1"));
    }

    @ParameterizedTest(name = "search rejects {0}={1}")
    @MethodSource("invalidSearchParameters")
    void searchRejectsOutOfRangeParameters(String parameter, String value) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/requests")
                .param("lat", parameter.equals("lat") ? value : "55.7")
                .param("lon", parameter.equals("lon") ? value : "37.6");
        if (parameter.equals("radius")) {
            request.param("radius", value);
        }
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isNotEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void searchRequiresCoordinates() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/requests").param("lat", "55.7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void searchRejectsMalformedDateFilter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/requests")
                        .param("lat", "55.7").param("lon", "37.6").param("startDate", "yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void searchMapsInvalidDateRangeFromService() throws Exception {
        when(service.searchNearbyRequests(55.7, 37.6, 5000.0, null, null, null))
                .thenThrow(new IllegalArgumentException("Invalid date range"));
        mockMvc.perform(MockMvcRequestBuilders.get("/requests")
                        .param("lat", "55.7").param("lon", "37.6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid date range"));
        verify(service).searchNearbyRequests(55.7, 37.6, 5000.0, null, null, null);
    }

    @Test
    void detailsReturnsMappedBody() throws Exception {
        when(service.getRequestDetails(REQUEST_ID)).thenReturn(details());
        mockMvc.perform(MockMvcRequestBuilders.get("/requests/{id}", REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.description").value("Outdoor game"))
                .andExpect(jsonPath("$.eventDate").value(EVENT_DATE.toString()))
                .andExpect(jsonPath("$.numberOfRounds").value(3))
                .andExpect(jsonPath("$.registrationOpen").value(true))
                .andExpect(jsonPath("$.participants").isArray());
        verify(service).getRequestDetails(REQUEST_ID);
    }

    @Test
    void detailsMapsMissingRequest() throws Exception {
        when(service.getRequestDetails(REQUEST_ID))
                .thenThrow(new ResourceNotFoundException("Request not found"));
        mockMvc.perform(MockMvcRequestBuilders.get("/requests/{id}", REQUEST_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Request not found"));
        verify(service).getRequestDetails(REQUEST_ID);
    }

    @Test
    void detailsRejectsMalformedRequestId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/requests/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    private static UpdateActivityRequestDto validUpdate() {
        return new UpdateActivityRequestDto("Updated match", "Outdoor game", EVENT_DATE, 3, 8);
    }

    private static CreateActivityRequestDto validCreate() {
        return createDto("Match", null, 3, "Moscow", 55.7, 37.6);
    }

    private static CreateActivityRequestDto createDto(String title, String description, Integer rounds,
                                                       String address, Double latitude, Double longitude) {
        return new CreateActivityRequestDto(title, description, 1L, 8, EVENT_DATE,
                rounds, address, latitude, longitude);
    }

    private static ActivityRequestDetailsDto details() {
        return new ActivityRequestDetailsDto(REQUEST_ID, "Updated match", "Outdoor game", "Football",
                8, 3, EVENT_DATE, "Moscow", 3, 55.7, 37.6, RequestStatus.PLANNED,
                null, true, USER_ID, List.of());
    }

    private static ActivityRequestFeedDto feed() {
        return new ActivityRequestFeedDto(REQUEST_ID, "Nearby match", "Football", 8, 3,
                EVENT_DATE, "Moscow", 3, true, 55.7, 37.6);
    }
}
