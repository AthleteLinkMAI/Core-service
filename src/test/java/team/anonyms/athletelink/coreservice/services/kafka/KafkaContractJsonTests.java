package team.anonyms.athletelink.coreservice.services.kafka;

import org.junit.jupiter.api.Test;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestDto;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestResolvedDto;
import team.anonyms.athletelink.coreservice.dto.kafka.GameRoundResultDto;
import team.anonyms.athletelink.coreservice.enums.RoundOutcome;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaContractJsonTests {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void roundCommandIsPlainJsonWithTypedPlayerOutcomes() throws Exception {
        var event = new GameRoundResultDto(UUID.randomUUID(), UUID.randomUUID(), 2, 7L,
                UUID.randomUUID(), List.of(new GameRoundResultDto.PlayerResult(UUID.randomUUID(), RoundOutcome.WIN)));

        String json = mapper.writeValueAsString(event);
        GameRoundResultDto decoded = mapper.readValue(json, GameRoundResultDto.class);

        assertTrue(json.contains("\"type\":\"ROUND_RESULT_SUBMITTED\""));
        assertEquals(event, decoded);
    }

    @Test
    void gameClosureReplyCanBeDeserialized() throws Exception {
        var reply = new CloseRequestResolvedDto(UUID.randomUUID(),
                CloseRequestResolvedDto.TYPE, false, "Round 2 is missing");

        assertEquals(reply, mapper.readValue(mapper.writeValueAsString(reply), CloseRequestResolvedDto.class));
    }

    @Test
    void closureCommandContainsOnlyContractFields() throws Exception {
        var command = new CloseRequestDto(UUID.randomUUID(), 3);
        String json = mapper.writeValueAsString(command);

        assertEquals(Set.of("type", "requestId", "expectedRounds"),
                mapper.readValue(json, Map.class).keySet());
        assertEquals(command, mapper.readValue(json, CloseRequestDto.class));
    }
}
