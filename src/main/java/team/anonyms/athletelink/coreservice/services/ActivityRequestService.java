package team.anonyms.athletelink.coreservice.services;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import team.anonyms.athletelink.coreservice.dto.CreateActivityRequestDto;
import team.anonyms.athletelink.coreservice.dto.ParticipantDto;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestDetailsDto;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestFeedDto;

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

import java.util.UUID;
import java.util.Optional;
import java.util.List;

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
     * 
     * Business rules:
     * - Cannot create a request if the sport ID isn't in the list of available sports.
     * - Cannot create a request if the player limit (maxPlayers) is not within the sport's allowed range.
     *
     * @param dto Data transfer object containing the details of the request to be created
     * @param organizerIdStr ID of the organizer as a string (from the token)
     * @throws IllegalArgumentException if the sport is not found or the player limit is invalid.
     */
    @Transactional
    public UUID createRequest(CreateActivityRequestDto dto, UUID organizerId) {

        Sport sport = sportRepository.findById(dto.sportId())
                .orElseThrow(() -> new IllegalArgumentException("Sport not found"));

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
     * 
     * Business rules:
     * - Cannot join if the request status is not PLANNED.
     * - Cannot join if the player limit (maxPlayers) has been reached.
     * - If the user previously left (LEFT), they can rejoin if there are available spots.
     * - If the user was kicked by the organizer (KICKED), rejoining is forbidden.
     *
     * @param requestId ID of the target request
     * @param userIdStr ID of the user as a string (from the token)
     * @throws IllegalArgumentException if the request is not found, no spots available, or the user is blocked in this request
     */
    @Transactional
    public void joinRequest(UUID requestId, UUID userId) {
        
        ActivityRequest request = activityRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Activity request not found"));

        if (request.getStatus() != RequestStatus.PLANNED) {
            throw new IllegalArgumentException("Cannot join this request. Current status is: " + request.getStatus());
        }

        Optional<RequestParticipant> existingParticipantOpt = requestParticipantRepository
                .findByActivityRequestIdAndUserId(requestId, userId);

        if (existingParticipantOpt.isPresent()) {
            RequestParticipant existing = existingParticipantOpt.get();
            
            if (existing.getStatus() == ParticipantStatus.ACCEPTED) {
                throw new IllegalArgumentException("You are already a participant");
            }
            if (existing.getStatus() == ParticipantStatus.KICKED) {
                throw new IllegalArgumentException("You were kicked from this request and cannot rejoin");
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
     * 
     * Business rules:
     * - Cannot kick the organizer from the request. The organizer must cancel the request entirely.
     * - Cannot kick the user who is not a participant of the request.
     * - Cannot kick the user who has already left the request.
     *
     * @param requestId ID of the target request
     * @param userIdStr ID of the user as a string (from the token)
     * @throws IllegalArgumentException if the request is not found, the user is not a participant, or the user is the organizer.
     */
    @Transactional
    public void leaveRequest(UUID requestId, UUID userId) {

        RequestParticipant participant = requestParticipantRepository
                .findByActivityRequestIdAndUserId(requestId, userId)
                .orElseThrow(() -> new IllegalArgumentException("You are not a participant of this request"));

        if (participant.getRole() == ParticipantRole.ORGANIZER) {
            throw new IllegalArgumentException("Organizer cannot leave the request. You must cancel the request entirely.");
        }

        if (participant.getStatus() == ParticipantStatus.LEFT) {
            throw new IllegalArgumentException("You have already left this request");
        }

        participant.setStatus(ParticipantStatus.LEFT);
        requestParticipantRepository.save(participant);
    }

    /**
     * Cancels the activity request.
     * 
     * Business rules:
     * - Only the creator (organizer) of the request can cancel it.
     * - Cannot cancel a request that is already COMPLETED or CANCELLED.
     *
     * @param requestId ID of the request to cancel
     * @param userIdStr ID of the user attempting the cancellation
     */
    @Transactional
    public void cancelRequest(UUID requestId, UUID userId) {

        ActivityRequest request = activityRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Activity request not found"));

        if (!request.getOrganizerId().equals(userId)) {
            throw new IllegalArgumentException("Only the organizer can cancel this request");
        }

        if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel a request with status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.CANCELLED);
        activityRequestRepository.save(request);
    }

    /**
     * Kicks a participant from the activity request.
     * 
     * Business rules:
     * - Only the creator (organizer) can kick participants.
     * - The organizer cannot kick themselves.
     * - The target user must be an existing participant of this request.
     *
     * @param requestId ID of the request
     * @param targetUserIdStr ID of the user to be kicked
     * @param organizerIdStr ID of the user attempting the kick (must be organizer)
     */
    @Transactional
    public void kickParticipant(UUID requestId, UUID targetUserId, UUID organizerId) {

        ActivityRequest request = activityRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Activity request not found"));

        if (!request.getOrganizerId().equals(organizerId)) {
            throw new IllegalArgumentException("Only the organizer can kick participants");
        }

        if (organizerId.equals(targetUserId)) {
            throw new IllegalArgumentException("Organizer cannot kick themselves");
        }

        RequestParticipant targetParticipant = requestParticipantRepository
                .findByActivityRequestIdAndUserId(requestId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user is not a participant of this request"));

        if (targetParticipant.getStatus() == ParticipantStatus.KICKED) {
            throw new IllegalArgumentException("User is already kicked");
        }

        targetParticipant.setStatus(ParticipantStatus.KICKED);
        requestParticipantRepository.save(targetParticipant);
    }

    /**
     * Gets the details of a specific activity request, including its participants and their statuses.
     *
     * @param requestId ID of the target request
     * @throws IllegalArgumentException if the request is not found.
     */
    public ActivityRequestDetailsDto getRequestDetails(UUID requestId) {
        ActivityRequest request = activityRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Activity request not found"));

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

    /**
     * Searches for nearby activity requests based on the provided latitude, longitude, radius, and optional sport ID.
     * The search filters requests that are planned, have an event date in the future, and
     * are within the specified radius from the given coordinates.
     * If a sport ID is provided, it filters requests for that specific sport.
     */
    @Transactional(readOnly = true)
    public List<ActivityRequestFeedDto> searchNearbyRequests(
            double latitude, double longitude, double radiusInMeters, Long sportId) {
        List<ActivityRequest> nearbyRequests = activityRequestRepository
                .findNearbyRequests(latitude, longitude, radiusInMeters, sportId);

        return nearbyRequests.stream().map(request -> {
            
            int currentPlayers = (int) requestParticipantRepository
                    .countByActivityRequestIdAndStatus(request.getId(), ParticipantStatus.ACCEPTED);
            
            return requestMapper.toFeedDto(request, currentPlayers);
            
        }).toList();
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
        
        if (currentPlayers >= request.getSport().getMaxPlayers()) {
            throw new IllegalArgumentException("This activity request is already full");
        }
    }
}