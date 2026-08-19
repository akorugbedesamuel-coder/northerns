package project.northerntrust.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.northerntrust.app.entity.RiskEvent;

import java.util.UUID;

@Repository
public interface RiskEventRepository extends JpaRepository<RiskEvent, UUID> {
}
