package team.anonyms.athletelink.coreservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import team.anonyms.athletelink.core_service.enums.ParticipantRole;
import team.anonyms.athletelink.core_service.enums.ParticipantStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 20)
    private ParticipantRole role;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 20)
    private ParticipantStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant joinedAt;
}