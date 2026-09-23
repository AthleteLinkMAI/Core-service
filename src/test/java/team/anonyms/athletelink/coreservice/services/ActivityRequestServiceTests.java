package team.anonyms.athletelink.coreservice.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import team.anonyms.athletelink.coreservice.dto.CreateActivityRequestDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.entities.Sport;
import team.anonyms.athletelink.coreservice.enums.ParticipantRole;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.mappers.ActivityRequestMapper;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.coreservice.repositories.SportRepository;

@ExtendWith(MockitoExtension.class)
class ActivityRequestServiceTests {

    @Mock
    private ActivityRequestRepository activityRequestRepository;

    @Mock
    private RequestParticipantRepository requestParticipantRepository;

    @Mock
    private SportRepository sportRepository;

    @Mock
    private ActivityRequestMapper requestMapper;

    @InjectMocks
    private ActivityRequestService activityRequestService;


    @Test
    void testCreateRequest_Success() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now(),
            3,
            "Moscow",
            0.0,
            0.0
        );

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(mockSport.getMinPlayers()).thenReturn(10);
        Mockito.when(mockSport.getMaxPlayers()).thenReturn(22);

        Mockito.when(sportRepository.findById(dto.sportId()))
            .thenReturn(Optional.of(mockSport));

        Mockito.when(activityRequestRepository.save(any(ActivityRequest.class)))
            .thenAnswer(invocation -> {
                ActivityRequest request = invocation.getArgument(0);
                request.setId(UUID.randomUUID());
                return request;
            });

        UUID resultId = activityRequestService.createRequest(dto, organizerId);

        assertNotNull(resultId);

        ArgumentCaptor<ActivityRequest> requestCaptor =
            ArgumentCaptor.forClass(ActivityRequest.class);
        Mockito.verify(activityRequestRepository).save(requestCaptor.capture());
        ActivityRequest savedRequest = requestCaptor.getValue();

        assertEquals(dto.title(), savedRequest.getTitle());
        assertEquals(dto.description(), savedRequest.getDescription());
        assertEquals(mockSport, savedRequest.getSport());
        assertEquals(dto.maxPlayers(), savedRequest.getMaxPlayers());
        assertEquals(dto.eventDate(), savedRequest.getEventDate());
        assertEquals(dto.addressText(), savedRequest.getAddressText());
        assertEquals(dto.numberOfRounds(), savedRequest.getNumberOfRounds());
        assertEquals(RequestStatus.PLANNED, savedRequest.getStatus());
        assertEquals(organizerId, savedRequest.getOrganizerId());

        Point location = savedRequest.getLocationPoint();
        assertNotNull(location);
        assertEquals(dto.longitude(), location.getX());
        assertEquals(dto.latitude(), location.getY());
        assertEquals(4326, location.getSRID());

        ArgumentCaptor<RequestParticipant> participantCaptor =
            ArgumentCaptor.forClass(RequestParticipant.class);
        Mockito.verify(requestParticipantRepository).save(participantCaptor.capture());
        RequestParticipant savedParticipant = participantCaptor.getValue();

        assertEquals(savedRequest, savedParticipant.getActivityRequest());
        assertEquals(organizerId, savedParticipant.getUserId());
        assertEquals(ParticipantRole.ORGANIZER, savedParticipant.getRole());
        assertEquals(ParticipantStatus.ACCEPTED, savedParticipant.getStatus());
    }


    @Test
    void testCreateRequest_NullDto() {
        UUID organizerId = UUID.randomUUID();

        assertThrows(NullPointerException.class,
            () -> activityRequestService.createRequest(null, organizerId)
        );

        Mockito.verifyNoInteractions(sportRepository);
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_NullOrganizedId() {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now(),
            3,
            "Moscow",
            0.0,
            0.0
        );

        assertThrows(IllegalArgumentException.class,
            () -> activityRequestService.createRequest(dto, null)
        );

        Mockito.verifyNoInteractions(sportRepository);
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_NullTitle() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            null,
            "Friendly match",
            1L,
            14,
            Instant.now(),
            3,
            "Moscow",
            0.0,
            0.0
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> activityRequestService.createRequest(dto, organizerId)
        );

        assertTrue(exception.getMessage().contains("Title not found"));
        Mockito.verifyNoInteractions(sportRepository);
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_NullSportId() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            null,
            14,
            Instant.now(),
            3,
            "Moscow",
            0.0,
            0.0
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> activityRequestService.createRequest(dto, organizerId)
        );

        assertTrue(exception.getMessage().contains("Sport not found"));
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_NullMaxPlayers() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            null,
            Instant.now(),
            3,
            "Moscow",
            0.0,
            0.0
        );

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(mockSport.getMinPlayers()).thenReturn(10);
        Mockito.when(mockSport.getMaxPlayers()).thenReturn(22);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> activityRequestService.createRequest(dto, organizerId)
        );

        assertTrue(exception.getMessage().contains("Players count for"));
        assertTrue(exception.getMessage().contains("must be between"));
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_NullEventDate() {
        UUID organizerId = UUID.randomUUID();
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

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(mockSport.getMinPlayers()).thenReturn(10);
        Mockito.when(mockSport.getMaxPlayers()).thenReturn(22);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> activityRequestService.createRequest(dto, organizerId)
        );

        Mockito.verifyNoInteractions(sportRepository);
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_NullNumberOfRounds() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now(),
            null,
            "Moscow",
            0.0,
            0.0
        );

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(mockSport.getMinPlayers()).thenReturn(10);
        Mockito.when(mockSport.getMaxPlayers()).thenReturn(22);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> activityRequestService.createRequest(dto, organizerId)
        );
        
        Mockito.verifyNoInteractions(sportRepository);
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_NullAddressText() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now(),
            3,
            null,
            0.0,
            0.0
        );

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(mockSport.getMinPlayers()).thenReturn(10);
        Mockito.when(mockSport.getMaxPlayers()).thenReturn(22);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> activityRequestService.createRequest(dto, organizerId)
        );
        
        Mockito.verifyNoInteractions(sportRepository);
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_NullLatitude() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now(),
            3,
            "Moscow",
            null,
            0.0
        );

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(mockSport.getMinPlayers()).thenReturn(10);
        Mockito.when(mockSport.getMaxPlayers()).thenReturn(22);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> activityRequestService.createRequest(dto, organizerId)
        );
        
        Mockito.verifyNoInteractions(sportRepository);
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_NullLongitude() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            14,
            Instant.now(),
            3,
            "Moscow",
            0.0,
            null
        );

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(mockSport.getMinPlayers()).thenReturn(10);
        Mockito.when(mockSport.getMaxPlayers()).thenReturn(22);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> activityRequestService.createRequest(dto, organizerId)
        );
        
        Mockito.verifyNoInteractions(sportRepository);
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }
}