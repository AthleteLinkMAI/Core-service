package team.anonyms.athletelink.coreservice.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import team.anonyms.athletelink.coreservice.dto.RoundResultResponseDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.ActivityRoundPlayerResult;
import team.anonyms.athletelink.coreservice.entities.ActivityRoundResult;
import team.anonyms.athletelink.coreservice.enums.RoundOutcome;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityResultsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "activityRequest", source = "request")
    ActivityRoundResult toRoundResult(ActivityRequest request, Integer roundNumber, UUID recordedBy);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roundResult", source = "round")
    ActivityRoundPlayerResult toPlayerResult(ActivityRoundResult round, UUID playerId, RoundOutcome outcome);

    @Mapping(target = "requestId", source = "round.activityRequest.id")
    @Mapping(target = "winners", source = "playerResults", qualifiedByName = "winnerIds")
    @Mapping(target = "losers", source = "playerResults", qualifiedByName = "loserIds")
    RoundResultResponseDto toResponse(ActivityRoundResult round, List<ActivityRoundPlayerResult> playerResults);

    @Named("winnerIds")
    default List<UUID> winnerIds(List<ActivityRoundPlayerResult> results) {
        return playerIdsByOutcome(results, RoundOutcome.WIN);
    }

    @Named("loserIds")
    default List<UUID> loserIds(List<ActivityRoundPlayerResult> results) {
        return playerIdsByOutcome(results, RoundOutcome.LOSS);
    }

    private List<UUID> playerIdsByOutcome(List<ActivityRoundPlayerResult> results, RoundOutcome outcome) {
        return results.stream()
                .filter(result -> result.getOutcome() == outcome)
                .map(ActivityRoundPlayerResult::getPlayerId)
                .toList();
    }
}
