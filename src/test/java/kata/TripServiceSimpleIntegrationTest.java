package kata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SpringBootTest
@Testcontainers
class TripServiceSimpleIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TripService tripService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    private Long aliceId;
    private Long bobId;
    private Long charlieId;
    private Long parisTrip;

    @BeforeEach
    void setUp() {
        // Clear all data using SQL 
        jdbcTemplate.execute("DELETE FROM user_trip");
        jdbcTemplate.execute("DELETE FROM user_friends");
        jdbcTemplate.execute("DELETE FROM trip");
        jdbcTemplate.execute("DELETE FROM user_table");

        // Create users directly in database
        aliceId = jdbcTemplate.queryForObject(
                "INSERT INTO user_table (username) VALUES ('alice') RETURNING id", Long.class);
        bobId = jdbcTemplate.queryForObject(
                "INSERT INTO user_table (username) VALUES ('bob') RETURNING id", Long.class);
        charlieId = jdbcTemplate.queryForObject(
                "INSERT INTO user_table (username) VALUES ('charlie') RETURNING id", Long.class);

        // Create trips directly in database
        parisTrip = jdbcTemplate.queryForObject(
                "INSERT INTO trip (name) VALUES ('Trip to Paris') RETURNING id", Long.class);
    }

    @Test
    void should_return_trips_when_users_are_friends() throws UserNotLoggedInException {
        // Given: Mock authentication to return Alice's ID
        when(authenticationFacade.getCurrentUser())
                .thenReturn(new CurrentUser(aliceId, "alice"));

        // Set up friendship: Alice and Bob are friends
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)", aliceId, bobId);
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)", bobId, aliceId);

        // Bob has the Paris trip
        jdbcTemplate.update("INSERT INTO user_trip (user_id, trip_id) VALUES (?, ?)", bobId, parisTrip);

        // Create a User object for Bob (required by TripService interface)
        User bob = new User();
        bob.setId(bobId);
        bob.setUsername("bob");

        // When: Alice requests Bob's trips
        List<Trip> trips = tripService.getTripsByUser(bob);

        // Then: Alice should see Bob's trips
        assertThat(trips).hasSize(1);
        assertThat(trips.get(0).name()).isEqualTo("Trip to Paris");
    }

    @Test
    void should_return_empty_trips_when_users_are_not_friends() throws UserNotLoggedInException {
        // Given: Mock authentication to return Alice's ID
        when(authenticationFacade.getCurrentUser())
                .thenReturn(new CurrentUser(aliceId, "alice"));

        // Charlie has the Paris trip but is not Alice's friend
        jdbcTemplate.update("INSERT INTO user_trip (user_id, trip_id) VALUES (?, ?)", charlieId, parisTrip);

        // Create a User object for Charlie
        User charlie = new User();
        charlie.setId(charlieId);
        charlie.setUsername("charlie");

        // When: Alice requests Charlie's trips
        List<Trip> trips = tripService.getTripsByUser(charlie);

        // Then: Alice should see no trips (empty list)
        assertThat(trips).isEmpty();
    }

    @Test
    void should_throw_exception_when_user_not_authenticated() {
        // Given: No user is authenticated
        when(authenticationFacade.getCurrentUser())
                .thenThrow(new IllegalStateException("No authenticated user"));

        User someUser = new User();
        someUser.setId(bobId);

        // When & Then: Should throw UserNotLoggedInException
        assertThatThrownBy(() -> tripService.getTripsByUser(someUser))
                .isInstanceOf(UserNotLoggedInException.class);
    }

    @Test
    void should_throw_exception_when_authenticated_user_not_found_in_database() {
        // Given: Authentication returns a user ID that doesn't exist in database
        when(authenticationFacade.getCurrentUser())
                .thenReturn(new CurrentUser(999L, "nonexistent"));

        User someUser = new User();
        someUser.setId(bobId);

        // When & Then: Should throw UserNotLoggedInException
        assertThatThrownBy(() -> tripService.getTripsByUser(someUser))
                .isInstanceOf(UserNotLoggedInException.class);
    }

    @Test
    void should_load_friendships_from_database_correctly() throws UserNotLoggedInException {
        // Given: Complex friendship network in database
        // Alice is friends with Bob, Bob is friends with Charlie, but Alice is NOT friends with Charlie
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)", aliceId, bobId);
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)", bobId, aliceId);
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)", bobId, charlieId);
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)", charlieId, bobId);

        Long tokyoTrip = jdbcTemplate.queryForObject(
                "INSERT INTO trip (name) VALUES ('Tokyo Adventure') RETURNING id", Long.class);

        // Bob has Paris trip, Charlie has Tokyo trip
        jdbcTemplate.update("INSERT INTO user_trip (user_id, trip_id) VALUES (?, ?)", bobId, parisTrip);
        jdbcTemplate.update("INSERT INTO user_trip (user_id, trip_id) VALUES (?, ?)", charlieId, tokyoTrip);

        when(authenticationFacade.getCurrentUser())
                .thenReturn(new CurrentUser(aliceId, "alice"));

        // Create User objects
        User bob = new User();
        bob.setId(bobId);
        bob.setUsername("bob");

        User charlie = new User();
        charlie.setId(charlieId);
        charlie.setUsername("charlie");

        // When: Alice requests trips
        List<Trip> bobTrips = tripService.getTripsByUser(bob);
        List<Trip> charlieTrips = tripService.getTripsByUser(charlie);

        // Then: Alice can see Bob's trips (they're friends) but not Charlie's (not friends)
        assertThat(bobTrips).hasSize(1);
        assertThat(bobTrips.get(0).name()).isEqualTo("Trip to Paris");

        assertThat(charlieTrips).isEmpty(); // Alice and Charlie are not direct friends
    }
}
