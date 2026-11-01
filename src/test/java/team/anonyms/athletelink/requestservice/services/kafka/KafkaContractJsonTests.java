package team.anonyms.athletelink.requestservice.services.kafka;

import org.junit.jupiter.api.Test;
import team.anonyms.athletelink.requestservice.dto.kafka.ActivityRequestStartedDto;
import team.anonyms.athletelink.requestservice.dto.kafka.JoinDecisionDto;
import team.anonyms.athletelink.requestservice.dto.kafka.NewParticipantDto;
import team.anonyms.athletelink.requestservice.dto.kafka.RequestStatusChangedDto;
import team.anonyms.athletelink.requestservice.enums.ParticipantRole;
import team.anonyms.athletelink.requestservice.enums.RequestStatus;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaContractJsonTests {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void joinRequestIncludesOrganizerAndCorrelationId() throws Exception {
        var event = new NewParticipantDto(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID());
        String json = mapper.writeValueAsString(event);
        assertEquals(Set.of("type", "eventId", "requestId", "userId", "organizerId"),
                mapper.readValue(json, Map.class).keySet());
        assertEquals(event, mapper.readValue(json, NewParticipantDto.class));
    }

    @Test
    void startSnapshotContainsParticipantsAndSportName() throws Exception {
        var event = new ActivityRequestStartedDto(UUID.randomUUID(), UUID.randomUUID(),
                "Футбол", 3, UUID.randomUUID(), List.of(
                new ActivityRequestStartedDto.Participant(UUID.randomUUID(),
                        UUID.randomUUID(), ParticipantRole.ORGANIZER)));
        assertEquals(event, mapper.readValue(mapper.writeValueAsString(event), ActivityRequestStartedDto.class));
    }

    @Test
    void replyContractsRoundTrip() throws Exception {
        var join = new JoinDecisionDto(JoinDecisionDto.TYPE, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), true);
        var status = new RequestStatusChangedDto(RequestStatusChangedDto.TYPE,
                UUID.randomUUID(), UUID.randomUUID(), RequestStatus.COMPLETED);
        assertEquals(join, mapper.readValue(mapper.writeValueAsString(join), JoinDecisionDto.class));
        assertEquals(status, mapper.readValue(mapper.writeValueAsString(status), RequestStatusChangedDto.class));
    }
}
