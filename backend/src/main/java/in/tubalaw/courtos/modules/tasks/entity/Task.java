package in.tubalaw.courtos.modules.tasks.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "matter_id")
    private Long matterId;

    @Column(name = "matter_title")
    private String matterTitle;

    @Column(name = "assigned_to")
    private String assignedTo;

    private String type;

    @Builder.Default private String priority = "Medium";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Builder.Default private boolean done = false;

    @Builder.Default private String status = "Open";

    @Column(name = "created_by")
    private String createdBy;
}
