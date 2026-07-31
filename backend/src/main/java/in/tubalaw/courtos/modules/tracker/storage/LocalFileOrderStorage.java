package in.tubalaw.courtos.modules.tracker.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.nio.file.*;

@Slf4j
@Component
public class LocalFileOrderStorage implements OrderFileStorage {

    @Value("${tracker.storage.local-path:./data/tracker-orders}")
    private String basePath;

    @Override
    public String store(String tenantId, String cnr, String filename, byte[] bytes) throws Exception {
        Path dir = Paths.get(basePath, tenantId, cnr);
        Files.createDirectories(dir);
        Path file = dir.resolve(filename);
        Files.write(file, bytes);
        log.info("[LocalFileOrderStorage] Stored {} bytes at {}", bytes.length, file.toAbsolutePath());
        // Return a relative key, not an absolute path — keeps it portable
        return tenantId + "/" + cnr + "/" + filename;
    }

    @Override
    public byte[] retrieve(String storageKey) throws Exception {
        Path file = Paths.get(basePath, storageKey);
        if (!Files.exists(file)) {
            throw new FileNotFoundException("Order file not found: " + storageKey);
        }
        return Files.readAllBytes(file);
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(Paths.get(basePath, storageKey));
    }
}
