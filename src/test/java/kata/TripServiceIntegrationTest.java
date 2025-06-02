package kata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
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
class TripServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TripService tripService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    private User alice;
    private User bob;
    private User charlie;
    private Trip parisTrip;
    private Trip londonTrip;

    @BeforeEach
    void setUp() {
        // Clear all data using SQL to avoid Spring Data JDBC collection issues
        jdbcTemplate.execute("DELETE FROM user_trip");
        jdbcTemplate.execute("DELETE FROM user_friends");
        jdbcTemplate.execute("DELETE FROM trip");
        jdbcTemplate.execute("DELETE FROM user_table");

        // Create users
        alice = new User();
        alice.setUsername("alice");
        alice = userRepository.save(alice);

        bob = new User();
        bob.setUsername("bob");
        bob = userRepository.save(bob);

        charlie = new User();
        charlie.setUsername("charlie");
        charlie = userRepository.save(charlie);

        // Create trips
        parisTrip = new Trip("Trip to Paris");
        parisTrip = tripRepository.save(parisTrip);

        londonTrip = new Trip("London Business Trip");
        londonTrip = tripRepository.save(londonTrip);
    }

    private void makeFriends(User user1, User user2) {
        // Insert bidirectional friendship
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)",
                user1.getId(), user2.getId());
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)",
                user2.getId(), user1.getId());
    }

    private void assignTripToUser(User user, Trip trip) {
        jdbcTemplate.update("INSERT INTO user_trip (user_id, trip_id) VALUES (?, ?)",
                user.getId(), trip.id());
    }

    @Test
    void should_return_trips_when_users_are_friends() throws UserNotLoggedInException {
        // Given: Alice is logged in and Bob is her friend with trips
        when(authenticationFacade.getCurrentUser())
                .thenReturn(new CurrentUser(alice.getId(), alice.getUsername()));

        // Set up friendship and trips in database
        makeFriends(alice, bob);
        assignTripToUser(bob, parisTrip);
        assignTripToUser(bob, londonTrip);

        // When: Alice requests Bob's trips
        List<Trip> trips = tripService.getTripsByUser(bob);

        // Then: Alice should see Bob's trips
        assertThat(trips).hasSize(2);
        assertThat(trips).extracting(Trip::name)
                .containsExactlyInAnyOrder("Trip to Paris", "London Business Trip");
    }

    @Test
    void should_return_empty_trips_when_users_are_not_friends() throws UserNotLoggedInException {
        // Given: Alice is logged in but Charlie is not her friend
        when(authenticationFacade.getCurrentUser())
                .thenReturn(new CurrentUser(alice.getId(), alice.getUsername()));

        // Charlie has trips but is not Alice's friend
        assignTripToUser(charlie, parisTrip);

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

        // When & Then: Should throw UserNotLoggedInException
        assertThatThrownBy(() -> tripService.getTripsByUser(bob))
                .isInstanceOf(UserNotLoggedInException.class);
    }

    @Test
    void should_throw_exception_when_authenticated_user_not_found_in_database() {
        // Given: Authentication returns a user ID that doesn't exist in database
        when(authenticationFacade.getCurrentUser())
                .thenReturn(new CurrentUser(999L, "nonexistent"));

        // When & Then: Should throw UserNotLoggedInException
        assertThatThrownBy(() -> tripService.getTripsByUser(bob))
                .isInstanceOf(UserNotLoggedInException.class);
    }

    @Test
    void should_return_empty_trips_when_friend_has_no_trips() throws UserNotLoggedInException {
        // Given: Alice is logged in and Bob is her friend but has no trips
        when(authenticationFacade.getCurrentUser())
                .thenReturn(new CurrentUser(alice.getId(), alice.getUsername()));

        // Bob is Alice's friend but has no trips
        makeFriends(alice, bob);

        // When: Alice requests Bob's trips
        List<Trip> trips = tripService.getTripsByUser(bob);

        // Then: Should return empty list
        assertThat(trips).isEmpty();
    }



    @Test
    void should_work_with_complex_friendship_network() throws UserNotLoggedInException {
        // Given: Complex friendship network
        User diana = new User();
        diana.setUsername("diana");
        diana = userRepository.save(diana);

        Trip tokyoTrip = new Trip("Tokyo Adventure");
        tokyoTrip = tripRepository.save(tokyoTrip);

        // Friendship network: Alice -> Bob -> Diana (Alice can't see Diana's trips)
        makeFriends(alice, bob);
        makeFriends(bob, diana);
        
        // Diana has trips but is not directly Alice's friend
        assignTripToUser(diana, tokyoTrip);
        
        when(authenticationFacade.getCurrentUser())
                .thenReturn(new CurrentUser(alice.getId(), alice.getUsername()));

        // When: Alice requests Diana's trips
        List<Trip> trips = tripService.getTripsByUser(diana);

        // Then: Alice should not see Diana's trips (not direct friends)
        assertThat(trips).isEmpty();
    }

    @Test
    void should_load_friends_correctly_from_database() throws UserNotLoggedInException {
        // Given: Complex friendship setup in database
        makeFriends(alice, bob);
        makeFriends(alice, charlie);
        assignTripToUser(bob, parisTrip);
        assignTripToUser(charlie, londonTrip);
        
        when(authenticationFacade.getCurrentUser())
                .thenReturn(new CurrentUser(alice.getId(), alice.getUsername()));

        // When: Alice requests Bob's trips (she should see them)
        List<Trip> bobTrips = tripService.getTripsByUser(bob);
        
        // And: Alice requests Charlie's trips (she should see them too)
        List<Trip> charlieTrips = tripService.getTripsByUser(charlie);

        // Then: Alice should see both friends' trips
        assertThat(bobTrips).hasSize(1);
        assertThat(bobTrips.get(0).name()).isEqualTo("Trip to Paris");
        
        assertThat(charlieTrips).hasSize(1);
        assertThat(charlieTrips.get(0).name()).isEqualTo("London Business Trip");
    }
}
