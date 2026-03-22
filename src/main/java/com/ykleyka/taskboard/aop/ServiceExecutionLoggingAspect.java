package com.ykleyka.taskboard.aop;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceExecutionLoggingAspect {

    @Around("execution(public * com.ykleyka.taskboard.service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        String signature = joinPoint.getSignature().toShortString();
        try {
            Object result = joinPoint.proceed();
            log.info("Executed {} in {} ms", signature, toMillis(start));
            return result;
        } catch (Throwable exception) {
            log.warn(
                    "Failed {} in {} ms: {}",
                    signature,
                    toMillis(start),
                    exception.getMessage());
            throw exception;
        }
    }

    private long toMillis(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
