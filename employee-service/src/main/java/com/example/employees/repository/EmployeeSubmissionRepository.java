package com.example.employees.repository;

import com.example.employees.model.EmployeeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeSubmissionRepository extends JpaRepository<EmployeeSubmission, Long> {

    // JOIN FETCH loads the department in the SAME query, so it's already filled in by the
    // time the transaction closes - without this, reading submission.getDepartment() during
    // JSON serialization (which happens after the transaction ends, since open-in-view=false)
    // throws a LazyInitializationException.
    @Query("SELECT s FROM EmployeeSubmission s LEFT JOIN FETCH s.department WHERE s.status = :status ORDER BY s.submittedAt DESC")
    List<EmployeeSubmission> findByStatusOrderBySubmittedAtDesc(@Param("status") String status);

    @Query("SELECT s FROM EmployeeSubmission s LEFT JOIN FETCH s.department WHERE s.id = :id")
    Optional<EmployeeSubmission> findByIdWithDepartment(@Param("id") Long id);
}
