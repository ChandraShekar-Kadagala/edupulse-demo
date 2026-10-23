package com.edupulse.service.provider.contract;

import com.edupulse.model.DataProvenance;

import java.util.List;
import java.util.Map;

/**
 * Architectural specification and contract for future officially authorized
 * SRM University AP Student Portal / Academic Data API integrations.
 * 
 * DESIGN PRINCIPLES:
 * 1. Zero-Scraping: No scraping, no CAPTCHA defeating, no portal cookie theft.
 * 2. Authorized Credentials: OAuth2 client credentials or institutional API tokens only.
 * 3. Explicit Provenance: All records ingested through this contract are tagged with
 *    DataProvenance.OFFICIAL_INSTITUTIONAL_API.
 * 4. Safe Defaults: Remains unconfigured until institutional IT issues an approved endpoint & token.
 */
public interface SrmApiContract {

    /**
     * Reports whether the official contract has been configured with verified
     * university credentials and approved institutional agreements.
     */
    boolean isConfigured();

    /**
     * Expected authentication mechanism (e.g. OAUTH2_BEARER, MTLS_API_KEY).
     */
    String getAuthenticationMechanism();

    /**
     * Institutional base URL for academic queries. Returns null if unconfigured.
     */
    String getBaseUrl();

    /**
     * Synchronizes official academic records for an authorized student registration number.
     * 
     * @param srmRegistrationNumber Canonical student registration number (e.g. AP25110070436)
     * @throws IllegalStateException if unconfigured or unauthorized
     */
    void synchronizeOfficialStudentRecords(String srmRegistrationNumber);

    /**
     * Ingestion provenance associated with this contract.
     */
    default DataProvenance getProvenance() {
        return DataProvenance.OFFICIAL_INSTITUTIONAL_API;
    }
}
