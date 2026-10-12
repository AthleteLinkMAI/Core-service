package team.anonyms.athletelink.coreservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.coreservice.dto.SportDto;
import team.anonyms.athletelink.coreservice.entities.Sport;
import team.anonyms.athletelink.coreservice.repositories.SportRepository;

import java.lang.Long;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SportServiceTests {

    @Mock
    private SportRepository sportRepository;

    @InjectMocks
    private SportService sportService;

    @Test
    void testGetAllSports_ReturnsList() {

        Long sportIdActive = Integer.toUnsignedLong(1);
        Long sportIdInactive = Integer.toUnsignedLong(2);

        Sport mockSportActive = Sport.builder()
                .id(sportIdActive)
                .name("Active")
                .minPlayers(2)
                .maxPlayers(10)
                .iconUrl("1")
                .isPlayable(true)
                .build();
        Sport mockSportInactive = Sport.builder()
                .id(sportIdInactive)
                .name("Inactive")
                .minPlayers(4)
                .maxPlayers(8)
                .iconUrl(null)
                .isPlayable(false)
                .build();

        Mockito.when(sportRepository.findAll()).thenReturn(List.of(mockSportActive, mockSportInactive));

        List<SportDto> sports = sportService.getAllSports();

        assertEquals(2, sports.size());

        assertEquals(sportIdActive, sports.get(0).id());
        assertEquals(sportIdInactive, sports.get(1).id());

        assertEquals("Active", sports.get(0).name());
        assertEquals("Inactive", sports.get(1).name());

        assertEquals(2, sports.get(0).minPlayers());
        assertEquals(4, sports.get(1).minPlayers());

        assertEquals(10, sports.get(0).maxPlayers());
        assertEquals(8, sports.get(1).maxPlayers());

        assertEquals("1", sports.get(0).iconUrl());
        assertNull(sports.get(1).iconUrl());

        assertNotEquals(sports.get(0).id(), sports.get(1).id());
        assertNotEquals(sports.get(0).isPlayable(), sports.get(1).isPlayable());
    }

    @Test
    void testGetAllSports_ReturnsEmptyList() {

        Mockito.when(sportRepository.findAll()).thenReturn(List.of());

        List<SportDto> sports = sportService.getAllSports();

        assertEquals(0, sports.size());
    }
}
