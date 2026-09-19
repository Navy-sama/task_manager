package io.github.navysama.taskmanager.auth.internal.repository;

import io.github.navysama.taskmanager.auth.internal.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
