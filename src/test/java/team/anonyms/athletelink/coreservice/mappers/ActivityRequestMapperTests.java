package team.anonyms.athletelink.coreservice.mappers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestDetailsDto;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestFeedDto;
import team.anonyms.athletelink.coreservice.dto.ParticipantDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.Sport;
import team.anonyms.athletelink.coreservice.enums.ParticipantRole;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityRequestMapperTests {

    private final ActivityRequestMapper mapper = new ActivityRequestMapperImpl();
    private final UUID requestId = UUID.randomUUID();
    private final UUID organizerId = UUID.randomUUID();
    private final Instant eventDate = Instant.parse("2030-01-01T12:00:00Z");

    @Test
    void feedMapsEveryDisplayedFieldAndKeepsCoordinateOrder() {
        ActivityRequest request = request();

        ActivityRequestFeedDto result = mapper.toFeedDto(request, 3);

        assertEquals(new ActivityRequestFeedDto(requestId, "Evening game", "Football", 8, 3,
                eventDate, "Moscow", 5, true, 55.7558, 37.6173), result);
    }

    @Test
    void detailsIncludeParticipantsStatusAndCompletionError() {
        ActivityRequest request = request();
        request.setStatus(RequestStatus.ACTIVE);
        request.setRegistrationOpen(false);
        request.setCompletionErrorMessage("Round 2 is missing");
        ParticipantDto organizer = new ParticipantDto(organizerId, ParticipantRole.ORGANIZER,
                ParticipantStatus.ACCEPTED, Instant.parse("2026-01-01T12:00:00Z"));
        ParticipantDto departed = new ParticipantDto(UUID.randomUUID(), ParticipantRole.PLAYER,
                ParticipantStatus.LEFT, Instant.parse("2026-01-02T12:00:00Z"));

        ActivityRequestDetailsDto result = mapper.toDetailsDto(request, 1, List.of(organizer, departed));

        assertEquals(new ActivityRequestDetailsDto(requestId, "Evening game", "Bring a ball",
                "Football", 8, 1, eventDate, "Moscow", 5, 55.7558, 37.6173,
                RequestStatus.ACTIVE, "Round 2 is missing", false, organizerId,
                List.of(organizer, departed)), result);
    }

    private ActivityRequest request() {
        var point = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(37.6173, 55.7558));
        return ActivityRequest.builder()
                .id(requestId)
                .title("Evening game")
                .description("Bring a ball")
                .sport(Sport.builder().id(1L).name("Football").build())
                .maxPlayers(8)
                .eventDate(eventDate)
                .addressText("Moscow")
                .numberOfRounds(5)
                .locationPoint(point)
                .status(RequestStatus.PLANNED)
                .registrationOpen(true)
                .organizerId(organizerId)
                .build();
    }
}
