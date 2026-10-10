const fs = require('fs');
const path = require('path');

const entities = [
  'User', 'Student', 'Faculty', 'Course', 'Enrollment', 'Attendance',
  'Assessment', 'Mark', 'Assignment', 'AssignmentSubmission',
  'RiskAssessment', 'RiskFactor', 'Intervention', 'FollowUp', 'AuditLog'
];

const targetDir = path.join(__dirname, 'src', 'main', 'java', 'com', 'edupulse', 'repository');

if (!fs.existsSync(targetDir)) {
  fs.mkdirSync(targetDir, { recursive: true });
}

entities.forEach(entity => {
  const content = "package com.edupulse.repository;\n\n" +
"import com.edupulse.model." + entity + ";\n" +
"import org.springframework.data.jpa.repository.JpaRepository;\n" +
"import org.springframework.stereotype.Repository;\n\n" +
"@Repository\n" +
"public interface " + entity + "Repository extends JpaRepository<" + entity + ", Long> {\n" +
"}\n";
  fs.writeFileSync(path.join(targetDir, entity + 'Repository.java'), content);
});

// A few specific methods we'll need for MVP
const riskAssessmentRepo = path.join(targetDir, 'RiskAssessmentRepository.java');
let riskContent = fs.readFileSync(riskAssessmentRepo, 'utf8');
riskContent = riskContent.replace('}', "\n    java.util.Optional<RiskAssessment> findFirstByStudentIdOrderByCalculatedAtDesc(Long studentId);\n}\n");
fs.writeFileSync(riskAssessmentRepo, riskContent);

const attendanceRepo = path.join(targetDir, 'AttendanceRepository.java');
let attContent = fs.readFileSync(attendanceRepo, 'utf8');
attContent = attContent.replace('}', "\n    java.util.List<Attendance> findByStudentIdAndDateAfter(Long studentId, java.time.LocalDate date);\n}\n");
fs.writeFileSync(attendanceRepo, attContent);

const markRepo = path.join(targetDir, 'MarkRepository.java');
let markContent = fs.readFileSync(markRepo, 'utf8');
markContent = markContent.replace('}', "\n    java.util.List<Mark> findByStudentId(Long studentId);\n}\n");
fs.writeFileSync(markRepo, markContent);

const interventionRepo = path.join(targetDir, 'InterventionRepository.java');
let interContent = fs.readFileSync(interventionRepo, 'utf8');
interContent = interContent.replace('}', "\n    java.util.List<Intervention> findByStudentId(Long studentId);\n    java.util.List<Intervention> findByAssignedFacultyId(Long facultyId);\n}\n");
fs.writeFileSync(interventionRepo, interContent);

const followUpRepo = path.join(targetDir, 'FollowUpRepository.java');
let followContent = fs.readFileSync(followUpRepo, 'utf8');
followContent = followContent.replace('}', "\n    java.util.List<FollowUp> findByInterventionId(Long interventionId);\n}\n");
fs.writeFileSync(followUpRepo, followContent);

const userRepo = path.join(targetDir, 'UserRepository.java');
let userContent = fs.readFileSync(userRepo, 'utf8');
userContent = userContent.replace('}', "\n    java.util.Optional<User> findByUsername(String username);\n}\n");
fs.writeFileSync(userRepo, userContent);


console.log('Successfully generated repository classes.');
