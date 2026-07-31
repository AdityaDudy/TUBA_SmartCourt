package in.tubalaw.courtos.modules.documents.service.impl;

import in.tubalaw.courtos.common.exception.ResourceNotFoundException;
import in.tubalaw.courtos.modules.clients.entity.Client;
import in.tubalaw.courtos.modules.clients.repository.ClientRepository;
import in.tubalaw.courtos.modules.documents.entity.Document;
import in.tubalaw.courtos.modules.documents.repository.DocumentRepository;
import in.tubalaw.courtos.modules.documents.service.DocumentService;
import in.tubalaw.courtos.modules.filings.entity.Filing;
import in.tubalaw.courtos.modules.filings.repository.FilingRepository;
import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository docRepo;
    private final ClientRepository clientRepo;
    private final MatterRepository matterRepo;
    private final FilingRepository filingRepo;

    private static final String TENANT = "default";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    private final String[] backgrounds = { "#dcfce7", "#dbeafe", "#ede9fe", "#fce7f3", "#ccfbf1" };
    private final String[] textColors = { "var(--g1)", "var(--blue)", "var(--purple)", "#be185d", "var(--teal)" };

    @Override
    public List<Map<String, Object>> getClientFolders() {
        in.tubalaw.courtos.common.security.SecurityScopeContext.UserSecurityDetails user =
                in.tubalaw.courtos.common.security.SecurityScopeContext.getCurrentUser();
        in.tubalaw.courtos.common.security.SecurityScopeContext.DataScope scope =
                user != null ? user.getScope() : in.tubalaw.courtos.common.security.SecurityScopeContext.DataScope.OWN;

        List<Client> clients = clientRepo.findAllByTenantId(TENANT);

        // Filter clients based on data scope if OWN or TEAM scope
        if (scope != in.tubalaw.courtos.common.security.SecurityScopeContext.DataScope.ORG) {
            String userName = user != null && user.getEmail() != null ? user.getEmail().split("@")[0].replace(".", " ") : "";
            List<Matter> userMatters = matterRepo.findAllByTenantIdAndAdvocateContaining(TENANT, userName);
            Set<Long> allowedClientIds = userMatters.stream()
                    .map(Matter::getClientId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            clients = clients.stream()
                    .filter(c -> allowedClientIds.contains(c.getId()))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> folders = new ArrayList<>();

        for (Client client : clients) {
            List<Document> directDocs = docRepo.findAllByTenantIdAndClientId(TENANT, client.getId());
            List<Matter> matters = matterRepo.findAllByTenantIdAndClientId(TENANT, client.getId());

            long filingsCount = 0;
            Set<String> filingS3Urls = new HashSet<>();
            Instant lastUpdatedInstant = client.getCreatedAt();

            for (Matter m : matters) {
                List<Filing> filings = filingRepo.findAllByTenantIdAndMatterId(TENANT, m.getId());
                // Only count "Filed" filings — those are the only ones visible in Document
                // Vault
                List<Filing> filedFilings = filings.stream()
                        .filter(f -> "Filed".equalsIgnoreCase(f.getStage()))
                        .collect(Collectors.toList());
                filingsCount += filedFilings.size();
                filedFilings.forEach(f -> {
                    if (f.getS3Url() != null)
                        filingS3Urls.add(f.getS3Url());
                });
                for (Filing f : filings) {
                    if (f.getCreatedAt() != null && f.getCreatedAt().isAfter(lastUpdatedInstant)) {
                        lastUpdatedInstant = f.getCreatedAt();
                    }
                }
            }

            // Exclude Document rows that were auto-created for a Filed filing —
            // they're already counted via filingsCount above.
            long directDocsCount = directDocs.stream()
                    .filter(d -> d.getS3Url() == null || !filingS3Urls.contains(d.getS3Url()))
                    .count();

            for (Document d : directDocs) {
                if (d.getCreatedAt() != null && d.getCreatedAt().isAfter(lastUpdatedInstant)) {
                    lastUpdatedInstant = d.getCreatedAt();
                }
            }

            long totalCount = directDocsCount + filingsCount;
            int idx = (int) (client.getId() % backgrounds.length);

            Map<String, Object> folder = new HashMap<>();
            folder.put("clientId", client.getId());
            folder.put("clientName", client.getName());
            folder.put("count", totalCount);
            folder.put("bg", backgrounds[idx]);
            folder.put("tc", textColors[idx]);
            folder.put("lastUpdated", DATE_FORMATTER.format(lastUpdatedInstant));

            folders.add(folder);
        }

        return folders;
    }

    @Override
    public List<Map<String, Object>> getMatterFolders() {
        in.tubalaw.courtos.common.security.SecurityScopeContext.UserSecurityDetails user =
                in.tubalaw.courtos.common.security.SecurityScopeContext.getCurrentUser();
        in.tubalaw.courtos.common.security.SecurityScopeContext.DataScope scope =
                user != null ? user.getScope() : in.tubalaw.courtos.common.security.SecurityScopeContext.DataScope.OWN;

        List<Matter> matters;
        if (scope == in.tubalaw.courtos.common.security.SecurityScopeContext.DataScope.ORG) {
            matters = matterRepo.findAllByTenantId(TENANT);
        } else {
            String userName = user != null && user.getEmail() != null ? user.getEmail().split("@")[0].replace(".", " ") : "";
            matters = matterRepo.findAllByTenantIdAndAdvocateContaining(TENANT, userName);
        }

        List<Map<String, Object>> folders = new ArrayList<>();

        for (Matter matter : matters) {
            List<Document> directDocs = docRepo.findAllByTenantIdAndMatterId(TENANT, matter.getId());
            List<Filing> filings = filingRepo.findAllByTenantIdAndMatterId(TENANT, matter.getId());

            Instant lastUpdatedInstant = matter.getCreatedAt();
            for (Document d : directDocs) {
                if (d.getCreatedAt() != null && d.getCreatedAt().isAfter(lastUpdatedInstant)) {
                    lastUpdatedInstant = d.getCreatedAt();
                }
            }
            for (Filing f : filings) {
                if (f.getCreatedAt() != null && f.getCreatedAt().isAfter(lastUpdatedInstant)) {
                    lastUpdatedInstant = f.getCreatedAt();
                }
            }

            // Only count Filed filings — they are the only ones shown in Document Vault
            List<Filing> filedFilings = filings.stream()
                    .filter(f -> "Filed".equalsIgnoreCase(f.getStage()))
                    .collect(Collectors.toList());
            long filedFilingsCount = filedFilings.size();
            Set<String> filingS3Urls = filedFilings.stream()
                    .map(Filing::getS3Url)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            // Exclude Document rows auto-created for a Filed filing — already counted
            // above.
            long directDocsCount = directDocs.stream()
                    .filter(d -> d.getS3Url() == null || !filingS3Urls.contains(d.getS3Url()))
                    .count();
            long totalCount = directDocsCount + filedFilingsCount;
            int idx = (int) (matter.getId() % backgrounds.length);

            Map<String, Object> folder = new HashMap<>();
            folder.put("matterId", matter.getId());
            folder.put("matterTitle", matter.getTitle());
            folder.put("clientName", matter.getClientName());
            folder.put("count", totalCount);
            folder.put("bg", backgrounds[idx]);
            folder.put("tc", textColors[idx]);
            folder.put("lastUpdated", DATE_FORMATTER.format(lastUpdatedInstant));

            folders.add(folder);
        }

        return folders;
    }

    @Override
    public List<Map<String, Object>> getRecentDocuments() {
        List<Document> docs = docRepo.findAllByTenantId(TENANT);
        List<Filing> filings = filingRepo.findAllByTenantId(TENANT);

        List<Map<String, Object>> recent = new ArrayList<>();

        for (Document d : docs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("name", d.getName());
            item.put("type", d.getDocType() != null ? d.getDocType() : "Document");
            item.put("source", "Uploaded");
            item.put("matterId", d.getMatterId());
            item.put("clientId", d.getClientId());
            item.put("s3Url", d.getS3Url());
            item.put("date", d.getCreatedAt() != null ? DATE_FORMATTER.format(d.getCreatedAt()) : "—");

            item.put("meta", (d.getClientName() != null ? d.getClientName() : "No Client") + " · Direct Upload");
            item.put("icon", getFileIcon(d.getName()));
            item.put("bg", "#ede9fe");
            item.put("tc", "var(--purple)");
            item.put("tag", "b-a");
            item.put("label", d.getDocType() != null ? d.getDocType() : "Document");
            item.put("createdAt", d.getCreatedAt());
            recent.add(item);
        }

        for (Filing f : filings) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", f.getId());
            item.put("name", filingDisplayName(f));
            item.put("type", f.getFilingType() != null ? f.getFilingType() : "Filing");
            item.put("source", "Filing (" + f.getStage() + ")");
            item.put("matterId", f.getMatterId());
            Long clientId = null;
            if (f.getMatterId() != null) {
                clientId = matterRepo.findById(f.getMatterId()).map(Matter::getClientId).orElse(null);
            }
            item.put("clientId", clientId);
            item.put("date", f.getCreatedAt() != null ? DATE_FORMATTER.format(f.getCreatedAt()) : "—");

            item.put("meta", (f.getMatterTitle() != null ? f.getMatterTitle() : "Filing") + " · "
                    + (f.getAdvocate() != null ? f.getAdvocate() : "System"));
            item.put("icon", "fa-file-import");
            item.put("bg", "#fce7f3");
            item.put("tc", "#be185d");
            item.put("tag", "b-g");
            item.put("label", f.getFilingType() != null ? f.getFilingType() : "Filing");
            item.put("createdAt", f.getCreatedAt());
            if (f.getTitle() != null && f.getTitle().startsWith("Task Submission: ")) {
                String cleanName = f.getTitle().replace("Task Submission: ", "").trim();
                docRepo.findAllByTenantId(TENANT).stream()
                        .filter(doc -> doc.getName() != null && doc.getName().equalsIgnoreCase(cleanName))
                        .findFirst()
                        .ifPresent(doc -> item.put("s3Url", doc.getS3Url()));
            }
            recent.add(item);
        }

        return recent.stream()
                .sorted((a, b) -> ((Instant) b.get("createdAt")).compareTo((Instant) a.get("createdAt")))
                .limit(8)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Document uploadDocument(MultipartFile file, Long clientId, Long matterId, String docType, String[] tags) {
        Long finalClientId = clientId;
        String clientName = null;
        if (matterId != null) {
            Matter m = matterRepo.findById(matterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Matter", matterId));
            finalClientId = m.getClientId();
            clientName = m.getClientName();
        } else if (clientId != null) {
            Client c = clientRepo.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
            clientName = c.getName();
        }

        // Avoid duplicate documents - check if document already exists
        if (matterId != null) {
            List<Document> existingDocs = docRepo.findAllByTenantIdAndMatterId(TENANT, matterId);
            for (Document d : existingDocs) {
                if (d.getName() != null && d.getName().equalsIgnoreCase(file.getOriginalFilename())) {
                    return d;
                }
            }
        } else if (finalClientId != null) {
            List<Document> existingDocs = docRepo.findAllByTenantIdAndClientId(TENANT, finalClientId);
            for (Document d : existingDocs) {
                if (d.getName() != null && d.getName().equalsIgnoreCase(file.getOriginalFilename())
                        && d.getMatterId() == null) {
                    return d;
                }
            }
        }

        // Save file locally to uploads directory
        try {
            Path uploadDir = Paths.get("..").toAbsolutePath().normalize().resolve("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path destination = uploadDir.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
        }

        Document doc = Document.builder()
                .name(file.getOriginalFilename())
                .docType(docType)
                .matterId(matterId)
                .clientId(finalClientId)
                .clientName(clientName)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .s3Url("/uploads/" + file.getOriginalFilename())
                .tags(tags)
                .uploadedBy("Adv. Amit Sharma") // Mocked logged-in user or system
                .build();
        doc.setTenantId(TENANT);

        return docRepo.save(doc);
    }

    @Override
    public Map<String, Object> storeFileForFiling(MultipartFile file) {
        // Save file locally to uploads directory only. Intentionally does NOT
        // create/persist a Document entity, so the file will not appear in the
        // Matter/Client Document Vault until the owning Filing is marked "Filed"
        // (see FilingController#update, which creates the Document row at that point).
        try {
            Path uploadDir = Paths.get("..").toAbsolutePath().normalize().resolve("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path destination = uploadDir.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("name", file.getOriginalFilename());
        result.put("s3Url", "/uploads/" + file.getOriginalFilename());
        result.put("fileSize", file.getSize());
        result.put("mimeType", file.getContentType());
        return result;
    }

    private String filingDisplayName(Filing f) {
        if (f.getTitle() != null && f.getTitle().startsWith("Task Submission:")) {
            String fileName = f.getTitle().replaceFirst("(?i)^Task Submission:\\s*", "").trim();
            String matterTitle = f.getMatterTitle() != null ? f.getMatterTitle() : "Unlinked Matter";
            return matterTitle + " : " + fileName;
        }
        return f.getTitle();
    }

    @Override
    public List<Map<String, Object>> getClientFolderContents(Long clientId) {
        // Only return documents directly linked to this client with NO matter
        // association.
        // Matter-linked docs are accessible by drilling into the matter folder.
        List<Document> directDocs = docRepo.findAllByTenantIdAndClientIdAndMatterIdIsNull(TENANT, clientId);

        List<Map<String, Object>> contents = new ArrayList<>();
        for (Document d : directDocs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("itemType", "document");
            item.put("name", d.getName());
            item.put("type", d.getDocType());
            item.put("source", "Uploaded");
            item.put("matterId", d.getMatterId());
            item.put("s3Url", d.getS3Url());
            item.put("date", DATE_FORMATTER.format(d.getCreatedAt()));
            item.put("tags", d.getTags());
            item.put("icon", getFileIcon(d.getName()));
            contents.add(item);
        }
        return contents;
    }

    @Override
    public List<Map<String, Object>> getMatterFolderContents(Long matterId) {
        // Only show filings that have reached the "Filed" stage in Document Vault.
        // Earlier stages (Draft, Under Review, Signed, etc.) belong to the Filings
        // workflow.
        List<Filing> filings = filingRepo.findAllByTenantIdAndMatterId(TENANT, matterId)
                .stream()
                .filter(f -> "Filed".equalsIgnoreCase(f.getStage()))
                .collect(Collectors.toList());

        // s3Urls already represented by a Filed filing below — exclude their Document
        // rows from directDocs so the same file isn't listed twice.
        Set<String> filingS3Urls = filings.stream()
                .map(Filing::getS3Url)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Document> directDocs = docRepo.findAllByTenantIdAndMatterId(TENANT, matterId)
                .stream()
                .filter(d -> d.getS3Url() == null || !filingS3Urls.contains(d.getS3Url()))
                .collect(Collectors.toList());

        List<Map<String, Object>> contents = new ArrayList<>();

        for (Document d : directDocs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("itemType", "document");
            item.put("name", d.getName());
            item.put("type", d.getDocType());
            item.put("source", "Uploaded");
            item.put("s3Url", d.getS3Url());
            item.put("date", DATE_FORMATTER.format(d.getCreatedAt()));
            item.put("tags", d.getTags());
            item.put("icon", getFileIcon(d.getName()));
            contents.add(item);
        }

        for (Filing f : filings) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", f.getId());
            item.put("itemType", "filing");
            item.put("name", filingDisplayName(f));
            item.put("type", f.getFilingType());
            item.put("source", "Filing (" + f.getStage() + ")");
            item.put("date", DATE_FORMATTER.format(f.getCreatedAt()));
            item.put("tags", new String[] { f.getStatus() });
            item.put("icon", "fa-file-import");

            // Try to resolve the underlying uploaded document for this filing
            if (f.getS3Url() != null) {
                item.put("s3Url", f.getS3Url());
                // Find the document record by matching the s3Url
                docRepo.findAllByTenantId(TENANT).stream()
                        .filter(doc -> f.getS3Url().equals(doc.getS3Url()))
                        .findFirst()
                        .ifPresent(doc -> item.put("docId", doc.getId()));
            } else if (f.getTitle() != null && f.getTitle().startsWith("Task Submission: ")) {
                String cleanName = f.getTitle().replace("Task Submission: ", "").trim();
                docRepo.findAllByTenantId(TENANT).stream()
                        .filter(doc -> doc.getName() != null && doc.getName().equalsIgnoreCase(cleanName))
                        .findFirst()
                        .ifPresent(doc -> {
                            item.put("s3Url", doc.getS3Url());
                            item.put("docId", doc.getId());
                        });
            }
            contents.add(item);
        }
        return contents;
    }

    @Override
    @Transactional
    public Document createMockDocument(String name, Long matterId, String docType) {
        Long clientId = null;
        String clientName = null;
        if (matterId != null) {
            Matter m = matterRepo.findById(matterId)
                    .orElseThrow(() -> new in.tubalaw.courtos.common.exception.ResourceNotFoundException("Matter",
                            matterId));
            clientId = m.getClientId();
            clientName = m.getClientName();
        }

        Document doc = Document.builder()
                .name(name)
                .docType(docType)
                .matterId(matterId)
                .clientId(clientId)
                .clientName(clientName)
                .fileSize(1024L * 150)
                .mimeType("application/pdf")
                .s3Url("https://s3.example.com/" + name)
                .tags(new String[] { "Task Attachment" })
                .uploadedBy("Adv. Amit Sharma")
                .build();
        doc.setTenantId(TENANT);

        return docRepo.save(doc);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        docRepo.deleteById(id);
    }

    private String getFileIcon(String filename) {
        if (filename == null)
            return "fa-file-alt";
        String ext = filename.toLowerCase();
        if (ext.endsWith(".pdf"))
            return "fa-file-pdf";
        if (ext.endsWith(".docx") || ext.endsWith(".doc"))
            return "fa-file-word";
        if (ext.endsWith(".xlsx") || ext.endsWith(".xls"))
            return "fa-file-excel";
        if (ext.endsWith(".png") || ext.endsWith(".jpg") || ext.endsWith(".jpeg"))
            return "fa-file-image";
        return "fa-file-alt";
    }
}
