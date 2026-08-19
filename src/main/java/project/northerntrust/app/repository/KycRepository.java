package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.northerntrust.app.entity.KycProfile;
import project.northerntrust.app.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycRepository extends JpaRepository<KycProfile, UUID> {
    Optional<KycProfile> findByUser(User user);
    Optional<KycProfile> findByBvn(String bvn);
}
