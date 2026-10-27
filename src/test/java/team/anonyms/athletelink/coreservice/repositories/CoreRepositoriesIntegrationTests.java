package team.anonyms.athletelink.coreservice.repositories;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.OutboxEvent;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.entities.Sport;
import team.anonyms.athletelink.coreservice.enums.ParticipantRole;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;

import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class CoreRepositoriesIntegrationTests {
    @Container
    static final PostgreSQLContainer database = new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:15-3.3").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }

    @Autowired ActivityRequestRepository requests;
    @Autowired RequestParticipantRepository participants;
    @Autowired OutboxEventRepository outbox;
    @Autowired SportRepository sports;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final Instant future = Instant.now().plus(Duration.ofDays(20)).truncatedTo(ChronoUnit.SECONDS);

    @Test
    void nearbyQueryFiltersUnavailableRequestsAndSortsByDistance() {
        Sport sport = sports.findAll().getFirst();
        ActivityRequest nearest = request(sport, 37.6173, 55.7558, future, RequestStatus.PLANNED, true);
        ActivityRequest farther = request(sport, 37.6273, 55.7558, future, RequestStatus.PLANNED, true);
        request(sport, 37.6173, 55.7558, future, RequestStatus.PLANNED, false);
        request(sport, 37.6173, 55.7558, future, RequestStatus.ACTIVE, true);
        request(sport, 37.6173, 55.7558, Instant.now().minus(Duration.ofDays(1)), RequestStatus.PLANNED, true);
        request(sport, 40.0, 55.7558, future, RequestStatus.PLANNED, true);
        requests.flush();
        entityManager.clear();

        List<UUID> ids = requests.findNearbyRequests(55.7558, 37.6173, 1000, null, null, null)
                .stream().map(ActivityRequest::getId).toList();

        assertEquals(List.of(nearest.getId(), farther.getId()), ids);
    }

    @Test
    void nearbyQueryAppliesSportAndInclusiveDateBounds() {
        List<Sport> availableSports = sports.findAll();
        Sport selectedSport = availableSports.get(0);
        Sport otherSport = availableSports.get(1);
        ActivityRequest atStart = request(selectedSport, 37.6173, 55.7558, future,
                RequestStatus.PLANNED, true);
        ActivityRequest atEnd = request(selectedSport, 37.6173, 55.7558, future.plus(Duration.ofDays(2)),
                RequestStatus.PLANNED, true);
        request(selectedSport, 37.6173, 55.7558, future.minus(Duration.ofDays(1)),
                RequestStatus.PLANNED, true);
        request(selectedSport, 37.6173, 55.7558, future.plus(Duration.ofDays(3)),
                RequestStatus.PLANNED, true);
        request(otherSport, 37.6173, 55.7558, future,
                RequestStatus.PLANNED, true);
        requests.flush();
        entityManager.clear();

        List<UUID> ids = requests.findNearbyRequests(55.7558, 37.6173, 1000,
                        selectedSport.getId(), future, future.plus(Duration.ofDays(2)))
                .stream().map(ActivityRequest::getId).toList();

        assertEquals(2, ids.size());
        assertTrue(ids.containsAll(List.of(atStart.getId(), atEnd.getId())));
    }

    @Test
    void participantQueriesKeepRequestsAndStatusesSeparateAndDeleteUserEverywhere() {
        Sport sport = sports.findAll().getFirst();
        ActivityRequest first = request(sport, 37.6173, 55.7558, future, RequestStatus.PLANNED, true);
        ActivityRequest second = request(sport, 37.6173, 55.7558, future, RequestStatus.PLANNED, true);
        UUID sharedUser = UUID.randomUUID();
        RequestParticipant accepted = participant(first, sharedUser, ParticipantStatus.ACCEPTED);
        RequestParticipant pending = participant(first, UUID.randomUUID(), ParticipantStatus.PENDING);
        participant(second, sharedUser, ParticipantStatus.LEFT);
        participants.flush();
        entityManager.clear();

        assertEquals(1, participants.countByActivityRequestIdAndStatus(first.getId(), ParticipantStatus.ACCEPTED));
        assertEquals(0, participants.countByActivityRequestIdAndStatus(second.getId(), ParticipantStatus.ACCEPTED));
        assertEquals(accepted.getId(), participants.findByActivityRequestIdAndUserId(first.getId(), sharedUser)
                .orElseThrow().getId());
        assertTrue(participants.findByActivityRequestIdAndUserId(second.getId(), pending.getUserId()).isEmpty());
        assertEquals(2, participants.findAllByActivityRequestId(first.getId()).size());
        assertEquals(List.of(pending.getId()), participants.findAllByActivityRequestIdAndStatus(
                first.getId(), ParticipantStatus.PENDING).stream().map(RequestParticipant::getId).toList());

        participants.deleteAllByUserId(sharedUser);
        participants.flush();
        entityManager.clear();

        assertEquals(0, participants.countByActivityRequestIdAndStatus(first.getId(), ParticipantStatus.ACCEPTED));
        assertTrue(participants.findAllByActivityRequestId(second.getId()).isEmpty());
        assertEquals(List.of(pending.getId()), participants.findAllByActivityRequestId(first.getId())
                .stream().map(RequestParticipant::getId).toList());
    }

    @Test
    void databaseRejectsDuplicateParticipantForOneRequest() {
        ActivityRequest request = request(sports.findAll().getFirst(), 37.6173, 55.7558,
                future, RequestStatus.PLANNED, true);
        UUID userId = UUID.randomUUID();
        participant(request, userId, ParticipantStatus.ACCEPTED);
        participants.flush();

        participant(request, userId, ParticipantStatus.PENDING);
        assertThrows(DataIntegrityViolationException.class, participants::flush);
    }

    @Test
    void deletingOrganizersRequestsCascadesToTheirParticipantsOnly() {
        Sport sport = sports.findAll().getFirst();
        UUID organizer = UUID.randomUUID();
        ActivityRequest owned = request(sport, 37.6173, 55.7558, future, RequestStatus.PLANNED, true);
        owned.setOrganizerId(organizer);
        requests.saveAndFlush(owned);
        ActivityRequest other = request(sport, 37.6173, 55.7558, future, RequestStatus.PLANNED, true);
        RequestParticipant ownedParticipant = participant(owned, UUID.randomUUID(), ParticipantStatus.ACCEPTED);
        RequestParticipant otherParticipant = participant(other, UUID.randomUUID(), ParticipantStatus.ACCEPTED);
        participants.flush();
        entityManager.clear();

        requests.deleteAllByOrganizerId(organizer);
        requests.flush();
        entityManager.clear();

        assertFalse(requests.existsById(owned.getId()));
        assertFalse(participants.existsById(ownedParticipant.getId()));
        assertTrue(requests.existsById(other.getId()));
        assertTrue(participants.existsById(otherParticipant.getId()));
    }

    @Test
    void pendingOutboxSelectsOldestThenSmallestIdAndSkipsPublished() {
        Instant older = future.minus(Duration.ofDays(1));
        Instant newer = future;
        UUID lowestId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID nextId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID newestId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID publishedId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        saveOutbox(lowestId, older, false);
        saveOutbox(nextId, older, false);
        saveOutbox(newestId, newer, false);
        saveOutbox(publishedId, older.minusSeconds(1), true);
        entityManager.clear();

        assertEquals(lowestId, outbox.findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc()
                .orElseThrow().getId());
        markPublished(lowestId);
        assertEquals(nextId, outbox.findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc()
                .orElseThrow().getId());
        markPublished(nextId);
        assertEquals(newestId, outbox.findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc()
                .orElseThrow().getId());
        markPublished(newestId);
        assertTrue(outbox.findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc().isEmpty());
    }

    private ActivityRequest request(Sport sport, double longitude, double latitude, Instant date,
                                    RequestStatus status, boolean registrationOpen) {
        return requests.save(ActivityRequest.builder()
                .title("Test activity")
                .sport(sport)
                .maxPlayers(4)
                .eventDate(date)
                .addressText("Test address")
                .numberOfRounds(1)
                .locationPoint(geometryFactory.createPoint(new Coordinate(longitude, latitude)))
                .status(status)
                .registrationOpen(registrationOpen)
                .organizerId(UUID.randomUUID())
                .build());
    }

    private RequestParticipant participant(ActivityRequest request, UUID userId, ParticipantStatus status) {
        return participants.save(RequestParticipant.builder()
                .activityRequest(request)
                .userId(userId)
                .role(ParticipantRole.PLAYER)
                .status(status)
                .build());
    }

    private void saveOutbox(UUID id, Instant createdAt, boolean published) {
        OutboxEvent event = outbox.saveAndFlush(new OutboxEvent(id, UUID.randomUUID(), "game.commands", "{}"));
        jdbc.update("UPDATE outbox_events SET created_at = ?, published_at = ? WHERE id = ?",
                Timestamp.from(createdAt), published ? Timestamp.from(createdAt) : null, event.getId());
    }

    private void markPublished(UUID id) {
        OutboxEvent event = outbox.findById(id).orElseThrow();
        event.markPublished();
        outbox.saveAndFlush(event);
        entityManager.clear();
    }
}
