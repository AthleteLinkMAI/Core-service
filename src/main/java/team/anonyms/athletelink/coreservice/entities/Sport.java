package team.anonyms.athletelink.coreservice.entities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    @Size(max = 100, message = "Sport name cannot exceed 100 characters")
    @NotBlank(message = "Sport name is required")
    private String name;

    @NotNull(message = "Minimum players count is required")
    private Integer minPlayers;

    @NotNull(message = "Maximum players count is required")
    private Integer maxPlayers;

    @Size(max = 255, message = "Icon URL cannot exceed 255 characters")
    private String iconUrl;

    @NotNull
    @Builder.Default
    @Column(name = "is_playable", nullable = false)
    private Boolean isPlayable = true;
}
