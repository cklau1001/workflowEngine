package io.cklau1001.workflow1.wfe.engine;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Entry point to invoke the workflow engine on a regular basis
 * If the scheduler should be run as a separate process, active profile can be defined for that purpose.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("SCHEDULER")
public class WorkflowScheduler {

    private final WorkflowCoordinator workflowCoordinator;
    private final ObservationRegistry observationRegistry;

    @Scheduled(fixedDelay = 30000)
    public void invoke() throws Exception {

        Observation.createNotStarted("scheduler.job", observationRegistry).observe(() -> {
            log.info(">>>>>>>>>> [invoke]: {}: START", Thread.currentThread().getName());
            log.info("[invoke]: {}: scan any pending QUEUED requests", Thread.currentThread().getName());
            // move QUEUED requests to EXECUTING
            workflowCoordinator.executeRequests();

            log.info("[invoke]: {}: execute any QUEUED / RETRY tasks", Thread.currentThread().getName());
            // executing tasks
            workflowCoordinator.executeTasks();

            log.info("[invoke]: {}: scan any completed requests", Thread.currentThread().getName());
            // Mark completed or Failed on request
            workflowCoordinator.finalizeRequests();

            log.info("[invoke]: {}: handle any hung requests", Thread.currentThread().getName());
            workflowCoordinator.handleHungRequests();

            log.info("<<<<<<<<<<<< [invoke]: {}: END.", Thread.currentThread().getName());

        });

    }
}
