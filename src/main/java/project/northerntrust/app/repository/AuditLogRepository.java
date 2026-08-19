package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.northerntrust.app.entity.AuditLog;
import project.northerntrust.app.entity.User;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByUserOrderByCreatedAtDesc(User user);
}
