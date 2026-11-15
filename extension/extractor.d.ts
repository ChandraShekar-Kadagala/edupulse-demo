export interface ExtractedAcademicExport {
  sourceType: 'STUDENT_MANUAL_IMPORT';
  exportedAt: string;
  reportDate: string | null;
  attendance: Array<Record<string, string>>;
  ledger: Array<Record<string, string>>;
  cgpa: string | null;
  profile: Record<string, string>;
}

/** Reads the live SRM report DOM (and same-origin frames) into a clean, personal-field-stripped export. */
export function extractAcademicExport(rootDoc?: Document): ExtractedAcademicExport;
