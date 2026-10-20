package team.anonyms.athletelink.coreservice.services.kafka;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestResolvedDto;
import team.anonyms.athletelink.coreservice.dto.kafka.UserChange;
import team.anonyms.athletelink.coreservice.dto.kafka.enums.UserChangeType;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.coreservice.services.ActivityResultsService;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTests {

    @Mock ActivityRequestRepository requests;
    @Mock RequestParticipantRepository participants;
    @Mock ActivityResultsService results;

    private final ObjectMapper json = new ObjectMapper();
    private KafkaConsumerService consumer;

    @BeforeEach
    void setUp() {
        consumer = new KafkaConsumerService(requests, participants, results, json);
    }

    @Test
    void deleteUserRemovesOrganizedRequestsAndRemainingMemberships() throws Exception {
        UUID userId = UUID.randomUUID();

        consumer.receiveUserChange(json.writeValueAsString(new UserChange(UserChangeType.DELETE, userId)));

        verify(requests).deleteAllByOrganizerId(userId);
        verify(participants).deleteAllByUserId(userId);
        verifyNoInteractions(results);
    }

    @Test
    void createUserDoesNotDeleteData() throws Exception {
        UUID userId = UUID.randomUUID();

        consumer.receiveUserChange(json.writeValueAsString(new UserChange(UserChangeType.CREATE, userId)));

        verifyNoInteractions(requests, participants, results);
    }

    @Test
    void gameReplyIsDeserializedAndForwardedWithoutChangingIt() throws Exception {
        CloseRequestResolvedDto reply = new CloseRequestResolvedDto(UUID.randomUUID(),
                CloseRequestResolvedDto.TYPE, false, "Round 2 is missing");

        consumer.receiveGameReply(json.writeValueAsString(reply));

        verify(results).resolveCompletion(reply);
        verifyNoInteractions(requests, participants);
    }

    @Test
    void malformedUserChangeFailsBeforeDeletingAnything() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> consumer.receiveUserChange("{"));

        assertEquals("Invalid Kafka message", exception.getMessage());
        assertInstanceOf(Exception.class, exception.getCause());
        verifyNoInteractions(requests, participants, results);
    }

    @Test
    void malformedGameReplyFailsBeforeResolvingCompletion() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> consumer.receiveGameReply("{"));

        assertEquals("Invalid Kafka message", exception.getMessage());
        assertInstanceOf(Exception.class, exception.getCause());
        verifyNoInteractions(requests, participants, results);
    }
}
