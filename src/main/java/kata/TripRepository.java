package kata;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends CrudRepository<Trip, Long> {

    @Query("SELECT t.* FROM trip t JOIN user_trip ut ON t.id = ut.trip_id WHERE ut.user_id = :userId")
    List<Trip> findTripsByUser(@Param("userId") Long userId);

}
