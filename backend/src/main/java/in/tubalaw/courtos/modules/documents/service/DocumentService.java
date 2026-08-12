package in.tubalaw.courtos.modules.documents.service;

import in.tubalaw.courtos.modules.documents.entity.Document;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface DocumentService {
    List<Map<String, Object>> getClientFolders();

    List<Map<String, Object>> getMatterFolders();

    List<Map<String, Object>> getRecentDocuments();

    Document uploadDocument(MultipartFile file, Long clientId, Long matterId, String docType, String[] tags);

    List<Map<String, Object>> getClientFolderContents(Long clientId);

    Map<String, Object> storeFileForFiling(MultipartFile file);

    List<Map<String, Object>> getMatterFolderContents(Long matterId);

    Document createMockDocument(String name, Long matterId, String docType);

    void deleteDocument(Long id);
}
