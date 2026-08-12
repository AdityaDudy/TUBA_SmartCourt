package in.tubalaw.courtos.modules.diary.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "diary_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiaryEvent extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String title;
    @Column(name = "event_date", nullable = false) private LocalDate eventDate;
    @Column(name = "event_time") private String eventTime;
    @Builder.Default private String type = "hearing";
    @Column(name = "matter_id") private Long matterId;
    @Column(name = "matter_title") private String matterTitle;
    @Column(name = "client_id") private Long clientId;
    @Column(name = "client_name") private String clientName;
    @Column(name = "owner_id") private Long ownerId;
    @Column(name = "owner_name") private String ownerName;
    @Column(name = "created_by") private Long createdBy;
    private String court;
    private String notes;
    @Builder.Default private boolean urgent = false;
}
