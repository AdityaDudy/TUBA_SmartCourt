package in.tubalaw.courtos.modules.notifications.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(nullable = false) private String title;
    private String message;
    @Builder.Default private String type = "info";
    private String link;
    @Builder.Default private boolean read = false;
}
