package in.tubalaw.courtos.modules.masters.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "masters",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "category"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Master extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 200)
    @Column(columnDefinition = "text[]")
    private String[] items;
}
