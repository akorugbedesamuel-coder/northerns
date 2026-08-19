package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.northerntrust.app.entity.OtpCode;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.OtpPurpose;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    Optional<OtpCode> findTopByUserAndPurposeAndUsedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
            User user, OtpPurpose purpose, LocalDateTime now);
}
