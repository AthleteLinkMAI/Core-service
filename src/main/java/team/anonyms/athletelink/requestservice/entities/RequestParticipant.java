package team.anonyms.athletelink.requestservice.entities;

import jakarta.persistence.*;
import lombok.*;
import team.anonyms.athletelink.requestservice.enums.ParticipantRole;
import team.anonyms.athletelink.requestservice.enums.ParticipantStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "request_participants")
public class RequestParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private ActivityRequest activityRequest;

    @NotNull(message = "User ID cannot be null")
    private UUID userId;

    @NotNull(message = "Participant role is required")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ParticipantRole role;

    @NotNull(message = "Participant status is required")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ParticipantStatus status;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "join_attempt_id")
    private UUID joinAttemptId;
}
