package in.tubalaw.courtos.modules.clients.entity;

import in.tubalaw.courtos.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    @Column(nullable = false)
    private String name;

    @Builder.Default private String type   = "Individual";
    private String mobile;
    private String email;
    private String pan;
    private String gstin;
    private String aadhar;
    private String address;
    private String city;
    private String state;
    private String notes;

    @Builder.Default private String status = "active";

    // Extended client profile fields
    private String displayName;
    private java.time.LocalDate dob;
    private String gender;
    private String fatherSpouseName;
    private String alternateMobile;
    private String billingAddress;
    private String idProofType;
    private String idProofNumber;
    private String assignedAdvocate;
    private java.time.LocalDate clientSince;
    private String referralSource;

    @Builder.Default 
    private boolean vakalatnamaOnFile = false;
    @Builder.Default 
    private boolean engagementLetterSigned = false;
    private String conflictNotes;
    @Builder.Default 
    private boolean dataConsent = false;

    // Corporate Extension fields
    private String cin;
    private String registeredOfficeAddress;
    private String authorizedSignatoryName;
    private String authorizedSignatoryDesignation;
    private java.time.LocalDate incorporationDate;
    private String createdBy;
}
