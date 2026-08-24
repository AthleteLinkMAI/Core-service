package team.anonyms.athletelink.coreservice.services;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.core_service.dto.CreateActivityRequestDto;
import team.anonyms.athletelink.core_service.entities.ActivityRequest;
import team.anonyms.athletelink.core_service.entities.RequestParticipant;
import team.anonyms.athletelink.core_service.entities.Sport;
import team.anonyms.athletelink.core_service.enums.ParticipantRole;
import team.anonyms.athletelink.core_service.enums.ParticipantStatus;
import team.anonyms.athletelink.core_service.enums.RequestStatus;
import team.anonyms.athletelink.core_service.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.core_service.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.core_service.repositories.SportRepository;

import java.util.UUID;

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

        Coordinate coordinate = new Coordinate(dto.longitude(), dto.latitude());
        Point locationPoint = geometryFactory.createPoint(coordinate);

        ActivityRequest request = ActivityRequest.builder()
                .title(dto.title())
                .description(dto.description())
                .sport(sport)
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
}