package in.tubalaw.courtos.common.config;

import in.tubalaw.courtos.modules.auth.entity.User;
import in.tubalaw.courtos.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.email:admin@tubalaw.com}")
    private String seedEmail;

    @Value("${admin.seed.password:Admin@123}")
    private String seedPassword;

    @Value("${admin.seed.name:Admin User}")
    private String seedName;

    private static final String DEFAULT_TENANT = "default";

    private static final String[] ALL_ADMIN_PERMISSIONS = new String[] {
            "view_all",
            "create_matters",
            "edit_matters",
            "delete_matters",
            "view_docs",
            "upload_docs",
            "delete_docs",
            "manage_tasks",
            "manage_tasks_assign",
            "manage_tasks_close",
            "view_billing",
            "view_own_billing",
            "create_invoices",
            "export_billing",
            "export_data",
            "manage_clients",
            "delete_clients",
            "manage_users",
            "manage_roles",
            "impersonate_user",
            "system_settings",
            "view_audit",
            "view_own_audit",
            "scope_org"
    };

    @Override
    public void run(String... args) throws Exception {
        userRepo.findByEmailAndTenantId(seedEmail, DEFAULT_TENANT).ifPresentOrElse(
                user -> {
                    // Always ensure password, permissions, and active status are synced for admin
                    boolean updated = false;
                    if (!"admin".equalsIgnoreCase(user.getRole())) {
                        user.setRole("admin");
                        updated = true;
                    }
                    if (!"active".equalsIgnoreCase(user.getStatus())) {
                        user.setStatus("active");
                        updated = true;
                    }
                    // Sync password hash if changed
                    if (!passwordEncoder.matches(seedPassword, user.getPasswordHash())) {
                        user.setPasswordHash(passwordEncoder.encode(seedPassword));
                        updated = true;
                    }
                    // Sync full admin permissions
                    user.setPermissions(ALL_ADMIN_PERMISSIONS);
                    updated = true;

                    if (updated) {
                        userRepo.save(user);
                        log.info("Admin user [{}] updated and synced with full permissions on startup.", seedEmail);
                    }
                },
                () -> {
                    log.info("Admin user [{}] not found. Creating admin user with full permissions on startup...",
                            seedEmail);
                    User adminUser = new User();
                    adminUser.setTenantId(DEFAULT_TENANT);
                    adminUser.setName(seedName);
                    adminUser.setEmail(seedEmail);
                    adminUser.setPasswordHash(passwordEncoder.encode(seedPassword));
                    adminUser.setRole("admin");
                    adminUser.setDepartment("Administration");
                    adminUser.setDesignation("Senior Partner & Administrator");
                    adminUser.setInitials("AD");
                    adminUser.setGradient("linear-gradient(135deg,#b45309,#d97706)");
                    adminUser.setStatus("active");
                    adminUser.setPermissions(ALL_ADMIN_PERMISSIONS);
                    adminUser.setMfaEnabled(false);
                    adminUser.setFailedAttempts(0);

                    userRepo.save(adminUser);
                    log.info("Admin user [{}] successfully created and seeded on startup.", seedEmail);
                });
    }
}
