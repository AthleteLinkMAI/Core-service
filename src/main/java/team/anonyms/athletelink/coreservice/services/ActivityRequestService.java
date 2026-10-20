package team.anonyms.athletelink.coreservice.services;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import team.anonyms.athletelink.coreservice.dto.*;

import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.entities.Sport;
import team.anonyms.athletelink.coreservice.enums.ParticipantRole;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.exceptions.ForbiddenOperationException;
import team.anonyms.athletelink.coreservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.coreservice.mappers.ActivityRequestMapper;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.coreservice.repositories.SportRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.time.Instant;

/**
 * Service class for managing activity requests, including creating requests, 
 * joining/leaving requests, and retrieving request details.
 */
@Service 
public class ActivityRequestService {
    private final ActivityRequestRepository activityRequestRepository;
    private final RequestParticipantRepository requestParticipantRepository;
    private final SportRepository sportRepository;

    private final ActivityRequestMapper requestMapper;
    private static final int REQUESTS_PAGE_SIZE = 20;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public ActivityRequestService(
        ActivityRequestRepository activityRequestRepository, 
        RequestParticipantRepository requestParticipantRepository, 
        SportRepository sportRepository,
        ActivityRequestMapper requestMapper
    ) {
        this.activityRequestRepository = activityRequestRepository;
        this.requestParticipantRepository = requestParticipantRepository;
        this.sportRepository = sportRepository;
        this.requestMapper = requestMapper;
    }

