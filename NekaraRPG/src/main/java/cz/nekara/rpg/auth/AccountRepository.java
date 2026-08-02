package cz.nekara.rpg.auth;

import java.io.IOException;
import java.util.Optional;

public interface AccountRepository {
    Optional<AuthAccount> findByUsername(String username);

    boolean create(AuthAccount account) throws IOException;

    void update(AuthAccount account) throws IOException;

    boolean delete(String username) throws IOException;

    int count();
}
