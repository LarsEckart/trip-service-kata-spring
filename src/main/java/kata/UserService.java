package kata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationFacade authenticationFacade;

    @Autowired
    public UserService(UserRepository userRepository, AuthenticationFacade authenticationFacade) {
        this.userRepository = userRepository;
        this.authenticationFacade = authenticationFacade;
    }

    public User getCurrentUser() throws UserNotLoggedInException {
        try {
            CurrentUser currentUser = authenticationFacade.getCurrentUser();
            return userRepository.findById(currentUser.getId())
                    .orElseThrow(UserNotLoggedInException::new);
        } catch (IllegalStateException e) {
            throw new UserNotLoggedInException();
        }
    }
}
