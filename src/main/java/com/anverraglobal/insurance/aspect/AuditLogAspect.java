package com.anverraglobal.insurance.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogAspect.class);

    @Around("@annotation(auditLog)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        
        String action = auditLog.value().isEmpty() ? methodName : auditLog.value();

        logger.info("AUDIT: Starting {} in {} with args: {}", action, className, Arrays.toString(args));
        
        long start = System.currentTimeMillis();
        Object proceed;
        try {
            proceed = joinPoint.proceed();
        } catch (Throwable t) {
            logger.error("AUDIT: Exception in {} - {}", action, t.getMessage());
            throw t;
        }
        
        long executionTime = System.currentTimeMillis() - start;
        logger.info("AUDIT: Completed {} in {} ms", action, executionTime);
        
        return proceed;
    }
}
