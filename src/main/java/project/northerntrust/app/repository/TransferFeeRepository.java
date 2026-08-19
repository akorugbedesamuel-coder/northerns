package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.northerntrust.app.entity.TransferFee;

import java.util.UUID;

@Repository
public interface TransferFeeRepository extends JpaRepository<TransferFee, UUID> {
}
