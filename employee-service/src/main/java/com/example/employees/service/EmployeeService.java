package com.example.employees.service;

import com.example.employees.dto.EmployeeRequestDTO;
import com.example.employees.dto.EmployeeResponseDTO;
import com.example.employees.exception.DuplicateEmailException;
import com.example.employees.exception.ResourceNotFoundException;
import com.example.employees.mapper.EmployeeMapper;
import com.example.employees.model.Department;
import com.example.employees.model.Employee;
import com.example.employees.repository.DepartmentRepository;
import com.example.employees.repository.EmployeeRepository;
import com.example.employees.specification.EmployeeSpecifications;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
@Transactional
public class EmployeeService {

    private static final String UPLOAD_DIR = "uploads";

    private final EmployeeRepository repo;
    private final DepartmentRepository deptRepo;

    public EmployeeService(EmployeeRepository repo, DepartmentRepository deptRepo) {
        this.repo = repo;
        this.deptRepo = deptRepo;
    }

    // Cached: repeated identical searches (same text + department + page) are served
    // from memory instead of hitting the database again. Any write below evicts the
    // whole cache, so results can never go stale after an add/edit/delete.
    @Cacheable(
        value = "employeeSearch",
        key = "#q + '_' + #departmentId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()"
    )
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDTO> findAll(String q, Long departmentId, Pageable pageable) {
        Specification<Employee> spec = Specification
                .where(EmployeeSpecifications.notDeleted())
                .and(EmployeeSpecifications.search(q))
                .and(EmployeeSpecifications.inDepartment(departmentId));
        return repo.findAll(spec, pageable).map(EmployeeMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Employee findEntityById(Long id) {
        return repo.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDTO findById(Long id) {
        return EmployeeMapper.toDTO(findEntityById(id));
    }

    @CacheEvict(value = "employeeSearch", allEntries = true)
    public EmployeeResponseDTO create(EmployeeRequestDTO dto) {
        if (repo.existsByEmailIgnoreCaseAndDeletedFalse(dto.getEmail())) {
            throw new DuplicateEmailException("An employee with this email already exists");
        }
        Department dept = findDepartmentOrThrow(dto.getDepartmentId());
        Employee e = new Employee();
        applyDto(e, dto, dept);
        return EmployeeMapper.toDTO(repo.save(e));
    }

    @CacheEvict(value = "employeeSearch", allEntries = true)
    public EmployeeResponseDTO update(Long id, EmployeeRequestDTO dto) {
        Employee existing = findEntityById(id);
        if (repo.existsByEmailIgnoreCaseAndIdNotAndDeletedFalse(dto.getEmail(), id)) {
            throw new DuplicateEmailException("An employee with this email already exists");
        }
        Department dept = findDepartmentOrThrow(dto.getDepartmentId());
        applyDto(existing, dto, dept);
        return EmployeeMapper.toDTO(repo.save(existing));
    }

    // Soft delete: the row is kept and just flagged, which is why every read in this
    // class filters on "not deleted" instead of relying on the row being gone.
    @CacheEvict(value = "employeeSearch", allEntries = true)
    public void delete(Long id) {
        Employee e = findEntityById(id);
        e.setDeleted(true);
        repo.save(e);
    }

    @CacheEvict(value = "employeeSearch", allEntries = true)
    public EmployeeResponseDTO uploadPhoto(Long id, MultipartFile file) {
        Employee e = findEntityById(id);
        try {
            Path dir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            String safeName = id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path target = dir.resolve(safeName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            e.setProfileImagePath(safeName);
            return EmployeeMapper.toDTO(repo.save(e));
        } catch (IOException ex) {
            throw new RuntimeException("Could not save the uploaded photo", ex);
        }
    }

    @Transactional(readOnly = true)
    public String exportCsv() {
        List<Employee> all = repo.findAll(Specification.where(EmployeeSpecifications.notDeleted()));
        StringBuilder sb = new StringBuilder("Name,Email,Department,Job Title,Salary,Join Date\n");
        for (Employee e : all) {
            sb.append(csv(e.getName())).append(',')
              .append(csv(e.getEmail())).append(',')
              .append(csv(e.getDepartment() != null ? e.getDepartment().getName() : "")).append(',')
              .append(csv(e.getJobTitle())).append(',')
              .append(e.getSalary()).append(',')
              .append(e.getJoinDate() != null ? e.getJoinDate() : "").append('\n');
        }
        return sb.toString();
    }

    private Department findDepartmentOrThrow(Long departmentId) {
        return deptRepo.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department " + departmentId + " not found"));
    }

    private void applyDto(Employee e, EmployeeRequestDTO dto, Department dept) {
        e.setName(dto.getName());
        e.setEmail(dto.getEmail());
        e.setJobTitle(dto.getJobTitle());
        e.setSalary(dto.getSalary());
        e.setJoinDate(dto.getJoinDate());
        e.setDepartment(dept);
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
