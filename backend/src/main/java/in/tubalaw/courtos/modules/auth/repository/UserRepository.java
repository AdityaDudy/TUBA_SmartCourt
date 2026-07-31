package in.tubalaw.courtos.modules.auth.repository;

import in.tubalaw.courtos.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndTenantId(String email, String tenantId);

    Optional<User> findByEmail(String email);

    List<User> findAllByTenantId(String tenantId);

    List<User> findAllByTenantIdAndDepartment(String tenantId, String department);

    List<User> findAllByRoleAndTenantId(String role, String tenantId);

    boolean existsByEmailAndTenantId(String email, String tenantId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLogin = :now WHERE u.id = :id")
    void updateLastLogin(Long id, Instant now);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.id = :id")
    void incrementFailedAttempts(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedAttempts = 0, u.lockedUntil = null WHERE u.id = :id")
    void resetLockout(Long id);
}
