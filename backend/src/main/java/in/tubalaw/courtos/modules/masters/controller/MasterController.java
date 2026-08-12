package in.tubalaw.courtos.modules.masters.controller;

import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.masters.service.MasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masters")
@RequiredArgsConstructor
public class MasterController {

    private final MasterService masterService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(masterService.getAll()));
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<List<String>>> getCategory(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.ok(masterService.getCategory(key)));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<List<String>>> update(
            @PathVariable String key,
            @RequestBody Map<String, List<String>> body) {
        return ResponseEntity.ok(ApiResponse.ok(masterService.updateCategory(key, body.get("items")), "Master updated."));
    }

    @PostMapping("/{key}/items")
    public ResponseEntity<ApiResponse<List<String>>> addItem(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(masterService.addItem(key, body.get("item"))));
    }

    @DeleteMapping("/{key}/items/{item}")
    public ResponseEntity<ApiResponse<List<String>>> deleteItem(
            @PathVariable String key, @PathVariable String item) {
        return ResponseEntity.ok(ApiResponse.ok(masterService.deleteItem(key, item)));
    }

    @PutMapping("/{key}/reorder")
    public ResponseEntity<ApiResponse<List<String>>> reorder(
            @PathVariable String key,
            @RequestBody Map<String, List<String>> body) {
        return ResponseEntity.ok(ApiResponse.ok(masterService.reorder(key, body.get("items"))));
    }
}
