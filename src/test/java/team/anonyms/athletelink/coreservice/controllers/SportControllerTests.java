package team.anonyms.athletelink.coreservice.controllers;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team.anonyms.athletelink.coreservice.dto.SportDto;
import team.anonyms.athletelink.coreservice.services.SportService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SportController.class)
class SportControllerTests {

    @Autowired MockMvc mockMvc;
    @MockitoBean SportService sportService;

    @Test
    void returnsPlayableAndUnplayableSportsWithTheirPlayerLimits() throws Exception {
        when(sportService.getAllSports()).thenReturn(List.of(
                new SportDto(1L, "Football", 2, 22, "https://example.test/football.png", true),
                new SportDto(2L, "Retired sport", 4, 8, null, false)));

        mockMvc.perform(get("/sports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Football"))
                .andExpect(jsonPath("$[0].minPlayers").value(2))
                .andExpect(jsonPath("$[0].maxPlayers").value(22))
                .andExpect(jsonPath("$[0].iconUrl").value("https://example.test/football.png"))
                .andExpect(jsonPath("$[0].isPlayable").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].isPlayable").value(false));
        verify(sportService).getAllSports();
    }

    @Test
    void returnsEmptyArrayForEmptyCatalog() throws Exception {
        when(sportService.getAllSports()).thenReturn(List.of());

        mockMvc.perform(get("/sports"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(sportService).getAllSports();
    }
}
