package in.tubalaw.courtos.modules.documents.repository;

import in.tubalaw.courtos.modules.documents.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findAllByTenantId(String tenantId);
    List<Document> findAllByTenantIdAndMatterId(String tenantId, Long matterId);
    List<Document> findAllByTenantIdAndClientId(String tenantId, Long clientId);
    List<Document> findAllByTenantIdAndClientIdAndMatterIdIsNull(String tenantId, Long clientId);
    java.util.Optional<Document> findFirstByS3Url(String s3Url);
}
