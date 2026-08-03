package in.tubalaw.courtos.modules.matters.repository;

import in.tubalaw.courtos.modules.matters.entity.Matter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MatterRepository extends JpaRepository<Matter, Long> {

       List<Matter> findAllByTenantId(String tenantId);

       List<Matter> findAllByTenantIdAndStatus(String tenantId, String status);

       List<Matter> findAllByTenantIdAndType(String tenantId, String type);

       List<Matter> findAllByTenantIdAndClientId(String tenantId, Long clientId);

       @Query("SELECT m FROM Matter m WHERE m.tenantId = :tenantId " +
                     "AND (LOWER(m.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
                     "OR LOWER(m.clientName) LIKE LOWER(CONCAT('%', :q, '%')) " +
                     "OR LOWER(m.caseNo) LIKE LOWER(CONCAT('%', :q, '%')) " +
                     "OR LOWER(m.cnrNumber) LIKE LOWER(CONCAT('%', :q, '%')))")
       List<Matter> search(String tenantId, String q);

       long countByTenantIdAndStatus(String tenantId, String status);

       @Query("SELECT COUNT(m) FROM Matter m WHERE m.tenantId = :tenantId AND (LOWER(m.status) = 'closed' OR LOWER(m.status) = 'disposed')")
       long countClosedOrDisposed(String tenantId);

       /**
        * Matters with no TrackedCase row linked to them — used by the
        * bulk-link onboarding screen to show which matters still need a CNR.
        */
       @Query("SELECT m FROM Matter m WHERE m.tenantId = :tenantId " +
                     "AND NOT EXISTS (SELECT 1 FROM TrackedCase tc WHERE tc.matterId = m.id AND tc.tenantId = :tenantId)")
       List<Matter> findUnlinked(String tenantId);

       /** Case number lookup for automatic CNR ↔ Matter linking */
       java.util.Optional<Matter> findFirstByTenantIdAndCaseNoIgnoreCase(String tenantId, String caseNo);

       @Query("SELECT m FROM Matter m WHERE m.tenantId = :tenantId AND LOWER(m.caseNo) LIKE LOWER(CONCAT('%', :caseNo, '%'))")
       List<Matter> findByCaseNoLike(String tenantId, String caseNo);

       @Query("SELECT m FROM Matter m WHERE m.tenantId = :tenantId AND (LOWER(m.caseNo) = LOWER(:caseNo) OR LOWER(m.caseNo) LIKE LOWER(CONCAT('%', :caseNo, '%')))")
       List<Matter> findByTenantIdAndCaseNoMatching(String tenantId, String caseNo);

       java.util.Optional<Matter> findFirstByTenantIdAndCnrNumberIgnoreCase(String tenantId, String cnrNumber);

       /**
        * Matters that have a CNR filled in but have never been fetched via the
        * Court Tracker (no TrackedCase row yet) — used by DailyCauseListRefreshJob
        * to bootstrap tracking for every CNR registered against a Matter, not just
        * ones a user happened to search manually first.
        */
       @Query("SELECT m FROM Matter m WHERE m.tenantId = :tenantId " +
                     "AND m.cnrNumber IS NOT NULL AND m.cnrNumber <> '' " +
                     "AND NOT EXISTS (SELECT 1 FROM TrackedCase tc WHERE tc.tenantId = :tenantId " +
                     "AND UPPER(tc.cnr) = UPPER(m.cnrNumber))")
       List<Matter> findWithCnrButNotTracked(String tenantId);

       /**
        * OWN scope: matters where the user is the advocate or co-counsel.
        * Matches by partial name (case-insensitive) because the advocate field
        * stores display names like "Adv. Priya Kapoor".
        */
       @Query("SELECT m FROM Matter m WHERE m.tenantId = :tenantId " +
                     "AND (LOWER(m.advocate) LIKE LOWER(CONCAT('%', :name, '%')) " +
                     "OR LOWER(m.coCounsel) LIKE LOWER(CONCAT('%', :name, '%')))")
       List<Matter> findAllByTenantIdAndAdvocateContaining(String tenantId, String name);

       /** OWN scope with status filter */
       @Query("SELECT m FROM Matter m WHERE m.tenantId = :tenantId AND m.status = :status " +
                     "AND (LOWER(m.advocate) LIKE LOWER(CONCAT('%', :name, '%')) " +
                     "OR LOWER(m.coCounsel) LIKE LOWER(CONCAT('%', :name, '%')))")
       List<Matter> findAllByTenantIdAndStatusAndAdvocateContaining(String tenantId, String status, String name);

       /** OWN scope with type filter */
       @Query("SELECT m FROM Matter m WHERE m.tenantId = :tenantId AND m.type = :type " +
                     "AND (LOWER(m.advocate) LIKE LOWER(CONCAT('%', :name, '%')) " +
                     "OR LOWER(m.coCounsel) LIKE LOWER(CONCAT('%', :name, '%')))")
       List<Matter> findAllByTenantIdAndTypeAndAdvocateContaining(String tenantId, String type, String name);
}