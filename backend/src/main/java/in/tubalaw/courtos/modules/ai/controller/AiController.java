package in.tubalaw.courtos.modules.ai.controller;

import in.tubalaw.courtos.modules.ai.service.AiService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Data
    public static class ChatRequest {
        private List<Map<String, String>> messages;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> chatSync(@RequestBody ChatRequest body) {
        return ResponseEntity.ok(aiService.chatSync(body.getMessages()));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest body) {
        SseEmitter emitter = new SseEmitter(60000L); // 1 minute timeout
        aiService.streamChat(body.getMessages(), emitter);
        return emitter;
    }
}
