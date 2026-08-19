package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import project.northerntrust.app.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAccountNumber(String accountNumber);
    Optional<User> findByEmail(String email);
    
    boolean existsByAccountNumber(String accountNumber);
    boolean existsByEmail(String email);
}
