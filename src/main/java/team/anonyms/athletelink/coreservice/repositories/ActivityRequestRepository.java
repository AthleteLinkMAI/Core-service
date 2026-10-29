package team.anonyms.athletelink.coreservice.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;

import java.util.Collection;
import java.util.Optional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ActivityRequestRepository extends JpaRepository<ActivityRequest, UUID> {

    /**
     * Finds nearby activity requests based on the provided latitude, longitude, radius, and optional sport ID.
     * The query filters requests that are planned, have an event date in the future, and
     * are within the specified radius from the given coordinates. 
     * If a sport ID is provided, it filters requests for that specific sport.
     * The filtered requests are ordered by their distance from the provided coordinates.
     * How it works in detail:
     * 1. ST_MakePoint(:lon, :lat) - creates a Point object from user coordinates 
     * (longitude (долгота) X, then latitude (широта Y).
     * 2. ST_SetSRID(..., 4326) - indicates that these are GPS coordinates.
     * 3. ::geography - converts flat coordinates to spherical, so the radius works in meters.
     * 4. ST_DWithin - a fast PostGIS function that checks if a point is within a radius.
     * 5. ST_Distance - calculates the distance for sorting requests (from closest one to farthest in radius).
     */
    @Query(value = """
            SELECT * FROM activity_requests
            WHERE status = 'PLANNED'
              AND registration_open = TRUE
              AND event_date > CURRENT_TIMESTAMP
              AND (CAST(:sportId AS BIGINT) IS NULL OR sport_id = CAST(:sportId AS BIGINT))
              AND (CAST(:startDate AS timestamptz) IS NULL OR event_date >= CAST(:startDate AS timestamptz))
              AND (CAST(:endDate AS timestamptz) IS NULL OR event_date <= CAST(:endDate AS timestamptz))
              AND ST_DWithin(
                  location_point::geography,
                  ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                  :radius
              )
            ORDER BY ST_Distance(
                location_point::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
            ) ASC
            """, nativeQuery = true)
    List<ActivityRequest> findNearbyRequests(
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("radius") double radiusInMeters,
        @Param("sportId") Long sportId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ar
        FROM ActivityRequest ar
        WHERE ar.id = :id
        """)
    Optional<ActivityRequest> findByIdForUpdate(@Param("id") UUID requestId);

    /**
     * Finds requests organized by the user or associated with any of their participation records,
     * limited to the supplied request statuses.
     */
    @EntityGraph(attributePaths = "sport")
    @Query(
            value = """
        SELECT r FROM ActivityRequest r
        WHERE (
            r.organizerId = :userId
            OR EXISTS (
                SELECT p.id FROM RequestParticipant p
                WHERE p.activityRequest = r AND p.userId = :userId
            )
                    )
                    AND r.status IN :statuses
        ORDER BY r.eventDate DESC, r.id DESC
        """,
            countQuery = """
        SELECT count(r) FROM ActivityRequest r
        WHERE (
            r.organizerId = :userId
            OR EXISTS (
                SELECT p.id FROM RequestParticipant p
                WHERE p.activityRequest = r AND p.userId = :userId
            )
        )
        AND r.status IN :statuses
        """
    )
    Page<ActivityRequest> findAllForUser(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<RequestStatus> statuses,
            Pageable pageable
    );

    /** Finds requests organized by the user, limited to the supplied request statuses. */
    @EntityGraph(attributePaths = "sport")
    @Query(
            value = """
        SELECT r FROM ActivityRequest r
        WHERE r.organizerId = :userId
          AND r.status IN :statuses
        ORDER BY r.eventDate DESC, r.id DESC
        """,
            countQuery = """
        SELECT count(r) FROM ActivityRequest r
        WHERE r.organizerId = :userId
          AND r.status IN :statuses
        """
    )
    Page<ActivityRequest> findAllCreatedByUser(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<RequestStatus> statuses,
            Pageable pageable
    );

    void deleteAllByOrganizerId(UUID organizerId);

    @Query("""
        SELECT ar.id FROM ActivityRequest ar
        WHERE ar.status = :planned
          AND ar.eventDate <= :oneHourAgo
          AND (ar.eventDate <= :twoDaysAgo
               OR ar.firstReminderEnqueuedAt IS NULL
               OR (ar.eventDate <= :oneDayAgo AND ar.secondReminderEnqueuedAt IS NULL))
        ORDER BY ar.eventDate, ar.id
        """)
    List<UUID> findDueRequestIds(
            @Param("planned") RequestStatus planned,
            @Param("oneHourAgo") Instant oneHourAgo,
            @Param("oneDayAgo") Instant oneDayAgo,
            @Param("twoDaysAgo") Instant twoDaysAgo,
            Pageable pageable
    );
}
