package team.anonyms.athletelink.coreservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;
import team.anonyms.athletelink.core_service.enums.RequestStatus;

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

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sport_id")
    private Sport sport;

    @Column(nullable = false)
    private Instant eventDate;

    @Column(nullable = false, length = 255)
    private String addressText;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point locationPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(nullable = false)
    private UUID organizerId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}