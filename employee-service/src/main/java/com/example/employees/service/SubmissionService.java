package com.example.employees.service;

import com.example.employees.dto.SubmissionCorrectionDTO;
import com.example.employees.exception.ResourceNotFoundException;
import com.example.employees.model.Department;
import com.example.employees.model.Employee;
import com.example.employees.model.EmployeeSubmission;
import com.example.employees.repository.DepartmentRepository;
import com.example.employees.repository.EmployeeRepository;
import com.example.employees.repository.EmployeeSubmissionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SubmissionService {

    private static final String RESUME_DIR = "uploads/resumes";

    private final EmployeeSubmissionRepository repo;
    private final DepartmentRepository deptRepo;
    private final EmployeeRepository employeeRepo;
    private final EmailService emailService;

    public SubmissionService(EmployeeSubmissionRepository repo, DepartmentRepository deptRepo,
                              EmployeeRepository employeeRepo, EmailService emailService) {
        this.repo = repo;
        this.deptRepo = deptRepo;
        this.employeeRepo = employeeRepo;
        this.emailService = emailService;
    }

    // PUBLIC - no login required. Anyone can submit their details to apply.
    public EmployeeSubmission submit(String name, String email, String phone, String jobTitle,
                                      BigDecimal expectedSalary, Long departmentId, MultipartFile resume) {
        Department dept = findDepartmentOrThrow(departmentId);

        EmployeeSubmission s = new EmployeeSubmission();
        s.setName(name);
        s.setEmail(email);
        s.setPhone(phone);
        s.setJobTitle(jobTitle);
        s.setExpectedSalary(expectedSalary);
        s.setDepartment(dept);
        s.setStatus("PENDING");

        if (resume != null && !resume.isEmpty()) {
            s.setResumeFilePath(saveResume(resume));
        }
        return repo.save(s);
    }

    @Transactional(readOnly = true)
    public List<EmployeeSubmission> findByStatus(String status) {
        return repo.findByStatusOrderBySubmittedAtDesc(status.toUpperCase());
    }

    // Manager corrects details BEFORE approving (fixes typos, adjusts salary, reassigns department, etc.)
    public EmployeeSubmission update(Long id, SubmissionCorrectionDTO dto) {
        EmployeeSubmission existing = findOrThrow(id);
        Department dept = findDepartmentOrThrow(dto.getDepartmentId());

        existing.setName(dto.getName());
        existing.setEmail(dto.getEmail());
        existing.setPhone(dto.getPhone());
        existing.setJobTitle(dto.getJobTitle());
        existing.setExpectedSalary(dto.getExpectedSalary());
        existing.setDepartment(dept);
        return repo.save(existing);
    }

    // Approving a submission is what actually creates the real Employee record,
    // and it also invalidates the Employee search cache so the new hire shows up immediately.
    @CacheEvict(value = "employeeSearch", allEntries = true)
    public EmployeeSubmission approve(Long id, String managerNote) {
        EmployeeSubmission s = findOrThrow(id);
        s.setStatus("APPROVED");
        s.setManagerNote(managerNote);
        repo.save(s);

        Employee e = new Employee();
        e.setName(s.getName());
        e.setEmail(s.getEmail());
        e.setJobTitle(s.getJobTitle());
        e.setSalary(s.getExpectedSalary());
        e.setJoinDate(LocalDate.now());
        e.setDepartment(s.getDepartment());
        employeeRepo.save(e);

        emailService.sendApproval(s.getEmail(), s.getName());
        return s;
    }

    public EmployeeSubmission reject(Long id, String reason) {
        EmployeeSubmission s = findOrThrow(id);
        s.setStatus("REJECTED");
        s.setManagerNote(reason);
        repo.save(s);
        emailService.sendRejection(s.getEmail(), s.getName(), reason);
        return s;
    }

    private String saveResume(MultipartFile file) {
        try {
            Path dir = Paths.get(RESUME_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            String safeName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path target = dir.resolve(safeName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "resumes/" + safeName; // relative to the shared /uploads/** web folder
        } catch (IOException ex) {
            throw new RuntimeException("Could not save the uploaded resume", ex);
        }
    }

    private Department findDepartmentOrThrow(Long departmentId) {
        return deptRepo.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department " + departmentId + " not found"));
    }

    private EmployeeSubmission findOrThrow(Long id) {
        return repo.findByIdWithDepartment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application " + id + " not found"));
    }
}