    /**
     * Handles the logic of creating a new activity request and automatically adding the organizer as the first participant.
     * Business rules:
     * - Cannot create a request if the sport ID isn't in the list of available sports.
     * - Cannot create a request if the player limit (maxPlayers) is not within the sport's allowed range.
     *
     * @param dto Data transfer object containing the details of the request to be created
     * @param organizerId ID of the organizer as a string (from the token)
     */
    @Transactional
    public UUID createRequest(CreateActivityRequestDto dto, UUID organizerId) {

        Sport sport = sportRepository.findById(dto.sportId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport not found"));

        if (Boolean.FALSE.equals(sport.getIsPlayable())) {
            throw new IllegalStateException("New requests cannot be created for this sport.");
        }

        if (dto.maxPlayers() < sport.getMinPlayers() || dto.maxPlayers() > sport.getMaxPlayers()) {
            throw new IllegalArgumentException(String.format(
                    "Players count for %s must be between %d and %d", 
                    sport.getName(), sport.getMinPlayers(), sport.getMaxPlayers()
            ));
        }

        Coordinate coordinate = new Coordinate(dto.longitude(), dto.latitude());
        Point locationPoint = geometryFactory.createPoint(coordinate);

        ActivityRequest request = ActivityRequest.builder()
                .title(dto.title())
                .description(dto.description())
                .sport(sport)
                .maxPlayers(dto.maxPlayers())
                .eventDate(dto.eventDate())
                .addressText(dto.addressText())
                .numberOfRounds(dto.numberOfRounds())
                .locationPoint(locationPoint)
                .status(RequestStatus.PLANNED)
                .organizerId(organizerId)
                .build();

        activityRequestRepository.save(request);

        RequestParticipant participant = RequestParticipant.builder()
                .activityRequest(request)
                .userId(organizerId)
                .role(ParticipantRole.ORGANIZER)
                .status(ParticipantStatus.ACCEPTED)
                .build();

        requestParticipantRepository.save(participant);

        return request.getId();
    }


    /**
     * Handles the logic of joining an activity request.
     * Business rules:
     * - Cannot join if the request status is not PLANNED.
     * - Cannot join if the player limit (maxPlayers) has been reached.
     * - If the user previously left (LEFT), they can rejoin if there are available spots.
     * - If the user was kicked by the organizer (KICKED), rejoining is forbidden.
     *
     * @param requestId ID of the target request
     * @param userId ID of the user as a string (from the token)
     * @throws IllegalArgumentException if the request is not found, no spots available, or the user is blocked in this request
     */
    @Transactional
    public void joinRequest(UUID requestId, UUID userId) {
        
        ActivityRequest request = activityRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity request not found"));

        if (request.getStatus() != RequestStatus.PLANNED) {
            throw new IllegalStateException("Cannot join this request. Current status is: " + request.getStatus());
        }

        if (!request.getRegistrationOpen()) {
            throw new IllegalStateException(
                    "Registration for this request is closed."
            );
        }

        Optional<RequestParticipant> existingParticipantOpt = requestParticipantRepository
                .findByActivityRequestIdAndUserId(requestId, userId);

        if (existingParticipantOpt.isPresent()) {
            RequestParticipant existing = existingParticipantOpt.get();
            
            if (existing.getStatus() == ParticipantStatus.ACCEPTED) {
                throw new IllegalStateException("You are already a participant");
            }
            if (existing.getStatus() == ParticipantStatus.KICKED) {
                throw new IllegalStateException("You were kicked from this request and cannot rejoin");
            }
            if (existing.getStatus() == ParticipantStatus.PENDING) {
                throw new IllegalStateException("Your request to join is already pending");
            }
            
            if (existing.getStatus() == ParticipantStatus.LEFT) {
                checkCapacity(request);
                existing.setStatus(ParticipantStatus.ACCEPTED);
                requestParticipantRepository.save(existing);
                return;
            }
        }

        checkCapacity(request);

        RequestParticipant newParticipant = RequestParticipant.builder()
                .activityRequest(request)
                .userId(userId)
                .role(ParticipantRole.PLAYER)
                .status(ParticipantStatus.ACCEPTED)
                .build();

        requestParticipantRepository.save(newParticipant);
    }

    /**
     * Handles the logic of leaving an activity request.
     * Business rules:
     * - Cannot kick the organizer from the request. The organizer must cancel the request entirely.
     * - Cannot kick the user who is not a participant of the request.
     * - Cannot kick the user who has already left the request.
     *
     * @param requestId ID of the target request
     * @param userId ID of the user as a string (from the token)
     * @throws IllegalArgumentException if the request is not found, the user is not a participant, the user is the organizer,
     * already left this request or was kicked from it.
     */
    @Transactional
    public void leaveRequest(UUID requestId, UUID userId) {

        ActivityRequest request = activityRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity request not found."));

        RequestParticipant participant = requestParticipantRepository
                .findByActivityRequestIdAndUserId(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a participant of this request"));

        if (participant.getRole() == ParticipantRole.ORGANIZER) {
            throw new IllegalStateException("Organizer cannot leave the request. You must cancel the request entirely.");
        }

        if (participant.getStatus() == ParticipantStatus.LEFT) {
            throw new IllegalStateException("You have already left this request.");
        }

        if (participant.getStatus() == ParticipantStatus.KICKED) {
            throw new IllegalStateException("You were kicked from this request and cannot leave it again.");
        }

        if (request.getStatus() != RequestStatus.PLANNED) {
            throw new IllegalStateException("Cannot leave this request. Current status is: " + request.getStatus());
        }

        participant.setStatus(ParticipantStatus.LEFT);
        requestParticipantRepository.save(participant);
    }

    /**
     * Cancels the activity request.
     * Business rules:
     * - Only the creator (organizer) of the request can cancel it.
     * - Cannot cancel a request that is already COMPLETED or CANCELLED.
     *
     * @param requestId ID of the request to cancel
     * @param userId ID of the user attempting the cancellation
     */
    @Transactional
    public void cancelRequest(UUID requestId, UUID userId) {

        ActivityRequest request = activityRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity request not found"));

        validateOrganizer(request, userId);

        if (request.getStatus() != RequestStatus.PLANNED) {
            throw new IllegalStateException(
                    "Only a planned request can be cancelled."
            );
        }

        request.setStatus(RequestStatus.CANCELLED);
        activityRequestRepository.save(request);
    }

    /**
     * Kicks a participant from the activity request.
     * Business rules:
     * - Only the creator (organizer) can kick participants.
     * - The organizer cannot kick themselves.
     * - The target user must be an existing participant of this request.
     *
     * @param requestId ID of the request
     * @param targetUserId ID of the user to be kicked
     * @param organizerId ID of the user attempting the kick (must be organizer)
     */
    @Transactional
    public void kickParticipant(UUID requestId, UUID targetUserId, UUID organizerId) {

        ActivityRequest request = activityRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity request not found."));

        validateOrganizer(request, organizerId);

        if (organizerId.equals(targetUserId)) {
            throw new IllegalArgumentException("Organizer cannot kick themselves.");
        }

        RequestParticipant targetParticipant = requestParticipantRepository
                .findByActivityRequestIdAndUserId(requestId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user is not a participant of this request."));

        if (targetParticipant.getStatus() == ParticipantStatus.KICKED) {
            throw new IllegalStateException("User is already kicked.");
        }

        if (targetParticipant.getStatus() == ParticipantStatus.LEFT) {
            throw new IllegalStateException("User has already left the request.");
        }

        if (request.getStatus() != RequestStatus.PLANNED) {
            throw new IllegalStateException("Cannot kick participants from a request that is not in PLANNED status.");
        }

        targetParticipant.setStatus(ParticipantStatus.KICKED);
        requestParticipantRepository.save(targetParticipant);
    }

    /**
     * Updates an existing activity request.
     * Business rules:
     * - Only the organizer can edit the request.
     * - Cannot edit if the request is not in PLANNED status.
     * - New maxPlayers limit cannot be less than the current number of accepted participants.
     */
    @Transactional
    public ActivityRequestDetailsDto updateRequest(UUID requestId, UUID organizerId, UpdateActivityRequestDto dto) {
        ActivityRequest request = activityRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity request not found"));

        validateOrganizer(request, organizerId);

        if (request.getStatus() != RequestStatus.PLANNED) {
            throw new IllegalStateException("Can only edit requests in PLANNED status");
        }

        List<RequestParticipant> participants = requestParticipantRepository.findAllByActivityRequestId(requestId);
        
        int currentPlayers = (int) participants.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                .count();

        if (dto.maxPlayers() < currentPlayers) {
            throw new IllegalStateException("Cannot set maxPlayers below the current number of joined players (" + currentPlayers + ")");
        }
        if (dto.maxPlayers() < request.getSport().getMinPlayers() || dto.maxPlayers() > request.getSport().getMaxPlayers()) {
            throw new IllegalArgumentException(String.format(
                    "Players count for %s must be between %d and %d",
                    request.getSport().getName(),
                    request.getSport().getMinPlayers(),
                    request.getSport().getMaxPlayers()
            ));
        }

        request.setTitle(dto.title());
        request.setDescription(dto.description());
        request.setEventDate(dto.eventDate());
        request.setNumberOfRounds(dto.numberOfRounds());
        request.setMaxPlayers(dto.maxPlayers());

        activityRequestRepository.save(request);

        List<ParticipantDto> participantDtos = participants.stream()
                .map(p -> new ParticipantDto(p.getUserId(), p.getRole(), p.getStatus(), p.getJoinedAt()))
                .toList();

        return requestMapper.toDetailsDto(request, currentPlayers, participantDtos);
    }

    /**
     * Gets the details of a specific activity request, including its participants and their statuses.
     *
     * @param requestId ID of the target request
     * @throws IllegalArgumentException if the request is not found.
     */
    @Transactional(readOnly = true)
    public ActivityRequestDetailsDto getRequestDetails(UUID requestId) {
        ActivityRequest request = activityRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity request not found"));

        List<RequestParticipant> participants = requestParticipantRepository
                .findAllByActivityRequestId(requestId);

        List<ParticipantDto> participantDtos = participants.stream()
                .map(p -> new ParticipantDto(
                        p.getUserId(),
                        p.getRole(),
                        p.getStatus(),
                        p.getJoinedAt()
                ))
                .toList();

        int currentPlayers = (int) participants.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                .count();

        return requestMapper.toDetailsDto(request, currentPlayers, participantDtos);
    }

    @Transactional
    public void closeRegistration(UUID requestId, UUID organizerId) {
        ActivityRequest request = activityRequestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Activity request not found"));

        validateOrganizer(request, organizerId);

        if (request.getStatus() != RequestStatus.PLANNED) {
            throw new IllegalStateException(
                    "Registration can only be changed for a planned request."
            );
        }

        request.setRegistrationOpen(false);
    }

    @Transactional
    public void openRegistration(UUID requestId, UUID organizerId) {
        ActivityRequest request = activityRequestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Activity request not found"));

        validateOrganizer(request, organizerId);

        if (request.getStatus() != RequestStatus.PLANNED) {
            throw new IllegalStateException(
                    "Registration can only be changed for a planned request."
            );
        }

        request.setRegistrationOpen(true);
    }

    private void validateOrganizer(
            ActivityRequest request,
            UUID userId
    ) {
        if (!request.getOrganizerId().equals(userId)) {
            throw new ForbiddenOperationException(
                    "Only the organizer can perform this action."
            );
        }
    }

    @Transactional
    public void startRequest(UUID requestId, UUID organizerId) {
        ActivityRequest request = activityRequestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Activity request not found"));

        validateOrganizer(request, organizerId);

        if (request.getStatus() != RequestStatus.PLANNED) {
            throw new IllegalStateException(
                    "Only a planned request can be started."
            );
        }

        long acceptedPlayers = requestParticipantRepository
                .countByActivityRequestIdAndStatus(
                        requestId,
                        ParticipantStatus.ACCEPTED
                );

        if (acceptedPlayers < request.getSport().getMinPlayers()) {
            throw new IllegalStateException(
                    "Not enough players to start this request."
            );
        }

        request.setRegistrationOpen(false);
        request.setStatus(RequestStatus.ACTIVE);
    }

    /**
     * Searches for nearby activity requests based on the provided latitude, longitude, radius, and optional sport ID.
     * The search filters requests that are planned, have an event date in the future, and
     * are within the specified radius from the given coordinates.
     * If a sport ID is provided, it filters requests for that specific sport.
     */
    @Transactional(readOnly = true)
    public List<ActivityRequestFeedDto> searchNearbyRequests(
            double latitude, double longitude, double radiusInMeters, Long sportId,
        Instant startDate, Instant endDate) {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        List<ActivityRequest> nearbyRequests = activityRequestRepository
                .findNearbyRequests(latitude, longitude, radiusInMeters, sportId, startDate, endDate);

        return nearbyRequests.stream().map(request -> {
            
            int currentPlayers = (int) requestParticipantRepository
                    .countByActivityRequestIdAndStatus(request.getId(), ParticipantStatus.ACCEPTED);
            
            return requestMapper.toFeedDto(request, currentPlayers);
            
        }).toList();
    }

    /**
     * Returns a page of requests associated with the specified user, including inactive requests.
     * Without a status filter, every request status is included. Organizer-only mode excludes
     * requests where the user is solely a participant.
     *
     * @param userId user whose requests are queried
     * @param filterStatuses request statuses to include, or {@code null} for all statuses
     * @param pageNumber zero-based page number
     * @param forOrganizer whether to return only requests organized by the user
     * @return a page of request summaries, possibly empty
     */
    @Transactional(readOnly = true)
    public Page<ActivityRequestInformationDto> getRequestsByUserId(
            UUID userId,
            Set<RequestStatus> filterStatuses,
            Integer pageNumber,
            Boolean forOrganizer
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (pageNumber == null || pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must be >= 0");
        }

        Pageable pageable = PageRequest.of(pageNumber, REQUESTS_PAGE_SIZE);

        if (filterStatuses != null && filterStatuses.isEmpty()) {
            throw new IllegalArgumentException("statuses must be omitted or non-empty");
        }

        Set<RequestStatus> statuses = filterStatuses == null
                ? EnumSet.allOf(RequestStatus.class)
                : EnumSet.copyOf(filterStatuses);

        Page<ActivityRequest> requests = Boolean.TRUE.equals(forOrganizer)
                ? activityRequestRepository.findAllCreatedByUser(userId, statuses, pageable)
                : activityRequestRepository.findAllForUser(userId, statuses, pageable);

        return requests.map(request -> {
            int currentPlayers = Math.toIntExact(
                    requestParticipantRepository.countByActivityRequestIdAndStatus(
                            request.getId(), ParticipantStatus.ACCEPTED
                    )
            );

            return new ActivityRequestInformationDto(
                    request.getId(),
                    request.getTitle(),
                    request.getSport().getName(),
                    request.getMaxPlayers(),
                    currentPlayers,
                    request.getEventDate(),
                    request.getAddressText(),
                    request.getNumberOfRounds(),
                    request.getStatus(),
                    request.getRegistrationOpen(),
                    request.getLocationPoint().getY(),
                    request.getLocationPoint().getX()
            );
        });
    }

    /**
     * Checks if the activity request has available spots for new participants.
     *
     * @param request The activity request to check
     * @throws IllegalArgumentException if the request doesn't have available spots (i.e., it is full).
     */
    private void checkCapacity(ActivityRequest request) {
        long currentPlayers = requestParticipantRepository
                .countByActivityRequestIdAndStatus(request.getId(), ParticipantStatus.ACCEPTED);
        
        if (currentPlayers >= request.getMaxPlayers()) {
            throw new IllegalStateException("This activity request is already full");
        }
    }
}
