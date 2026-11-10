package team.anonyms.athletelink.requestservice.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team.anonyms.athletelink.requestservice.dto.PendingJoinRequestDto;
import team.anonyms.athletelink.requestservice.exceptions.ForbiddenOperationException;
import team.anonyms.athletelink.requestservice.services.ActivityRequestService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JoinRequestsController.class)
class JoinRequestsControllerEndpointsTests {
    private static final UUID REQUEST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORGANIZER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PARTICIPANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PLAYER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired MockMvc mockMvc;
    @MockitoBean ActivityRequestService service;

    @Test
    void organizerCanListPendingApplications() throws Exception {
        when(service.getPendingJoinRequests(REQUEST_ID, ORGANIZER_ID))
                .thenReturn(List.of(new PendingJoinRequestDto(PARTICIPANT_ID, REQUEST_ID, PLAYER_ID)));

        mockMvc.perform(get("/requests/{requestId}/join-requests", REQUEST_ID)
                        .header("X-User-Id", ORGANIZER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participantId").value(PARTICIPANT_ID.toString()))
                .andExpect(jsonPath("$[0].userId").value(PLAYER_ID.toString()));
    }

    @Test
    void organizerDecisionIsAppliedSynchronously() throws Exception {
        mockMvc.perform(post("/requests/{requestId}/join-requests/{participantId}/decision", REQUEST_ID, PARTICIPANT_ID)
                        .header("X-User-Id", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":false}"))
                .andExpect(status().isNoContent());

        verify(service).decideJoinRequest(REQUEST_ID, PARTICIPANT_ID, ORGANIZER_ID, false);
    }

    @Test
    void missingDecisionIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/requests/{requestId}/join-requests/{participantId}/decision", REQUEST_ID, PARTICIPANT_ID)
                        .header("X-User-Id", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.accepted").exists());

        verifyNoInteractions(service);
    }

    @Test
    void nonOrganizerGetsForbidden() throws Exception {
        doThrow(new ForbiddenOperationException("Only the organizer may do this"))
                .when(service).decideJoinRequest(REQUEST_ID, PARTICIPANT_ID, PLAYER_ID, true);

        mockMvc.perform(post("/requests/{requestId}/join-requests/{participantId}/decision", REQUEST_ID, PARTICIPANT_ID)
                        .header("X-User-Id", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":true}"))
                .andExpect(status().isForbidden());
    }
}
