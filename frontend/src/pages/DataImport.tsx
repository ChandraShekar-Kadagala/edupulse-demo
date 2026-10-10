import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Navigate } from 'react-router-dom';
import Papa from 'papaparse';
import api from '../services/api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { AlertTriangle, Upload, CheckCircle, Download, FileText } from 'lucide-react';

interface ParsedRow {
  student_id: string;
  academic_period: string;
  course_code: string;
  attendance_percentage: number;
  marks_obtained: number;
  max_marks: number;
  missing_assignments: number;
}

export default function DataImport() {
  const { user } = useAuth();
  const [file, setFile] = useState<File | null>(null);
  const [previewData, setPreviewData] = useState<ParsedRow[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [importResult, setImportResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  // Authorization Check
  if (user?.role === 'STUDENT') {
    return <Navigate to="/dashboard" replace />;
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      setFile(selectedFile);
      setError(null);
      setImportResult(null);

      // Browser-side preview and basic validation
      Papa.parse(selectedFile, {
        header: true,
        skipEmptyLines: true,
        complete: (results) => {
          const parsed = results.data.map((row: any) => ({
            student_id: row.student_id,
            academic_period: row.academic_period,
            course_code: row.course_code,
            attendance_percentage: parseFloat(row.attendance_percentage),
            marks_obtained: parseFloat(row.marks_obtained),
            max_marks: parseFloat(row.max_marks),
            missing_assignments: parseInt(row.missing_assignments, 10),
          }));
          setPreviewData(parsed.slice(0, 5)); // Preview top 5 rows
        },
        error: (err) => {
          setError("Failed to parse CSV: " + err.message);
        }
      });
    }
  };

  const handleConfirmImport = async () => {
    if (!file) return;
    setIsUploading(true);
    setError(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await api.post('/data-import/csv', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setImportResult(response.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to communicate with the server.');
    } finally {
      setIsUploading(false);
    }
  };

  const downloadTemplate = () => {
    const headers = "student_id,academic_period,course_code,attendance_percentage,marks_obtained,max_marks,missing_assignments\\n";
    const example = "STU-1001,2026-SEM1,CSE101,85,75,100,0\\n";
    const blob = new Blob([headers + example], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = "academic_import_template.csv";
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="p-8 max-w-6xl mx-auto space-y-6 overflow-y-auto h-full">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-white">Academic Data Import</h1>
          <p className="text-gray-400 mt-1">Upload verified academic records to the system.</p>
        </div>
        <Button onClick={downloadTemplate} variant="outline" className="flex items-center gap-2">
          <Download className="w-4 h-4" /> Download Template
        </Button>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/50 p-4 rounded-lg flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-red-400 mt-0.5" />
          <div>
            <h3 className="text-red-400 font-medium">Import Error</h3>
            <p className="text-red-300/80 text-sm mt-1">{error}</p>
          </div>
        </div>
      )}

      {importResult && (
        <Card className="bg-green-950/20 border-green-900/50">
          <CardHeader>
            <CardTitle className="text-green-400 flex items-center gap-2">
              <CheckCircle className="w-5 h-5" /> Import Complete: {importResult.status}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-4 gap-4 mb-4">
              <div className="bg-gray-900/50 p-3 rounded text-center">
                <div className="text-2xl font-bold text-white">{importResult.summary?.totalRecords || 0}</div>
                <div className="text-xs text-gray-400">Total</div>
              </div>
              <div className="bg-gray-900/50 p-3 rounded text-center">
                <div className="text-2xl font-bold text-green-400">{importResult.summary?.acceptedRecords || 0}</div>
                <div className="text-xs text-gray-400">Accepted</div>
              </div>
              <div className="bg-gray-900/50 p-3 rounded text-center">
                <div className="text-2xl font-bold text-yellow-400">{importResult.summary?.duplicateRecords || 0}</div>
                <div className="text-xs text-gray-400">Duplicates</div>
              </div>
              <div className="bg-gray-900/50 p-3 rounded text-center">
                <div className="text-2xl font-bold text-red-400">{importResult.summary?.rejectedRecords || 0}</div>
                <div className="text-xs text-gray-400">Rejected</div>
              </div>
            </div>
            
            {importResult.errors && importResult.errors.length > 0 && (
              <div className="mt-4">
                <h4 className="text-sm font-semibold text-gray-300 mb-2">Row-Level Errors</h4>
                <ul className="text-sm text-red-400 space-y-1 bg-gray-950 p-3 rounded border border-gray-800 h-32 overflow-y-auto">
                  {importResult.errors.map((e: any, idx: number) => (
                    <li key={idx}>Row {e.row} ({e.studentId}): {e.reason}</li>
                  ))}
                </ul>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <Card className="bg-gray-900 border-gray-800">
        <CardHeader>
          <CardTitle>Upload CSV File</CardTitle>
          <CardDescription>Select a strictly formatted CSV file to import.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center gap-4">
            <input 
              type="file" 
              accept=".csv" 
              onChange={handleFileChange}
              className="block w-full text-sm text-gray-400
                file:mr-4 file:py-2 file:px-4
                file:rounded-md file:border-0
                file:text-sm file:font-semibold
                file:bg-indigo-600 file:text-white
                hover:file:bg-indigo-700"
            />
          </div>

          {previewData.length > 0 && !importResult && (
            <div className="space-y-4">
              <h3 className="text-sm font-medium text-gray-300">Data Preview (First 5 rows)</h3>
              <div className="overflow-x-auto rounded-lg border border-gray-800">
                <table className="w-full text-sm text-left text-gray-300">
                  <thead className="text-xs text-gray-400 bg-gray-950 uppercase border-b border-gray-800">
                    <tr>
                      <th className="px-4 py-3">Student ID</th>
                      <th className="px-4 py-3">Period</th>
                      <th className="px-4 py-3">Course</th>
                      <th className="px-4 py-3">Att. %</th>
                      <th className="px-4 py-3">Marks</th>
                      <th className="px-4 py-3">Missing</th>
                    </tr>
                  </thead>
                  <tbody>
                    {previewData.map((row, i) => (
                      <tr key={i} className="border-b border-gray-800 bg-gray-900">
                        <td className="px-4 py-2 font-medium text-white">{row.student_id}</td>
                        <td className="px-4 py-2">{row.academic_period}</td>
                        <td className="px-4 py-2">{row.course_code}</td>
                        <td className="px-4 py-2">{row.attendance_percentage}</td>
                        <td className="px-4 py-2">{row.marks_obtained} / {row.max_marks}</td>
                        <td className="px-4 py-2">{row.missing_assignments}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="bg-yellow-500/10 border border-yellow-500/50 p-4 rounded-lg flex items-start gap-3">
                <AlertTriangle className="w-5 h-5 text-yellow-500 mt-0.5" />
                <div>
                  <h4 className="text-yellow-500 font-medium">Warning</h4>
                  <p className="text-yellow-200/80 text-sm mt-1">
                    You are about to submit this data for backend validation and persistence. Duplicates will be safely ignored. Existing records will NOT be overwritten.
                  </p>
                </div>
              </div>
              <Button onClick={handleConfirmImport} disabled={isUploading} className="w-full bg-indigo-600 hover:bg-indigo-700 text-white">
                {isUploading ? 'Validating and Importing...' : 'Confirm and Import Data'}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
