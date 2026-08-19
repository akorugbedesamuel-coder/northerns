package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.northerntrust.app.entity.Beneficiary;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.BeneficiaryType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {
    List<Beneficiary> findByUser(User user);
    List<Beneficiary> findByUserAndBeneficiaryType(User user, BeneficiaryType type);
    Optional<Beneficiary> findByBeneficiaryCode(String beneficiaryCode);
    Optional<Beneficiary> findByBeneficiaryCodeAndUser(String beneficiaryCode, User user);
}
