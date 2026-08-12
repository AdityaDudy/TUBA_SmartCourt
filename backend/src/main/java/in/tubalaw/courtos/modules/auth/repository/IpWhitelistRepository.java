package in.tubalaw.courtos.modules.auth.repository;

import in.tubalaw.courtos.modules.auth.entity.IpWhitelistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IpWhitelistRepository extends JpaRepository<IpWhitelistEntry, Long> {
    List<IpWhitelistEntry> findAllByTenantId(String tenantId);
    void deleteByIpAddressAndTenantId(String ipAddress, String tenantId);
}
