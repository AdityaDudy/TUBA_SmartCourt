package in.tubalaw.courtos.modules.auth.repository;

import in.tubalaw.courtos.modules.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    List<UserSession> findAllByTenantId(String tenantId);

    List<UserSession> findAllByUserIdAndTenantId(Long userId, String tenantId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.tenantId = :tenantId")
    void deleteAllByTenantId(String tenantId);
}
