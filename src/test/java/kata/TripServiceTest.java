package kata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserService userService;

    @InjectMocks
    private TripService tripService;

    private User loggedInUser;
    private User targetUser;
    private User friendUser;
    private Trip trip1;
    private Trip trip2;

    @BeforeEach
    void setUp() {
        loggedInUser = new User();
        loggedInUser.setId(1L);
        loggedInUser.setUsername("loggedUser");

        targetUser = new User();
        targetUser.setId(2L);
        targetUser.setUsername("targetUser");

        friendUser = new User();
        friendUser.setId(3L);
        friendUser.setUsername("friendUser");

        trip1 = new Trip(1L, "Trip to Paris");
        trip2 = new Trip(2L, "Trip to London");
    }

    @Test
    void getTripsByUser_whenUsersAreFriends_shouldReturnTrips() throws UserNotLoggedInException {
        when(userService.getCurrentUser()).thenReturn(loggedInUser);
        when(userRepository.findFriendsByUserId(loggedInUser.getId()))
                .thenReturn(Arrays.asList(targetUser, friendUser));
        when(tripRepository.findTripsByUser(targetUser.getId()))
                .thenReturn(Arrays.asList(trip1, trip2));

        List<Trip> result = tripService.getTripsByUser(targetUser);

        assertThat(result).containsExactly(trip1, trip2);
    }

    @Test
    void getTripsByUser_whenUsersAreNotFriends_shouldReturnEmptyList() throws UserNotLoggedInException {
        when(userService.getCurrentUser()).thenReturn(loggedInUser);
        when(userRepository.findFriendsByUserId(loggedInUser.getId()))
                .thenReturn(Arrays.asList(friendUser));

        List<Trip> result = tripService.getTripsByUser(targetUser);

        assertThat(result).isEmpty();
    }

    @Test
    void getTripsByUser_whenUserHasNoFriends_shouldReturnEmptyList() throws UserNotLoggedInException {
        when(userService.getCurrentUser()).thenReturn(loggedInUser);
        when(userRepository.findFriendsByUserId(loggedInUser.getId()))
                .thenReturn(Arrays.asList());

        List<Trip> result = tripService.getTripsByUser(targetUser);

        assertThat(result).isEmpty();
    }

    @Test
    void getTripsByUser_whenFriendHasNoTrips_shouldReturnEmptyList() throws UserNotLoggedInException {
        when(userService.getCurrentUser()).thenReturn(loggedInUser);
        when(userRepository.findFriendsByUserId(loggedInUser.getId()))
                .thenReturn(Arrays.asList(targetUser));
        when(tripRepository.findTripsByUser(targetUser.getId()))
                .thenReturn(Arrays.asList());

        List<Trip> result = tripService.getTripsByUser(targetUser);

        assertThat(result).isEmpty();
    }

    @Test
    void getTripsByUser_whenRequestingOwnTrips_shouldReturnTrips() throws UserNotLoggedInException {
        when(userService.getCurrentUser()).thenReturn(loggedInUser);
        when(userRepository.findFriendsByUserId(loggedInUser.getId()))
                .thenReturn(Arrays.asList(loggedInUser));
        when(tripRepository.findTripsByUser(loggedInUser.getId()))
                .thenReturn(Arrays.asList(trip1));

        List<Trip> result = tripService.getTripsByUser(loggedInUser);

        assertThat(result).containsExactly(trip1);
    }
}