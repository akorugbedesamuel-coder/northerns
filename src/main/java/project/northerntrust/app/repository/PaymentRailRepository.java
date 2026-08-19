package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.northerntrust.app.entity.PaymentRail;
import project.northerntrust.app.entity.enums.PaymentRailType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRailRepository extends JpaRepository<PaymentRail, UUID> {
    Optional<PaymentRail> findByRailType(PaymentRailType railType);
}
