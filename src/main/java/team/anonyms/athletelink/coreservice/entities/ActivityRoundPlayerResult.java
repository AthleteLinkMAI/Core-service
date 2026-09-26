package team.anonyms.athletelink.coreservice.entities;

import jakarta.persistence.*;
import lombok.*;
import team.anonyms.athletelink.coreservice.enums.RoundOutcome;

import java.util.UUID;

@Entity
@Table(name = "activity_round_player_results")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ActivityRoundPlayerResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_result_id", nullable = false)
    private ActivityRoundResult roundResult;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 8)
    private RoundOutcome outcome;
}
