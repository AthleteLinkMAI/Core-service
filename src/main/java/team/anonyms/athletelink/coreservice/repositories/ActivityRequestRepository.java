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
     * 
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