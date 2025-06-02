package kata;

public class UserSession {
    
    private static UserSession instance;
    private User loggedUser;
    
    private UserSession() {}
    
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    
    public User getLoggedUser() {
        return loggedUser;
    }
    
    public void setLoggedUser(User user) {
        this.loggedUser = user;
    }
}
