package kata;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserService userService;

    @Autowired
    public TripService(TripRepository tripRepository, UserService userService) {
        this.tripRepository = tripRepository;
        this.userService = userService;
    }

    public List<Trip> getTripsByUser(User user) throws UserNotLoggedInException {
        List<Trip> tripList = new ArrayList<>();
        User loggedUser = userService.getCurrentUser();
        boolean isFriend = false;
        for (User friend : user.getFriends()) {
            if (loggedUser.getId().equals(friend.getId())) {
                isFriend = true;
                break;
            }
        }
        if (isFriend) {
            tripList = tripRepository.findTripsByUser(user.getId());
        }
        return tripList;
    }

}
