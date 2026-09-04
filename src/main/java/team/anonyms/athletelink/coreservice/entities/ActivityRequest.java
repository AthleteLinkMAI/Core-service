package team.anonyms.athletelink.coreservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    @Size(max = 100, message = "Title cannot exceed 100 characters")
    @NotBlank(message = "Title is required")
    private String title;

    @Size(max = 1024, message = "Description cannot exceed 1024 characters.")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sport_id")
    private Sport sport;

    @NotNull(message = "Event date is required")
    private Instant eventDate;

    @NotBlank(message = "Address text is required")
    private String addressText;

    @NotNull(message = "Location point is required")
    private Point locationPoint;

    @NotNull(message = "Number of rounds is required")
    private Integer numberOfRounds;

    @NotNull(message = "Max players count is required")
    private Integer maxPlayers;

    @NotNull(message = "Request status is required")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private RequestStatus status;

    @NotNull(message = "Organizer ID is required")
    private UUID organizerId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}