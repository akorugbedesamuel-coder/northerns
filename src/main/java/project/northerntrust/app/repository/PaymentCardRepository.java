package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.northerntrust.app.entity.PaymentCard;
import project.northerntrust.app.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, UUID> {
    Optional<PaymentCard> findByUser(User user);
}
