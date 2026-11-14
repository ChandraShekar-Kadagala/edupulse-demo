package com.edupulse.importing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The student-import preview endpoint validates an upload, stores nothing, and never reports "live". */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.seeder.enabled=false"})
class StudentImportPreviewTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    /** Two valid attendance rows, one broken (present+absent != conducted); a client lie of "LIVE" is ignored. */
    private static final String BODY = """
        {
          "sourceType": "LIVE_AUTHORIZED_CONNECTION",
          "reportDate": "2099-01-15",
          "attendance": [
            {"subjectCode":"SYN 101","subjectName":"One","classesConducted":"40","present":"34","absent":"6","odMlTaken":"0","presentPercentage":"85.00","odMlPercentage":"0","attendancePercentage":"85.00"},
            {"subjectCode":"SYN 102","subjectName":"Two","classesConducted":"30","present":"15","absent":"15","odMlTaken":"0","presentPercentage":"50.00","odMlPercentage":"0","attendancePercentage":"50.00"},
            {"subjectCode":"SYN 103","subjectName":"Bad","classesConducted":"40","present":"30","absent":"6","odMlTaken":"0","presentPercentage":"75","odMlPercentage":"0","attendancePercentage":"75"}
          ],
          "ledger": [
            {"semester":"Semester 1","monthYear":"DEC-2099","subjectCode":"SYN 101","subjectDescription":"One","credit":"4.0","grade":"A+","gradePoints":"10.0","result":"PASS","attempt":"1"}
          ],
          "cgpa": "8.85",
          "profile": {"registerNo":"SYN-0001","studentName":"Synthetic Person","gender":"X"}
        }
        """;

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Validates the upload, flags the bad row, and labels it a manual import (never live)")
    void previewValidatesAndNeverReportsLive() throws Exception {
        String json = mockMvc.perform(post("/api/students/me/imports/preview")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.sourceLabel", containsString("not independently verified")))
                .andExpect(jsonPath("$.acceptedAttendance").value(2))
                .andExpect(jsonPath("$.acceptedLedger").value(1))
                .andExpect(jsonPath("$.cgpa").value(8.85))
                .andExpect(jsonPath("$.rejected").value(1))
                .andReturn().getResponse().getContentAsString();

        JsonNode r = om.readTree(json);
        // The server ignores the client's "LIVE" claim: nothing is ever live, and only the two good rows pass.
        boolean badRowReported = false;
        for (JsonNode issue : r.get("issues")) {
            if (issue.get("message").asText().contains("SYN 103")) badRowReported = true;
        }
        org.junit.jupiter.api.Assertions.assertTrue(badRowReported, "the inconsistent row must be reported");
    }

    @Test
    @DisplayName("Anonymous callers cannot use the preview endpoint")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/students/me/imports/preview")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("An empty upload is valid and simply accepts nothing")
    void emptyUploadAcceptsNothing() throws Exception {
        mockMvc.perform(post("/api/students/me/imports/preview")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedAttendance").value(0))
                .andExpect(jsonPath("$.acceptedLedger").value(0))
                .andExpect(jsonPath("$.cgpa").doesNotExist());
    }
}
