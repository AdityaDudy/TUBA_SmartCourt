package in.tubalaw.courtos.modules.auth.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users",
       uniqueConstraints = @UniqueConstraint(columnNames = {"email", "tenant_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private String role = "advocate";

    private String department;
    private String designation;
    private String mobile;

    @Column(name = "bar_council_no")
    private String barCouncilNo;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "mfa_enabled")
    @Builder.Default
    private boolean mfaEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private String status = "active";

    private String initials;
    private String gradient;

    @Column(name = "theme")
    private String theme;

    @Column(name = "avatar", columnDefinition = "TEXT")
    private String avatar;

    /**
     * Stored as PostgreSQL text[] but mapped as comma-separated string
     * to avoid Hibernate 6 @Array annotation complexity.
     * Use getPermissionList() / setPermissionList() helpers.
     */
    @Column(name = "permissions", columnDefinition = "text[]")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @org.hibernate.annotations.Array(length = 50)
    private String[] permissions;

    @Column(name = "failed_attempts")
    @Builder.Default
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login")
    private Instant lastLogin;
}
