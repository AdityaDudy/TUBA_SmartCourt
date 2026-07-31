package in.tubalaw.courtos.modules.tracker.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Lightweight result row returned by the case-number search endpoint.
 * Each row represents a candidate case matched by case-number query against
 * the eCourtsIndia partner search API (GET /api/partner/search).
 *
 * The user selects one row; the chosen {@code cnr} is then fed into the
 * existing {@code GET /api/tracker/search?cnr=…} pipeline.
 */
@Data
@Builder
public class CaseSearchResultDto {

    /** eCourts Case Number Record — used to load full case detail */
    private String cnr;

    /** E.g. "CS(OS)", "WP(C)", "CRL.REV.P." */
    private String caseType;

    /** First petitioner / appellant name(s) */
    private String[] petitioners;

    /** First respondent name(s) */
    private String[] respondents;

    /** Filing date string as returned by vendor (dd-MM-yyyy or similar) */
    private String filingDate;

    /** Human-readable court name, e.g. "Delhi High Court" */
    private String courtName;

    /** eCourtsIndia court code used in the search, e.g. "DLHC01" */
    private String courtCode;
}
