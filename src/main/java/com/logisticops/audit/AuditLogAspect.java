// AuditLogAspect.java content
package com.logisticops.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditLogAspect {
    @After("execution(* com.logisticops.controller..*(..))")
    public void logAfter(JoinPoint joinPoint) {
        System.out.println("Executed method: " + joinPoint.getSignature().getName());
    }
}