package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.northerntrust.app.entity.Account;
import project.northerntrust.app.entity.Transfer;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.TransferType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    Optional<Transfer> findByReference(String reference);
    long countByUserAndCreatedAtAfter(User user, LocalDateTime dateTime);
    List<Transfer> findByUserOrderByCreatedAtDesc(User user);
    List<Transfer> findByUserAndPendingApprovalTrueOrderByRiskScoreDesc(User user);
    Optional<Transfer> findByReferenceAndUser(String reference, User user);

    List<Transfer> findByPendingApprovalTrueOrderByRiskScoreDesc();
    List<Transfer> findAllByOrderByCreatedAtDesc();

    long countByFromAccountAndCreatedAtAfterAndTransferTypeIn(
            Account fromAccount, LocalDateTime createdAt, Collection<TransferType> transferTypes);
}
