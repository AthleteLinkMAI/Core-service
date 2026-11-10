package team.anonyms.athletelink.requestservice.services.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.requestservice.dto.kafka.RequestStatusChangedDto;
import team.anonyms.athletelink.requestservice.dto.kafka.UserChange;
import team.anonyms.athletelink.requestservice.dto.kafka.enums.UserChangeType;
import team.anonyms.athletelink.requestservice.enums.RequestStatus;
import team.anonyms.athletelink.requestservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.requestservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.requestservice.services.ActivityRequestService;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTests {
    @Mock ActivityRequestRepository requests;
    @Mock RequestParticipantRepository participants;
    @Mock ActivityRequestService service;

    private final ObjectMapper json = new ObjectMapper();
    private KafkaConsumerService consumer;

    @BeforeEach
    void setUp() {
        consumer = new KafkaConsumerService(requests, participants, service, json);
    }

    @Test
    void deleteUserRemovesRequestsAndMemberships() throws Exception {
        UUID userId = UUID.randomUUID();
        consumer.receiveUserChange(json.writeValueAsString(new UserChange(UserChangeType.DELETE, userId)));
        verify(requests).deleteAllByOrganizerId(userId);
        verify(participants).deleteAllByUserId(userId);
    }

    @Test
    void completionIsRoutedByType() throws Exception {
        var reply = new RequestStatusChangedDto(RequestStatusChangedDto.TYPE,
                UUID.randomUUID(), UUID.randomUUID(), RequestStatus.COMPLETED);
        consumer.receiveGameReply(json.writeValueAsString(reply));
        verify(service).applyGameStatus(reply);
    }

    @Test
    void malformedReplyCannotChangeState() {
        assertThrows(IllegalArgumentException.class, () -> consumer.receiveGameReply("{"));
        verifyNoInteractions(service);
    }
}
