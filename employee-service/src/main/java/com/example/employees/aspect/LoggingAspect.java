package com.example.employees.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// This is a "cross-cutting concern": instead of adding timing/logging code inside
// every method of EmployeeService and DepartmentService, one rule here wraps all
// of them automatically. The service classes stay focused only on business logic.
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.example.employees.service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long elapsed = System.currentTimeMillis() - start;
        log.info("[AOP] {} executed in {} ms", pjp.getSignature().toShortString(), elapsed);
        return result;
    }
}
