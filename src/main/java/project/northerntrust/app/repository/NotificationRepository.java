package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.northerntrust.app.entity.Notification;
import project.northerntrust.app.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);

    Optional<Notification> findByIdAndUser(UUID id, User user);
}
