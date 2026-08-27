package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.northerntrust.app.entity.TransferDetail;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferDetailRepository extends JpaRepository<TransferDetail, UUID> {
    Optional<TransferDetail> findByTransfer_Id(UUID id);
}
