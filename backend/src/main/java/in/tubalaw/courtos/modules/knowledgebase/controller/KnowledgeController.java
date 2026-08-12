package in.tubalaw.courtos.modules.knowledgebase.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.*;
import in.tubalaw.courtos.common.audit.BaseEntity;
import lombok.*;

import java.util.List;
import java.util.Map;

@Entity @Table(name = "knowledge_items")
class KnowledgeItem extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    String category;
    String title;
    String court;
    String citation;
    Integer year;
    String author;
    String summary;
    String content;
    @Column(name = "doc_type") String docType;
}

@Repository
interface KnowledgeRepository extends JpaRepository<KnowledgeItem, Long> {
    List<KnowledgeItem> findAllByTenantIdAndCategory(String tenantId, String category);
    List<KnowledgeItem> findAllByTenantId(String tenantId);
}

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeRepository repo;
    private static final String TENANT = "default";

    @GetMapping("/judgments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> judgments() {
        var items = repo.findAllByTenantIdAndCategory(TENANT, "judgment");
        var list = items.stream().<Map<String, Object>>map(i ->
            Map.of("title",i.title,"court",i.court != null?i.court:"","tag","b-b","label",i.citation != null?i.citation:"")
        ).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> templates() {
        var items = repo.findAllByTenantIdAndCategory(TENANT, "template");
        var list = items.stream().<Map<String, Object>>map(i ->
            Map.of("name",i.title,"type",i.docType != null?i.docType:"General","updatedAt","2026-07-01")
        ).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> articles() {
        var items = repo.findAllByTenantIdAndCategory(TENANT, "article");
        var list = items.stream().<Map<String, Object>>map(i ->
            Map.of("title",i.title,"author",i.author != null?i.author:"Editorial","date","2026-01-01","summary",i.summary != null?i.summary:"")
        ).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<KnowledgeItem>>> search(@RequestParam String q) {
        var all = repo.findAllByTenantId(TENANT);
        var filtered = all.stream()
            .filter(i -> i.title != null && i.title.toLowerCase().contains(q.toLowerCase()))
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(filtered));
    }
}
