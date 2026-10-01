package team.anonyms.athletelink.coreservice.services.kafka;

import org.junit.jupiter.api.Test;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestResolvedDto;
import team.anonyms.athletelink.coreservice.dto.kafka.GameRoundResultDto;
import team.anonyms.athletelink.coreservice.enums.RoundOutcome;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
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
        var reply = new CloseRequestResolvedDto(CloseRequestResolvedDto.TYPE,
                UUID.randomUUID(), UUID.randomUUID(), false, "Round 2 is missing");

        assertEquals(reply, mapper.readValue(mapper.writeValueAsString(reply), CloseRequestResolvedDto.class));
    }
}
