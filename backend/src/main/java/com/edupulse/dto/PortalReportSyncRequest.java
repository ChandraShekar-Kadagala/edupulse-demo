package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalReportSyncRequest {
    private String attendanceHtml;
    private String marksHtml;
    private String ledgerHtml;
}
