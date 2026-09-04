package team.anonyms.athletelink.coreservice.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestDetailsDto;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestFeedDto;
import team.anonyms.athletelink.coreservice.dto.ParticipantDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ActivityRequestMapper {

    @Mapping(target = "sportName", source = "request.sport.name")
    @Mapping(target = "latitude", expression = "java(request.getLocationPoint().getY())")
    @Mapping(target = "longitude", expression = "java(request.getLocationPoint().getX())")
    @Mapping(target = "currentPlayers", source = "currentPlayers")
    ActivityRequestFeedDto toFeedDto(ActivityRequest request, int currentPlayers);

    @Mapping(target = "sportName", source = "request.sport.name")
    @Mapping(target = "latitude", expression = "java(request.getLocationPoint().getY())")
    @Mapping(target = "longitude", expression = "java(request.getLocationPoint().getX())")
    @Mapping(target = "currentPlayers", source = "currentPlayers")
    @Mapping(target = "participants", source = "participants")
    ActivityRequestDetailsDto toDetailsDto(ActivityRequest request, int currentPlayers, List<ParticipantDto> participants);
}