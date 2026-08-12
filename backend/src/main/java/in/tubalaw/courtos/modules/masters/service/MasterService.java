package in.tubalaw.courtos.modules.masters.service;

import in.tubalaw.courtos.modules.masters.entity.Master;
import in.tubalaw.courtos.modules.masters.repository.MasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MasterService {

    private final MasterRepository repo;
    private static final String TENANT = "default";

    /** Returns all categories as a Map<category, items[]> */
    public Map<String, List<String>> getAll() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        repo.findAllByTenantId(TENANT)
                .forEach(m -> map.put(m.getCategory(), m.getItems() != null ? Arrays.asList(m.getItems()) : List.of()));
        return map;
    }

    public List<String> getCategory(String category) {
        return repo.findByTenantIdAndCategory(TENANT, category)
                .map(m -> m.getItems() != null ? Arrays.asList(m.getItems()) : List.<String>of())
                .orElse(List.of());
    }

    @Transactional
    public List<String> updateCategory(String category, List<String> items) {
        Master master = repo.findByTenantIdAndCategory(TENANT, category)
                .orElseGet(() -> {
                    Master m = new Master();
                    m.setTenantId(TENANT);
                    m.setCategory(category);
                    return m;
                });
        master.setItems(items.toArray(new String[0]));
        return Arrays.asList(repo.save(master).getItems());
    }

    @Transactional
    public List<String> addItem(String category, String item) {
        List<String> current = new ArrayList<>(getCategory(category));
        if (!current.contains(item))
            current.add(item);
        return updateCategory(category, current);
    }

    @Transactional
    public List<String> deleteItem(String category, String item) {
        List<String> current = new ArrayList<>(getCategory(category));
        current.remove(item);
        return updateCategory(category, current);
    }

    @Transactional
    public List<String> reorder(String category, List<String> items) {
        return updateCategory(category, items);
    }
}
