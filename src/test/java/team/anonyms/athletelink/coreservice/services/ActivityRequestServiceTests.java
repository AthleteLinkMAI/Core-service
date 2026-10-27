package team.anonyms.athletelink.coreservice.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
import team.anonyms.athletelink.coreservice.dto.ActivityRequestDetailsDto;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestFeedDto;
import team.anonyms.athletelink.coreservice.dto.ParticipantDto;
import team.anonyms.athletelink.coreservice.dto.UpdateActivityRequestDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.entities.Sport;
import team.anonyms.athletelink.coreservice.enums.ParticipantRole;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.coreservice.exceptions.ForbiddenOperationException;
import team.anonyms.athletelink.coreservice.mappers.ActivityRequestMapper;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.coreservice.repositories.SportRepository;

@ExtendWith(MockitoExtension.class)
class ActivityRequestServiceTests {

    private final UUID requestId = UUID.randomUUID();
    private final UUID organizerId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

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
            5.0,
            7.0
        );

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(mockSport.getIsPlayable()).thenReturn(true);
        Mockito.when(mockSport.getMinPlayers()).thenReturn(14);
        Mockito.when(mockSport.getMaxPlayers()).thenReturn(14);

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
        assertEquals(savedRequest.getId(), resultId);
        assertTrue(savedRequest.getRegistrationOpen());

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
    void testCreateRequest_UnderLimitMaxPlayers() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            4,
            Instant.now(),
            3,
            "Moscow",
            0.0,
            0.0
        );

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(mockSport.getIsPlayable()).thenReturn(true);
        Mockito.when(mockSport.getName()).thenReturn("football");
        Mockito.when(mockSport.getMinPlayers()).thenReturn(10);
        Mockito.when(mockSport.getMaxPlayers()).thenReturn(22);

        Mockito.when(sportRepository.findById(dto.sportId()))
            .thenReturn(Optional.of(mockSport));
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> activityRequestService.createRequest(dto, organizerId)
        );

        assertTrue(exception.getMessage().contains("Players count for football must be between 10 and 22"));
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_OverLimitMaxPlayers() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            44,
            Instant.now(),
            3,
            "Moscow",
            0.0,
            0.0
        );

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(mockSport.getIsPlayable()).thenReturn(true);
        Mockito.when(mockSport.getName()).thenReturn("football");
        Mockito.when(mockSport.getMinPlayers()).thenReturn(10);
        Mockito.when(mockSport.getMaxPlayers()).thenReturn(22);

        Mockito.when(sportRepository.findById(dto.sportId()))
            .thenReturn(Optional.of(mockSport));
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> activityRequestService.createRequest(dto, organizerId)
        );
        
        assertTrue(exception.getMessage().contains("Players count for football must be between 10 and 22"));
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }


    @Test
    void testCreateRequest_SportNotFound() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
            "Football",
            "Friendly match",
            1L,
            4,
            Instant.now(),
            3,
            "Moscow",
            0.0,
            0.0
        );


        Mockito.when(sportRepository.findById(dto.sportId()))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> activityRequestService.createRequest(dto, organizerId)
        );
        
        assertTrue(exception.getMessage().contains("Sport not found"));
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void testCreateRequest_SportIsInactive() {
        UUID organizerId = UUID.randomUUID();
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "Football",
                "Friendly match",
                1L,
                4,
                Instant.now(),
                3,
                "Moscow",
                0.0,
                0.0
        );

        Sport mockSport = Mockito.mock(Sport.class);
        Mockito.when(sportRepository.findById(dto.sportId()))
                .thenReturn(Optional.of(mockSport));

        Mockito.when(mockSport.getIsPlayable()).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> activityRequestService.createRequest(dto, organizerId)
        );

        assertTrue(exception.getMessage().contains("New requests cannot be created for this sport."));
        Mockito.verifyNoInteractions(activityRequestRepository);
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void testJoinRequest_Success() {
        UUID organizerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ActivityRequest activityRequest = ActivityRequest.builder()
                .id(requestId)
                .title("Football")
                .description("Friendly match")
                .maxPlayers(4)
                .eventDate(Instant.now())
                .addressText("GUK")
                .numberOfRounds(3)
                .status(RequestStatus.PLANNED)
                .organizerId(organizerId)
                .build();

        Mockito.when(activityRequestRepository.findByIdForUpdate(
                requestId)).thenReturn(Optional.of(activityRequest));

        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(
                requestId, userId)).thenReturn(Optional.empty());
        Mockito.when(requestParticipantRepository.countByActivityRequestIdAndStatus(
                requestId, ParticipantStatus.ACCEPTED)).thenReturn(1L);

        activityRequestService.joinRequest(activityRequest.getId(), userId);

        ArgumentCaptor<RequestParticipant> participantCaptor =
                ArgumentCaptor.forClass(RequestParticipant.class);

        Mockito.verify(requestParticipantRepository).save(participantCaptor.capture());
        RequestParticipant savedParticipant = participantCaptor.getValue();

        assertEquals(userId, savedParticipant.getUserId());
        assertEquals(requestId, savedParticipant.getActivityRequest().getId());
        assertEquals(ParticipantStatus.ACCEPTED, savedParticipant.getStatus());
        assertEquals(ParticipantRole.PLAYER, savedParticipant.getRole());
    }

    @Test
    void testJoinRequest_RequestNotFound() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        Mockito.when(activityRequestRepository.findByIdForUpdate(
                requestId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> activityRequestService.joinRequest(requestId, userId)
        );

        assertTrue(exception.getMessage().contains("Activity request not found"));
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void testJoinRequest_RequestIsStarted() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        ActivityRequest activityRequest = ActivityRequest.builder()
                .id(requestId)
                .status(RequestStatus.ACTIVE)
                .build();

        Mockito.when(activityRequestRepository.findByIdForUpdate(
                requestId)).thenReturn(Optional.of(activityRequest));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> activityRequestService.joinRequest(requestId, userId)
        );

        assertTrue(exception.getMessage().contains("Cannot join this request. Current status is: ACTIVE"));
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void testJoinRequest_RegistrationIsClosed() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        ActivityRequest activityRequest = ActivityRequest.builder()
                .id(requestId)
                .status(RequestStatus.PLANNED)
                .registrationOpen(false)
                .build();

        Mockito.when(activityRequestRepository.findByIdForUpdate(
                requestId)).thenReturn(Optional.of(activityRequest));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> activityRequestService.joinRequest(requestId, userId)
        );

        assertTrue(exception.getMessage().contains("Registration for this request is closed."));
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void testJoinRequest_UserIsAlreadyParticipant() {
        ActivityRequest request = plannedRequest();
        foundForUpdate(request);
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.of(participant(userId, ParticipantRole.PLAYER, ParticipantStatus.ACCEPTED)));

        assertThrows(IllegalStateException.class, () -> activityRequestService.joinRequest(requestId, userId));
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void createRequestAcceptsBothSportPlayerLimits() {
        Sport sport = sport(10, 22);
        Mockito.when(sportRepository.findById(1L)).thenReturn(Optional.of(sport));
        Mockito.when(activityRequestRepository.save(any(ActivityRequest.class))).thenAnswer(invocation -> {
            ActivityRequest request = invocation.getArgument(0);
            request.setId(UUID.randomUUID());
            return request;
        });

        assertNotNull(activityRequestService.createRequest(createDto(10), organizerId));
        assertNotNull(activityRequestService.createRequest(createDto(22), organizerId));

        ArgumentCaptor<ActivityRequest> saved = ArgumentCaptor.forClass(ActivityRequest.class);
        Mockito.verify(activityRequestRepository, Mockito.times(2)).save(saved.capture());
        assertEquals(List.of(10, 22), saved.getAllValues().stream().map(ActivityRequest::getMaxPlayers).toList());
        Mockito.verify(requestParticipantRepository, Mockito.times(2)).save(any(RequestParticipant.class));
    }

    @Test
    void joinRequestRejectsKickedParticipant() {
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.of(participant(userId, ParticipantRole.PLAYER, ParticipantStatus.KICKED)));

        assertThrows(IllegalStateException.class, () -> activityRequestService.joinRequest(requestId, userId));
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void joinRequestRejectsPendingParticipantWithoutCheckingCapacity() {
        RequestParticipant pending = participant(userId, ParticipantRole.PLAYER, ParticipantStatus.PENDING);
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.of(pending));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> activityRequestService.joinRequest(requestId, userId));

        assertEquals("Your request to join is already pending", exception.getMessage());
        assertEquals(ParticipantStatus.PENDING, pending.getStatus());
        Mockito.verify(requestParticipantRepository, Mockito.never())
                .countByActivityRequestIdAndStatus(any(), any());
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void joinRequestReacceptsExistingParticipantWhoLeft() {
        ActivityRequest request = plannedRequest();
        RequestParticipant existing = participant(userId, ParticipantRole.PLAYER, ParticipantStatus.LEFT);
        foundForUpdate(request);
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.of(existing));
        Mockito.when(requestParticipantRepository.countByActivityRequestIdAndStatus(requestId, ParticipantStatus.ACCEPTED))
                .thenReturn(3L);

        activityRequestService.joinRequest(requestId, userId);

        assertEquals(ParticipantStatus.ACCEPTED, existing.getStatus());
        ArgumentCaptor<RequestParticipant> saved = ArgumentCaptor.forClass(RequestParticipant.class);
        Mockito.verify(requestParticipantRepository).save(saved.capture());
        assertSame(existing, saved.getValue());
    }

    @Test
    void joinRequestRejectsNewParticipantWhenFull() {
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.empty());
        Mockito.when(requestParticipantRepository.countByActivityRequestIdAndStatus(requestId, ParticipantStatus.ACCEPTED))
                .thenReturn(4L);

        assertThrows(IllegalStateException.class, () -> activityRequestService.joinRequest(requestId, userId));
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void joinRequestRejectsReturningParticipantWhenFull() {
        RequestParticipant existing = participant(userId, ParticipantRole.PLAYER, ParticipantStatus.LEFT);
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.of(existing));
        Mockito.when(requestParticipantRepository.countByActivityRequestIdAndStatus(requestId, ParticipantStatus.ACCEPTED))
                .thenReturn(4L);

        assertThrows(IllegalStateException.class, () -> activityRequestService.joinRequest(requestId, userId));
        assertEquals(ParticipantStatus.LEFT, existing.getStatus());
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void leaveRequestMarksAcceptedPlayerAsLeft() {
        RequestParticipant player = participant(userId, ParticipantRole.PLAYER, ParticipantStatus.ACCEPTED);
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.of(player));

        activityRequestService.leaveRequest(requestId, userId);

        assertEquals(ParticipantStatus.LEFT, player.getStatus());
        Mockito.verify(requestParticipantRepository).save(player);
    }

    @Test
    void leaveRequestRejectsMissingRequest() {
        Mockito.when(activityRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> activityRequestService.leaveRequest(requestId, userId));
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void leaveRequestRejectsMissingParticipant() {
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> activityRequestService.leaveRequest(requestId, userId));
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void leaveRequestRejectsOrganizer() {
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, organizerId))
                .thenReturn(Optional.of(participant(organizerId, ParticipantRole.ORGANIZER, ParticipantStatus.ACCEPTED)));
        assertThrows(IllegalStateException.class, () -> activityRequestService.leaveRequest(requestId, organizerId));
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void leaveRequestRejectsLeftOrKickedPlayer() {
        foundForUpdate(plannedRequest());
        for (ParticipantStatus status : List.of(ParticipantStatus.LEFT, ParticipantStatus.KICKED)) {
            RequestParticipant player = participant(userId, ParticipantRole.PLAYER, status);
            Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                    .thenReturn(Optional.of(player));
            assertThrows(IllegalStateException.class, () -> activityRequestService.leaveRequest(requestId, userId),
                    "status=" + status);
            assertEquals(status, player.getStatus());
        }
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void leaveRequestRejectsNonPlannedRequest() {
        ActivityRequest request = plannedRequest();
        request.setStatus(RequestStatus.ACTIVE);
        foundForUpdate(request);
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.of(participant(userId, ParticipantRole.PLAYER, ParticipantStatus.ACCEPTED)));
        assertThrows(IllegalStateException.class, () -> activityRequestService.leaveRequest(requestId, userId));
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void cancelRequestMarksPlannedRequestCancelled() {
        ActivityRequest request = plannedRequest();
        foundForUpdate(request);
        activityRequestService.cancelRequest(requestId, organizerId);
        assertEquals(RequestStatus.CANCELLED, request.getStatus());
        Mockito.verify(activityRequestRepository).save(request);
    }

    @Test
    void cancelRequestRejectsMissingRequest() {
        Mockito.when(activityRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> activityRequestService.cancelRequest(requestId, organizerId));
        Mockito.verify(activityRequestRepository, Mockito.never()).save(any());
    }

    @Test
    void cancelRequestRejectsNonOrganizer() {
        foundForUpdate(plannedRequest());
        assertThrows(ForbiddenOperationException.class, () -> activityRequestService.cancelRequest(requestId, userId));
        Mockito.verify(activityRequestRepository, Mockito.never()).save(any());
    }

    @Test
    void cancelRequestRejectsNonPlannedRequest() {
        ActivityRequest request = plannedRequest();
        request.setStatus(RequestStatus.COMPLETED);
        foundForUpdate(request);
        assertThrows(IllegalStateException.class, () -> activityRequestService.cancelRequest(requestId, organizerId));
        assertEquals(RequestStatus.COMPLETED, request.getStatus());
        Mockito.verify(activityRequestRepository, Mockito.never()).save(any());
    }

    @Test
    void kickParticipantMarksPlayerKicked() {
        RequestParticipant player = participant(userId, ParticipantRole.PLAYER, ParticipantStatus.ACCEPTED);
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.of(player));
        activityRequestService.kickParticipant(requestId, userId, organizerId);
        assertEquals(ParticipantStatus.KICKED, player.getStatus());
        Mockito.verify(requestParticipantRepository).save(player);
    }

    @Test
    void kickParticipantRejectsMissingRequest() {
        Mockito.when(activityRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> activityRequestService.kickParticipant(requestId, userId, organizerId));
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void kickParticipantRejectsNonOrganizer() {
        foundForUpdate(plannedRequest());
        assertThrows(ForbiddenOperationException.class,
                () -> activityRequestService.kickParticipant(requestId, userId, UUID.randomUUID()));
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void kickParticipantRejectsOrganizerKickingSelf() {
        foundForUpdate(plannedRequest());
        assertThrows(IllegalArgumentException.class,
                () -> activityRequestService.kickParticipant(requestId, organizerId, organizerId));
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void kickParticipantRejectsMissingTarget() {
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> activityRequestService.kickParticipant(requestId, userId, organizerId));
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void kickParticipantRejectsAlreadyKickedOrLeftTarget() {
        foundForUpdate(plannedRequest());
        for (ParticipantStatus status : List.of(ParticipantStatus.KICKED, ParticipantStatus.LEFT)) {
            RequestParticipant player = participant(userId, ParticipantRole.PLAYER, status);
            Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                    .thenReturn(Optional.of(player));
            assertThrows(IllegalStateException.class,
                    () -> activityRequestService.kickParticipant(requestId, userId, organizerId), "status=" + status);
            assertEquals(status, player.getStatus());
        }
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void kickParticipantRejectsNonPlannedRequest() {
        ActivityRequest request = plannedRequest();
        request.setStatus(RequestStatus.ACTIVE);
        foundForUpdate(request);
        Mockito.when(requestParticipantRepository.findByActivityRequestIdAndUserId(requestId, userId))
                .thenReturn(Optional.of(participant(userId, ParticipantRole.PLAYER, ParticipantStatus.ACCEPTED)));
        assertThrows(IllegalStateException.class,
                () -> activityRequestService.kickParticipant(requestId, userId, organizerId));
        Mockito.verify(requestParticipantRepository, Mockito.never()).save(any());
    }

    @Test
    void updateRequestChangesFieldsAndCountsOnlyAcceptedPlayers() {
        ActivityRequest request = plannedRequest();
        RequestParticipant accepted = participant(organizerId, ParticipantRole.ORGANIZER, ParticipantStatus.ACCEPTED);
        RequestParticipant left = participant(userId, ParticipantRole.PLAYER, ParticipantStatus.LEFT);
        UpdateActivityRequestDto dto = updateDto(3);
        ActivityRequestDetailsDto expected = detailsDto();
        foundForUpdate(request);
        Mockito.when(requestParticipantRepository.findAllByActivityRequestId(requestId))
                .thenReturn(List.of(accepted, left));
        Mockito.when(requestMapper.toDetailsDto(Mockito.eq(request), Mockito.eq(1), Mockito.anyList()))
                .thenReturn(expected);

        ActivityRequestDetailsDto actual = activityRequestService.updateRequest(requestId, organizerId, dto);

        assertSame(expected, actual);
        assertEquals(dto.title(), request.getTitle());
        assertEquals(dto.description(), request.getDescription());
        assertEquals(dto.eventDate(), request.getEventDate());
        assertEquals(dto.numberOfRounds(), request.getNumberOfRounds());
        assertEquals(dto.maxPlayers(), request.getMaxPlayers());
        Mockito.verify(activityRequestRepository).save(request);
        Mockito.verify(requestMapper).toDetailsDto(request, 1, List.of(participantDto(accepted), participantDto(left)));
    }

    @Test
    void updateRequestRejectsMissingRequest() {
        Mockito.when(activityRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> activityRequestService.updateRequest(requestId, organizerId, updateDto(4)));
        Mockito.verifyNoInteractions(requestParticipantRepository, requestMapper);
    }

    @Test
    void updateRequestRejectsNonOrganizer() {
        foundForUpdate(plannedRequest());
        assertThrows(ForbiddenOperationException.class,
                () -> activityRequestService.updateRequest(requestId, userId, updateDto(4)));
        Mockito.verifyNoInteractions(requestParticipantRepository, requestMapper);
    }

    @Test
    void updateRequestRejectsNonPlannedRequest() {
        ActivityRequest request = plannedRequest();
        request.setStatus(RequestStatus.ACTIVE);
        foundForUpdate(request);
        assertThrows(IllegalStateException.class,
                () -> activityRequestService.updateRequest(requestId, organizerId, updateDto(4)));
        Mockito.verifyNoInteractions(requestParticipantRepository, requestMapper);
    }

    @Test
    void updateRequestRejectsLimitBelowAcceptedCount() {
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findAllByActivityRequestId(requestId)).thenReturn(List.of(
                participant(organizerId, ParticipantRole.ORGANIZER, ParticipantStatus.ACCEPTED),
                participant(userId, ParticipantRole.PLAYER, ParticipantStatus.ACCEPTED),
                participant(UUID.randomUUID(), ParticipantRole.PLAYER, ParticipantStatus.ACCEPTED)));
        assertThrows(IllegalStateException.class,
                () -> activityRequestService.updateRequest(requestId, organizerId, updateDto(2)));
        Mockito.verify(activityRequestRepository, Mockito.never()).save(any());
        Mockito.verifyNoInteractions(requestMapper);
    }

    @Test
    void updateRequestRejectsOutsideSportLimits() {
        foundForUpdate(plannedRequest());
        Mockito.when(requestParticipantRepository.findAllByActivityRequestId(requestId)).thenReturn(List.of());
        for (int maxPlayers : List.of(1, 11)) {
            assertThrows(IllegalArgumentException.class,
                    () -> activityRequestService.updateRequest(requestId, organizerId, updateDto(maxPlayers)),
                    "maxPlayers=" + maxPlayers);
        }
        Mockito.verify(activityRequestRepository, Mockito.never()).save(any());
        Mockito.verifyNoInteractions(requestMapper);
    }

    @Test
    void getRequestDetailsIncludesAllParticipantsButCountsAcceptedOnly() {
        ActivityRequest request = plannedRequest();
        RequestParticipant accepted = participant(organizerId, ParticipantRole.ORGANIZER, ParticipantStatus.ACCEPTED);
        RequestParticipant pending = participant(userId, ParticipantRole.PLAYER, ParticipantStatus.PENDING);
        ActivityRequestDetailsDto expected = detailsDto();
        Mockito.when(activityRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        Mockito.when(requestParticipantRepository.findAllByActivityRequestId(requestId))
                .thenReturn(List.of(accepted, pending));
        Mockito.when(requestMapper.toDetailsDto(Mockito.eq(request), Mockito.eq(1), Mockito.anyList()))
                .thenReturn(expected);

        assertSame(expected, activityRequestService.getRequestDetails(requestId));
        Mockito.verify(requestMapper).toDetailsDto(request, 1, List.of(participantDto(accepted), participantDto(pending)));
    }

    @Test
    void getRequestDetailsRejectsMissingRequest() {
        Mockito.when(activityRequestRepository.findById(requestId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> activityRequestService.getRequestDetails(requestId));
        Mockito.verifyNoInteractions(requestParticipantRepository, requestMapper);
    }

    @Test
    void closeRegistrationClosesPlannedRequest() {
        ActivityRequest request = plannedRequest();
        foundForUpdate(request);
        activityRequestService.closeRegistration(requestId, organizerId);
        assertFalse(request.getRegistrationOpen());
    }

    @Test
    void openRegistrationOpensPlannedRequest() {
        ActivityRequest request = plannedRequest();
        request.setRegistrationOpen(false);
        foundForUpdate(request);
        activityRequestService.openRegistration(requestId, organizerId);
        assertTrue(request.getRegistrationOpen());
    }

    @Test
    void closeAndOpenRegistrationRejectMissingRequest() {
        Mockito.when(activityRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> activityRequestService.closeRegistration(requestId, organizerId));
        assertThrows(ResourceNotFoundException.class,
                () -> activityRequestService.openRegistration(requestId, organizerId));
    }

    @Test
    void closeAndOpenRegistrationRejectNonOrganizer() {
        ActivityRequest request = plannedRequest();
        foundForUpdate(request);
        assertThrows(ForbiddenOperationException.class,
                () -> activityRequestService.closeRegistration(requestId, userId));
        assertThrows(ForbiddenOperationException.class,
                () -> activityRequestService.openRegistration(requestId, userId));
        assertTrue(request.getRegistrationOpen());
    }

    @Test
    void closeAndOpenRegistrationRejectNonPlannedRequest() {
        ActivityRequest request = plannedRequest();
        request.setStatus(RequestStatus.ACTIVE);
        foundForUpdate(request);
        assertThrows(IllegalStateException.class,
                () -> activityRequestService.closeRegistration(requestId, organizerId));
        assertThrows(IllegalStateException.class,
                () -> activityRequestService.openRegistration(requestId, organizerId));
        assertTrue(request.getRegistrationOpen());
    }

    @Test
    void startRequestClosesRegistrationAtMinimumAcceptedCount() {
        ActivityRequest request = plannedRequest();
        foundForUpdate(request);
        Mockito.when(requestParticipantRepository.countByActivityRequestIdAndStatus(requestId, ParticipantStatus.ACCEPTED))
                .thenReturn(2L);
        activityRequestService.startRequest(requestId, organizerId);
        assertEquals(RequestStatus.ACTIVE, request.getStatus());
        assertFalse(request.getRegistrationOpen());
    }

    @Test
    void startRequestRejectsMissingRequest() {
        Mockito.when(activityRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> activityRequestService.startRequest(requestId, organizerId));
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void startRequestRejectsNonOrganizer() {
        foundForUpdate(plannedRequest());
        assertThrows(ForbiddenOperationException.class, () -> activityRequestService.startRequest(requestId, userId));
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void startRequestRejectsNonPlannedRequest() {
        ActivityRequest request = plannedRequest();
        request.setStatus(RequestStatus.ACTIVE);
        foundForUpdate(request);
        assertThrows(IllegalStateException.class, () -> activityRequestService.startRequest(requestId, organizerId));
        Mockito.verifyNoInteractions(requestParticipantRepository);
    }

    @Test
    void startRequestRejectsTooFewAcceptedPlayers() {
        ActivityRequest request = plannedRequest();
        foundForUpdate(request);
        Mockito.when(requestParticipantRepository.countByActivityRequestIdAndStatus(requestId, ParticipantStatus.ACCEPTED))
                .thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> activityRequestService.startRequest(requestId, organizerId));
        assertEquals(RequestStatus.PLANNED, request.getStatus());
        assertTrue(request.getRegistrationOpen());
    }

    @Test
    void searchNearbyRequestsRejectsInvertedDateRangeBeforeRepositoryCall() {
        Instant later = Instant.parse("2030-02-01T00:00:00Z");
        Instant earlier = Instant.parse("2030-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> activityRequestService.searchNearbyRequests(55.7, 37.6, 1000, 1L, later, earlier));
        Mockito.verifyNoInteractions(activityRequestRepository, requestParticipantRepository, requestMapper);
    }

    @Test
    void searchNearbyRequestsMapsEveryResultWithItsAcceptedCount() {
        ActivityRequest first = plannedRequest();
        UUID secondId = UUID.randomUUID();
        ActivityRequest second = ActivityRequest.builder().id(secondId).build();
        Instant start = Instant.parse("2030-01-01T00:00:00Z");
        Instant end = Instant.parse("2030-02-01T00:00:00Z");
        ActivityRequestFeedDto firstDto = feedDto(requestId, 2);
        ActivityRequestFeedDto secondDto = feedDto(secondId, 0);
        Mockito.when(activityRequestRepository.findNearbyRequests(55.7, 37.6, 1000, 1L, start, end))
                .thenReturn(List.of(first, second));
        Mockito.when(requestParticipantRepository.countByActivityRequestIdAndStatus(requestId, ParticipantStatus.ACCEPTED))
                .thenReturn(2L);
        Mockito.when(requestParticipantRepository.countByActivityRequestIdAndStatus(secondId, ParticipantStatus.ACCEPTED))
                .thenReturn(0L);
        Mockito.when(requestMapper.toFeedDto(first, 2)).thenReturn(firstDto);
        Mockito.when(requestMapper.toFeedDto(second, 0)).thenReturn(secondDto);

        assertEquals(List.of(firstDto, secondDto),
                activityRequestService.searchNearbyRequests(55.7, 37.6, 1000, 1L, start, end));
        Mockito.verify(requestMapper).toFeedDto(first, 2);
        Mockito.verify(requestMapper).toFeedDto(second, 0);
    }

    @Test
    void searchNearbyRequestsAllowsOpenEndedDatesAndEmptyResults() {
        Instant start = Instant.parse("2030-01-01T00:00:00Z");
        Instant end = Instant.parse("2030-02-01T00:00:00Z");
        Mockito.when(activityRequestRepository.findNearbyRequests(55.7, 37.6, 1000, null, start, null))
                .thenReturn(List.of());
        Mockito.when(activityRequestRepository.findNearbyRequests(55.7, 37.6, 1000, null, null, end))
                .thenReturn(List.of());
        assertTrue(activityRequestService.searchNearbyRequests(55.7, 37.6, 1000, null, start, null).isEmpty());
        assertTrue(activityRequestService.searchNearbyRequests(55.7, 37.6, 1000, null, null, end).isEmpty());
        Mockito.verifyNoInteractions(requestParticipantRepository, requestMapper);
    }

    private ActivityRequestFeedDto feedDto(UUID id, int currentPlayers) {
        return new ActivityRequestFeedDto(id, "Mapped", "Football", 4, currentPlayers,
                Instant.parse("2030-01-01T12:00:00Z"), "Moscow", 3, true, 55.7, 37.6);
    }

    private ParticipantDto participantDto(RequestParticipant participant) {
        return new ParticipantDto(participant.getUserId(), participant.getRole(),
                participant.getStatus(), participant.getJoinedAt());
    }

    private ActivityRequestDetailsDto detailsDto() {
        return new ActivityRequestDetailsDto(requestId, "Mapped", null, "Football", 4, 1,
                Instant.parse("2030-01-01T12:00:00Z"), "Moscow", 3, 55.7, 37.6,
                RequestStatus.PLANNED, null, true, organizerId, List.of());
    }

    private void foundForUpdate(ActivityRequest request) {
        Mockito.when(activityRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
    }

    private Sport sport(int minPlayers, int maxPlayers) {
        return Sport.builder().id(1L).name("Football").minPlayers(minPlayers)
                .maxPlayers(maxPlayers).isPlayable(true).build();
    }

    private ActivityRequest plannedRequest() {
        return ActivityRequest.builder().id(requestId).organizerId(organizerId)
                .sport(sport(2, 10)).status(RequestStatus.PLANNED)
                .registrationOpen(true).maxPlayers(4).numberOfRounds(3).build();
    }

    private RequestParticipant participant(UUID id, ParticipantRole role, ParticipantStatus status) {
        return RequestParticipant.builder().activityRequest(plannedRequest()).userId(id)
                .role(role).status(status).joinedAt(Instant.parse("2026-01-01T00:00:00Z")).build();
    }

    private CreateActivityRequestDto createDto(int maxPlayers) {
        return new CreateActivityRequestDto("Football", "Friendly match", 1L, maxPlayers,
                Instant.parse("2030-01-01T12:00:00Z"), 3, "Moscow", 55.7, 37.6);
    }

    private UpdateActivityRequestDto updateDto(int maxPlayers) {
        return new UpdateActivityRequestDto("New title", "New description",
                Instant.parse("2030-02-01T12:00:00Z"), 5, maxPlayers);
    }
}
