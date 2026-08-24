package team.anonyms.athletelink.coreservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
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
@Table(name = "activity_requests")
public class ActivityRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Size(max = 100)
    @NotBlank
    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sport_id")
    private Sport sport;

    @NotNull
    private Instant eventDate;

    @NotBlank
    private String addressText;

    @NotNull
    private Point locationPoint;

    @Size(max = 20)
    @NotBlank
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private RequestStatus status;

    @NotNull
    private UUID organizerId;

    @CreationTimestamp
    private Instant createdAt;
}