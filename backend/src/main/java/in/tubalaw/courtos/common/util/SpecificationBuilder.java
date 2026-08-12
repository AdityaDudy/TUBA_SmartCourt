package in.tubalaw.courtos.common.util;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Objects;

/**
 * Generic Specification factory.
 * All methods are null-safe: passing a null or blank value to a combinator
 * should be guarded by the caller (the service layer), not here.
 */
public final class SpecificationBuilder {

    private SpecificationBuilder() {}

    /** tenant_id = :tenantId */
    public static <T> Specification<T> tenantEq(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    /** field = :value (exact, case-sensitive — suits enum-like columns) */
    public static <T> Specification<T> fieldEq(String field, String value) {
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    /** field = :value for Long columns (e.g. clientId, matterId) */
    public static <T> Specification<T> fieldEqLong(String field, Long value) {
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    /** LOWER(field) LIKE LOWER('%:value%') */
    public static <T> Specification<T> fieldContains(String field, String value) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%");
    }

    /**
     * OR across multiple fields: LOWER(f1) LIKE '%q%' OR LOWER(f2) LIKE '%q%' ...
     * Useful for free-text search boxes.
     */
    public static <T> Specification<T> multiFieldSearch(String query, String... fields) {
        return (root, cq, cb) -> {
            String pattern = "%" + query.toLowerCase() + "%";
            Predicate[] predicates = Arrays.stream(fields)
                    .map(f -> cb.like(cb.lower(root.get(f)), pattern))
                    .toArray(Predicate[]::new);
            return cb.or(predicates);
        };
    }

    /**
     * Null-safe AND combinator. Any null spec in the varargs is silently skipped.
     * An empty list resolves to a conjunction (no restriction).
     */
    @SafeVarargs
    public static <T> Specification<T> and(Specification<T>... specs) {
        return Arrays.stream(specs)
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }
}
