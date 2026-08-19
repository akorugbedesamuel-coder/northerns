package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.northerntrust.app.entity.StatementLine;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.ProductKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatementLineRepository extends JpaRepository<StatementLine, UUID> {
    List<StatementLine> findByUserOrderByLineDateDesc(User user);
    List<StatementLine> findByUserAndProductKeyOrderByLineDateDesc(User user, ProductKey productKey);
    Optional<StatementLine> findFirstByUserAndTransferReferenceAndLineTypeOrderByLineDateDesc(
            User user, String transferReference, String lineType);
}
