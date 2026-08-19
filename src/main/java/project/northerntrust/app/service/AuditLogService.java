package project.northerntrust.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.northerntrust.app.entity.AuditLog;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.repository.AuditLogRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public AuditLog record(User user, String action, String entityType, UUID entityId, Map<String, ?> details) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        if (details != null && !details.isEmpty()) {
            try {
                log.setDetails(objectMapper.writeValueAsString(details));
            } catch (JsonProcessingException e) {
                log.setDetails(details.toString());
            }
        }
        return auditLogRepository.save(log);
    }

    public Map<String, ?> detailsOf(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
