package io.cklau1001.workflow1.aop;

import io.cklau1001.workflow1.dto.ProcessResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TimerWorkflowAspect {

    private final MeterRegistry meterRegistry;

    /*
        The advice needs to access parameters of timerWorkflow, bind it with the variable name
     */
    @Around("@annotation(timerWorkflow)")
    public Object aroundTimeMethod(ProceedingJoinPoint pjp, TimerWorkflow timerWorkflow) throws Exception {
        log.info("[aroundTimeMethod]: entered <<<<<<<<<< ");
        Timer.Sample sample = Timer.start(meterRegistry);
        String status = "success";
        try {
            return pjp.proceed();

        } catch (Exception e) {
            status = "error";
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            sample.stop(Timer.builder("bookticket.frequency")
                    .tag("status", status)
                    .tag("workflow", timerWorkflow.workflow())
                    .register(meterRegistry));
            log.info("[aroundTimeMethod]: exit >>>>>>>>>>>>>> ");
        }

    }
}
