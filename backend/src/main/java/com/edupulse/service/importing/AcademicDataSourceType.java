package com.edupulse.service.importing;

/**
 * How a piece of academic data reached EduPulse. Used for user-facing labels so that a manual import is
 * never presented as a live institutional connection.
 */
public enum AcademicDataSourceType {

    /** Institution-approved, authorized connection (API or sanctioned export feed). None exists today. */
    LIVE_AUTHORIZED_CONNECTION("Live institutional connection", true),

    /** Report/file the student downloaded from the portal and uploaded themselves. Not independently verified. */
    STUDENT_MANUAL_IMPORT("Imported by the student from a portal report (not independently verified)", false),

    /** Previously imported data shown again; may be out of date. */
    CACHED_IMPORT("Previously imported data (may be out of date)", false),

    /** Synthetic showcase data. */
    SYNTHETIC_DEMO("Synthetic demo data", false);

    private final String label;
    private final boolean live;

    AcademicDataSourceType(String label, boolean live) {
        this.label = label;
        this.live = live;
    }

    public String label() {
        return label;
    }

    /** True only for an authorized live connection. Manual and cached imports are never live. */
    public boolean isLive() {
        return live;
    }
}
