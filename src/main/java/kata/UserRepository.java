package kata;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    @Query("SELECT u.* FROM user_table u JOIN user_friends uf ON u.id = uf.friend_id WHERE uf.user_id = :userId")
    List<User> findFriendsByUserId(@Param("userId") Long userId);

    Optional<User> findByUsername(String username);
}
