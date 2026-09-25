package team.anonyms.athletelink.coreservice.controllers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import team.anonyms.athletelink.coreservice.dto.CreateActivityRequestDto;
import team.anonyms.athletelink.coreservice.services.ActivityRequestService;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(ActivityRequestController.class)
public class ActivityRequestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ActivityRequestService activityRequestService;

    @Test
    void testCreateRequest_Success() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now().plus(1, ChronoUnit.DAYS),
            3,
            "Moscow",
            0.0,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.requestId").value(createdId.toString()));
        
        verify(activityRequestService)
                .createRequest(any(CreateActivityRequestDto.class), eq(userId));
    }


    @Test
    void testCreateRequest_NullUserId() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now().plus(1, ChronoUnit.DAYS),
            3,
            "Moscow",
            0.0,
            0.0
        );
        UUID userId = null;

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(null);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }


    @Test
    void testCreateRequest_NullDto() throws Exception {
        CreateActivityRequestDto dto = null;
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }


    @Test
    void testCreateRequest_NullDtoTitle() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            null,
            "Friendly match",
            1L,
            14,
            Instant.now().plus(1, ChronoUnit.DAYS),
            3,
            "Moscow",
            0.0,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }


    @Test
    void testCreateRequest_NullDtoDescription() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            null,
            1L,
            14,
            Instant.now().plus(1, ChronoUnit.DAYS),
            3,
            "Moscow",
            0.0,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.requestId").value(createdId.toString()));
        
        verify(activityRequestService)
                .createRequest(any(CreateActivityRequestDto.class), eq(userId));
    }


    @Test
    void testCreateRequest_NullDtoSportId() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            null,
            14,
            Instant.now().plus(1, ChronoUnit.DAYS),
            3,
            "Moscow",
            0.0,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }


    @Test
    void testCreateRequest_NullDtoMaxPlayers() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            null,
            Instant.now().plus(1, ChronoUnit.DAYS),
            3,
            "Moscow",
            0.0,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }


    @Test
    void testCreateRequest_NullDtoEventDate() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            null,
            3,
            "Moscow",
            0.0,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }
    
    
    @Test
    void testCreateRequest_NullDtoNumberOfRounds() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now().plus(1, ChronoUnit.DAYS),
            null,
            "Moscow",
            0.0,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }
    
    
    @Test
    void testCreateRequest_NullDtoAddressText() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now().plus(1, ChronoUnit.DAYS),
            3,
            null,
            0.0,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }
    
    
    @Test
    void testCreateRequest_NullDtoLatitude() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now().plus(1, ChronoUnit.DAYS),
            3,
            "Moscow",
            null,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }
    
    
    @Test
    void testCreateRequest_NullDtoLongitude() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now().plus(1, ChronoUnit.DAYS),
            3,
            "Moscow",
            0.0,
            null
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }
    
    
    @Test
    void testCreateRequest_DtoPastEventDate() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now().minus(1, ChronoUnit.DAYS),
            3,
            "Moscow",
            0.0,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId)).thenReturn(createdId);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(activityRequestService);
    }
    
    
    @Test
    void testCreateRequest_CreateRequestOfActivityRequestServiceReturnIllegalArgumentException() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now().plus(1, ChronoUnit.DAYS),
            3,
            "Moscow",
            0.0,
            0.0
        );
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();

        Mockito.when(activityRequestService.createRequest(dto, userId))
                .thenThrow(new IllegalArgumentException("Sport not found"));
        
        mockMvc.perform(MockMvcRequestBuilders.post("/requests")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        verify(activityRequestService)
                .createRequest(any(CreateActivityRequestDto.class), eq(userId));
    }

}


