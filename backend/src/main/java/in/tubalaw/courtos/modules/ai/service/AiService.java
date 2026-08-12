package in.tubalaw.courtos.modules.ai.service;

import in.tubalaw.courtos.modules.clients.entity.Client;
import in.tubalaw.courtos.modules.clients.repository.ClientRepository;
import in.tubalaw.courtos.modules.matters.entity.Matter;
import in.tubalaw.courtos.modules.matters.repository.MatterRepository;
import in.tubalaw.courtos.modules.causelist.entity.Hearing;
import in.tubalaw.courtos.modules.causelist.repository.HearingRepository;
import in.tubalaw.courtos.modules.tasks.entity.Task;
import in.tubalaw.courtos.modules.tasks.repository.TaskRepository;
import in.tubalaw.courtos.modules.filings.entity.Filing;
import in.tubalaw.courtos.modules.filings.repository.FilingRepository;
import in.tubalaw.courtos.modules.billing.entity.Invoice;
import in.tubalaw.courtos.modules.billing.repository.InvoiceRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final MatterRepository matterRepo;
    private final ClientRepository clientRepo;
    private final FilingRepository filingRepo;
    private final HearingRepository hearingRepo;
    private final TaskRepository taskRepo;
    private final InvoiceRepository invoiceRepo;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${openrouter.api-key:${OPENROUTER_API_KEY:}}")
    private String apiKey;

    @org.springframework.beans.factory.annotation.Value("${openrouter.model:google/gemini-2.5-flash}")
    private String model;

    @org.springframework.beans.factory.annotation.Value("${openrouter.url:https://openrouter.ai/api/v1/chat/completions}")
    private String openrouterUrl;

    private static final String TENANT = "default";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String buildSystemPrompt() {
        List<Client> clients = clientRepo.findAllByTenantId(TENANT);
        List<Matter> matters = matterRepo.findAllByTenantId(TENANT);
        List<Hearing> hearings = hearingRepo.findAllByTenantId(TENANT);
        List<Task> tasks = taskRepo.findAllByTenantId(TENANT);
        List<Filing> filings = filingRepo.findAllByTenantId(TENANT);
        List<Invoice> invoices = invoiceRepo.findAllByTenantId(TENANT);

        StringBuilder sb = new StringBuilder();
        sb.append(
                "You are TUBA Legal AI — an intelligent legal assistant for CourtOS, India's smart court management platform.\n\n");
        sb.append(
                "You respond to queries using actual data present in the database. Below is the complete current state of the firm's database:\n\n");

        sb.append("### CLIENTS REGISTRY\n");
        for (Client c : clients) {
            sb.append(String.format("- ID: %d, Code: %s, Name: %s, Type: %s, Mobile: %s, Email: %s, Status: %s\n",
                    c.getId(), c.getCode(), c.getName(), c.getType(), c.getMobile(), c.getEmail(), c.getStatus()));
        }
        sb.append("\n");

        sb.append("### ACTIVE MATTERS (CASES)\n");
        for (Matter m : matters) {
            sb.append(String.format(
                    "- ID: %d, Title: %s, Case No: %s, Client Name: %s, Court: %s, Type: %s, Status: %s, Stage: %s\n",
                    m.getId(), m.getTitle(), m.getCaseNo(), m.getClientName(), m.getCourt(), m.getType(), m.getStatus(),
                    m.getStage()));
        }
        sb.append("\n");

        sb.append("### CAUSE LIST (HEARINGS SCHEDULE)\n");
        for (Hearing h : hearings) {
            sb.append(String.format("- ID: %d, Title: %s, Court: %s, Date: %s, Time: %s, Status: %s, Advocate: %s\n",
                    h.getId(), h.getCaseTitle(), h.getCourt(), h.getHearingDate(), h.getHearingTime(), h.getStatus(),
                    h.getAdvocate()));
        }
        sb.append("\n");

        sb.append("### FIRM TASKS\n");
        for (Task t : tasks) {
            sb.append(String.format(
                    "- ID: %d, Title: %s, Matter: %s, Assigned To: %s, Type: %s, Priority: %s, Due: %s, Status: %s, Done: %b\n",
                    t.getId(), t.getTitle(), t.getMatterTitle(), t.getAssignedTo(), t.getType(), t.getPriority(),
                    t.getDueDate(), t.getStatus(), t.isDone()));
        }
        sb.append("\n");

        sb.append("### FILINGS MONITOR\n");
        for (Filing f : filings) {
            sb.append(String.format(
                    "- ID: %d, Title: %s, Matter: %s, Court: %s, Type: %s, Stage: %s, Due: %s, Filed: %s\n",
                    f.getId(), f.getTitle(), f.getMatterTitle(), f.getCourt(), f.getFilingType(), f.getStage(),
                    f.getDueDate(), f.getFiledDate()));
        }
        sb.append("\n");

        sb.append("### BILLING & INVOICES\n");
        for (Invoice i : invoices) {
            sb.append(String.format("- Invoice No: %s, Client: %s, Amount: %.2f, Due: %s, Status: %s\n",
                    i.getInvoiceNo(), i.getClientName(), i.getAmount(), i.getDueDate(), i.getStatus()));
        }
        sb.append("\n");

        sb.append("Guidelines:\n");
        sb.append("1. Answer users accurately based ONLY on the database state above.\n");
        sb.append(
                "2. If asked about a client, matter, or hearing, cross-reference the ID or details from the registries and provide correct facts.\n");
        sb.append("3. Keep responses conversational, concise, professional, and formatted in clean Markdown.\n");

        return sb.toString();
    }

    public void streamChat(List<Map<String, String>> messages, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                // Construct messages payload for OpenRouter
                List<Map<String, String>> payloadMessages = new ArrayList<>();
                payloadMessages.add(Map.of("role", "system", "content", buildSystemPrompt()));

                // Add actual conversation history (skip system message sent by frontend to
                // avoid conflict)
                for (Map<String, String> msg : messages) {
                    if (!"system".equalsIgnoreCase(msg.get("role"))) {
                        payloadMessages.add(msg);
                    }
                }

                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("messages", payloadMessages);
                body.put("stream", true);
                body.put("max_tokens", 8000);

                String jsonBody = objectMapper.writeValueAsString(body);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(openrouterUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("HTTP-Referer", "http://localhost:8084")
                        .header("X-Title", "TUBA Smart Court")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                log.info("Sending request to OpenRouter URL: {}", openrouterUrl);
                HttpResponse<InputStream> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                log.info("OpenRouter response status code: {}", response.statusCode());
                if (response.statusCode() != 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                        StringBuilder err = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            err.append(line).append("\n");
                        }
                        log.error("OpenRouter Error response: {}", err.toString());
                    }
                    emitter.send(Map.of("choices", List.of(
                            Map.of("delta", Map.of("content", "Error from OpenRouter: " + response.statusCode())))));
                    emitter.complete();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) {
                                break;
                            }

                            try {
                                Map<?, ?> parsed = objectMapper.readValue(data, Map.class);
                                List<?> choices = (List<?>) parsed.get("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                                    Map<?, ?> delta = (Map<?, ?>) choice.get("delta");
                                    if (delta != null && delta.containsKey("content")) {
                                        String content = (String) delta.get("content");
                                        Map<String, Object> chunk = Map.of(
                                                "choices", List.of(
                                                        Map.of("delta", Map.of("content", content))));
                                        emitter.send(chunk);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
                emitter.complete();

            } catch (Exception e) {
                log.error("Error in AI stream", e);
                try {
                    emitter.send("Error processing request: " + e.getMessage());
                } catch (Exception ignored) {
                }
                emitter.complete();
            }
        });
    }

    public Map<String, Object> chatSync(List<Map<String, String>> messages) {
        try {
            List<Map<String, String>> payloadMessages = new ArrayList<>();
            payloadMessages.add(Map.of("role", "system", "content", buildSystemPrompt()));
            for (Map<String, String> msg : messages) {
                if (!"system".equalsIgnoreCase(msg.get("role"))) {
                    payloadMessages.add(msg);
                }
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", payloadMessages);
            body.put("stream", false);
            body.put("max_tokens", 8000);

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openrouterUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return Map.of("content", "Error from OpenRouter: " + response.statusCode());
            }

            Map<?, ?> parsed = objectMapper.readValue(response.body(), Map.class);
            List<?> choices = (List<?>) parsed.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) choice.get("message");
                if (message != null && message.containsKey("content")) {
                    return Map.of("content", message.get("content"));
                }
            }
            return Map.of("content", "No response content received.");

        } catch (Exception e) {
            log.error("Error in AI sync chat", e);
            return Map.of("content", "Error: " + e.getMessage());
        }
    }
}
