package team.anonyms.athletelink.coreservice.repositories;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.enums.ParticipantRole;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.mappers.ActivityRequestMapperImpl;
import team.anonyms.athletelink.coreservice.services.ActivityRequestService;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({ActivityRequestService.class, ActivityRequestMapperImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class JoinRequestConcurrencyIntegrationTests {
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
    @Autowired SportRepository sports;
    @Autowired ActivityRequestService service;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousJoinsCannotFillTheLastPlaceTwice() throws Exception {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        UUID requestId = transactions.execute(status -> {
            UUID organizerId = UUID.randomUUID();
            ActivityRequest request = requests.save(ActivityRequest.builder()
                    .title("Two-player game")
                    .sport(sports.findAll().getFirst())
                    .maxPlayers(2)
                    .eventDate(Instant.now().plus(Duration.ofDays(10)))
                    .addressText("Test address")
                    .numberOfRounds(1)
                    .locationPoint(new GeometryFactory(new PrecisionModel(), 4326)
                            .createPoint(new Coordinate(37.6173, 55.7558)))
                    .status(RequestStatus.PLANNED)
                    .registrationOpen(true)
                    .organizerId(organizerId)
                    .build());
            participants.save(RequestParticipant.builder()
                    .activityRequest(request)
                    .userId(organizerId)
                    .role(ParticipantRole.ORGANIZER)
                    .status(ParticipantStatus.ACCEPTED)
                    .build());
            return request.getId();
        });
        UUID firstUser = UUID.randomUUID();
        UUID secondUser = UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Throwable> first = executor.submit(joinAttempt(requestId, firstUser, ready, start));
            Future<Throwable> second = executor.submit(joinAttempt(requestId, secondUser, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            Throwable firstResult = first.get(30, TimeUnit.SECONDS);
            Throwable secondResult = second.get(30, TimeUnit.SECONDS);
            int successfulJoins = (firstResult == null ? 1 : 0) + (secondResult == null ? 1 : 0);
            assertEquals(1, successfulJoins);
            IllegalStateException rejected = assertInstanceOf(IllegalStateException.class,
                    firstResult == null ? secondResult : firstResult);
            assertEquals("This activity request is already full", rejected.getMessage());

            transactions.executeWithoutResult(status -> {
                assertEquals(2, participants.countByActivityRequestIdAndStatus(
                        requestId, ParticipantStatus.ACCEPTED));
                long joinedUsers = participants.findAllByActivityRequestId(requestId).stream()
                        .filter(participant -> participant.getUserId().equals(firstUser)
                                || participant.getUserId().equals(secondUser))
                        .count();
                assertEquals(1, joinedUsers);
            });
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Callable<Throwable> joinAttempt(UUID requestId, UUID userId,
                                            CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            try {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    return new IllegalStateException("Start signal timed out");
                }
                service.joinRequest(requestId, userId);
                return null;
            } catch (Throwable failure) {
                return failure;
            }
        };
    }
}
