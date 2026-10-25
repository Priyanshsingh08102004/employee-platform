package com.example.employees.specification;

import com.example.employees.model.Employee;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Each method returns a small, reusable filter. The service layer combines them
 * with .and(...) so one query can handle "search text only", "department only",
 * "both", or "neither" without writing a separate @Query for every combination.
 */
public class EmployeeSpecifications {

    public static Specification<Employee> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Employee> search(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction(); // "always true" - no filtering applied
            }
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("jobTitle")), like),
                    cb.like(cb.lower(root.join("department", JoinType.LEFT).get("name")), like)
            );
        };
    }

    public static Specification<Employee> inDepartment(Long departmentId) {
        return (root, query, cb) -> {
            if (departmentId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("department").get("id"), departmentId);
        };
    }
}
