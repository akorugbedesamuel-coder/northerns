package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.northerntrust.app.entity.TransferStatusLog;

import java.util.UUID;

@Repository
public interface TransferStatusLogRepository extends JpaRepository<TransferStatusLog, UUID> {
}
