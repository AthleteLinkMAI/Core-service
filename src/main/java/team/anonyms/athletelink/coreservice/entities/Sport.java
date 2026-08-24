package team.anonyms.athletelink.coreservice.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "sports")
public class Sport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 50)
    @NotBlank
    private String name;

    @NotNull
    private Integer minPlayers;

    @NotNull
    private Integer maxPlayers;

    @Size(max = 255)
    @NotBlank
    private String iconUrl;
}