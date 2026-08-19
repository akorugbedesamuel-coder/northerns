package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.northerntrust.app.entity.Account;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.ProductKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
    List<Account> findByUser(User user);
    Optional<Account> findByUserAndProductKey(User user, ProductKey productKey);
}
