package team.anonyms.athletelink.coreservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;

import java.util.List;
import java.util.UUID;

public interface ActivityRequestRepository extends JpaRepository<ActivityRequest, UUID> {

    /**
     * Finds nearby activity requests based on the provided latitude, longitude, radius, and optional sport ID.
     * The query filters requests that are planned, have an event date in the future, and
     * are within the specified radius from the given coordinates. 
     * If a sport ID is provided, it filters requests for that specific sport.
     * The filtered requests are ordered by their distance from the provided coordinates.
     */
    @Query(value = """
            SELECT * FROM activity_requests 
            WHERE status = 'PLANNED' 
              AND event_date > CURRENT_TIMESTAMP 
              AND (CAST(:sportId AS BIGINT) IS NULL OR sport_id = CAST(:sportId AS BIGINT)) 
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
        @Param("latitude") double latitude,
        @Param("longitude") double longitude,
        @Param("radius") double radiusInMeters,
        @Param("sportId") Long sportId
    );
}