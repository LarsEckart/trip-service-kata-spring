package kata;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Autowired
    public TripService(TripRepository tripRepository, UserRepository userRepository, UserService userService) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public List<Trip> getTripsByUser(User user) throws UserNotLoggedInException {
        List<Trip> tripList = new ArrayList<>();
        User loggedUser = userService.getCurrentUser();
        
        List<User> friends = userRepository.findFriendsByUserId(loggedUser.getId());
        boolean isFriend = friends.stream()
                .anyMatch(friend -> friend.getId().equals(user.getId()));
        
        if (isFriend) {
            tripList = tripRepository.findTripsByUser(user.getId());
        }
        return tripList;
    }

}
