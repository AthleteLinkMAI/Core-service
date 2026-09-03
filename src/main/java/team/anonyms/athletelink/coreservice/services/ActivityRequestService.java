package team.anonyms.athletelink.coreservice.services;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.transaction.annotation.Transactional;

import team.anonyms.athletelink.coreservice.dto.CreateActivityRequestDto;
import team.anonyms.athletelink.coreservice.dto.ParticipantDto;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestDetailsDto;

import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.entities.Sport;
import team.anonyms.athletelink.coreservice.enums.ParticipantRole;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.coreservice.repositories.SportRepository;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

public class ActivityRequestService {
    private final ActivityRequestRepository activityRequestRepository;
    private final RequestParticipantRepository requestParticipantRepository;
    private final SportRepository sportRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public ActivityRequestService(
        ActivityRequestRepository activityRequestRepository, 
        RequestParticipantRepository requestParticipantRepository, 
        SportRepository sportRepository
    ) {
        this.activityRequestRepository = activityRequestRepository;
        this.requestParticipantRepository = requestParticipantRepository;
        this.sportRepository = sportRepository;
    }

    @Transactional
    public UUID createRequest(CreateActivityRequestDto dto, String organizerIdStr) {
        UUID organizerId = UUID.fromString(organizerIdStr);

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

    @Transactional
    public void joinRequest(UUID requestId, String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        
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

    @Transactional
    public void leaveRequest(UUID requestId, String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);

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

        return new ActivityRequestDetailsDto(
                request.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getSport().getName(),
                request.getSport().getMaxPlayers(),
                currentPlayers,
                request.getEventDate(),
                request.getAddressText(),
                request.getLocationPoint().getY(),
                request.getLocationPoint().getX(),
                request.getStatus(),
                request.getOrganizerId(),
                participantDtos
        );
    }

    private void checkCapacity(ActivityRequest request) {
        long currentPlayers = requestParticipantRepository
                .countByActivityRequestIdAndStatus(request.getId(), ParticipantStatus.ACCEPTED);
        
        if (currentPlayers >= request.getSport().getMaxPlayers()) {
            throw new IllegalArgumentException("This activity request is already full");
        }
    }
}