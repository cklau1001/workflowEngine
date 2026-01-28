package io.cklau1001.workflow1.wfe.engine;

import io.cklau1001.workflow1.wfe.component.*;
import io.cklau1001.workflow1.wfe.dto.RequestInfo;
import io.cklau1001.workflow1.wfe.dto.TaskInfo;
import io.cklau1001.workflow1.wfe.model.RequestEntity;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.RequestDBService;
import io.cklau1001.workflow1.wfe.service.TaskDBService;
import io.cklau1001.workflow1.wfe.service.TaskExecutorDBHelper;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * The top-level class used by WorkflowScheduler and users to mainpulate workflow engine
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowCoordinator {

    private final RequestDBService requestDBService;
    private final TaskDBService taskDBService;
    private final TaskExecutorDBHelper taskExecutorDBHelper;
    private final WorkflowRegistry workflowRegistry;
    private final ObjectProvider<TaskExecutor> taskExecutorProvider;
    private final ExecutorService wfeExecutorService;
    private final TaskUtil taskUtil;

    /**
     * create a new request
     *
     * @param workflowName
     * @param payload
     * @return
     */
    public String newRequest(String workflowName, Object payload) {

        return requestDBService.newRequest(workflowName, payload);
    }

    /**
     * Get a list of QUEUED requests and update them to EXECUTING
     * A permissitic lock is used so that another JVM can only get the QUEUED requests after this JVM completes.
     *
     */
    @Transactional
    public void executeRequests() {

        List<RequestInfo> queueingTasks = requestDBService.findAllPendingRequests(Integer.MAX_VALUE);

        for (RequestInfo requestInfo: queueingTasks) {

            String workflowName = requestInfo.getWorkflowName();
            Optional<Step> firstStepOptional = workflowRegistry.getFirstStep(workflowName, requestInfo.getRequestId());
            if (firstStepOptional.isEmpty()) {
                // mark request failed
                String error = "[executeRequests]: No firstStepId provided, mark request failed, requestId=%s"
                        .formatted(requestInfo.getRequestId());

                log.warn(error);
                requestDBService.markFailed(requestInfo.getRequestId(), error);
                continue;
            }

            Step firstStep = firstStepOptional.get();
            markRequestExecutingStatus(requestInfo.getRequestId(), firstStep);
        }

    }

    /**
     * Create a new task and mark request to EXECUTING from QUEUED
     * make it as a separate method to form a transaction context
     *
     * @param requestId
     * @param firstStep
     */
    @Transactional
    public void markRequestExecutingStatus(@NonNull String requestId, @NonNull Step firstStep) {

        Task task = workflowRegistry.getTask(firstStep.getTaskName()).orElseThrow(() -> {
            String error = ("[markRequestExeutingStatus]: Unable to find related task of target step, " +
                    "taskName=[%s], stepId=[{}], requestId=[{}]").formatted(
                            firstStep.getTaskName(), firstStep.getStepId(), requestId);
            log.error(error);
            return new IllegalArgumentException(error);
        });
        int maxRetry = workflowRegistry.getPollableConfig(task).map(PollableConfig::maxRetry).orElse(0);
        int retryInterval = workflowRegistry.getPollableConfig(task).map(PollableConfig::retryIntervalInMinutes).orElse(0);


        String taskId = taskDBService.newTask(requestId, firstStep, null, maxRetry);
        requestDBService.markExecutingFromQueued(requestId);
        log.info(("[executingRequestStatus]: mark request [EXECUTING] and firstStep [QUEUED], " +
                "requestId=[%s], taskName=[%s], taskId=[%s], stepId=[%s]").formatted(
                requestId, firstStep.getTaskName(), taskId, firstStep.getStepId()
        ));
    }

    /**
     * Check all EXECUTING (Not completed) requests and
     * mark COMPLETED if no non-completing task status ( EXECUTING, RETRY, SUSPENDED )
     * for every COMPLETED request
     *   mark COMPLETED if no FAILED tasks,
     *   mark FAILED if FAILED task(s)
     *
     */
    @Transactional
    public void finalizeRequests() {

        log.info("[finalizeRequests]: entered");
        Set<TaskEntity.TaskStatus> runningTaskStatusSet = Set.of(TaskEntity.TaskStatus.EXECUTING,
                TaskEntity.TaskStatus.QUEUED,
                TaskEntity.TaskStatus.SUSPENDED,
                TaskEntity.TaskStatus.RETRY);

        List<RequestInfo> executingTasks = requestDBService.findAllRequestsByStatus(RequestEntity.RequestStatus.EXECUTING);

        log.debug("[finalizeRequests]: executingTasks-len=[{}]", executingTasks.size());

        for (RequestInfo requestInfo: executingTasks) {
            log.debug("[finalizeRequests]: checking taskStatus of request, requestId=[{}], workflowName=[{}]",
                    requestInfo.getRequestId(), requestInfo.getWorkflowName());

            List<TaskInfo> taskInfoList = taskDBService.getAllTasksByRequestId(requestInfo.getRequestId());
            Set<TaskEntity.TaskStatus> statusSet = taskInfoList.stream()
                    .map(TaskInfo::getTaskStatus)
                    .collect(Collectors.toSet());

            statusSet.forEach(s -> log.debug("[finalizeRequests]: list of status in the given request, requestId=[{}], status=[{}]",
                    requestInfo.getRequestId(), s));

            if (statusSet.isEmpty()) {
                log.info("[finalizeRequests]: request not yet started - SKIP, requestId=[{}], workflowName=[{}]",
                        requestInfo.getRequestId(), requestInfo.getWorkflowName());
                continue;
            }

            // continue if any running status found in statusSet
            if (!Collections.disjoint(statusSet, runningTaskStatusSet)) {
                log.info("[finalizeRequests]: request not yet fully completed - SKIP, requestId=[{}], workflowName=[{}]",
                        requestInfo.getRequestId(), requestInfo.getWorkflowName());
                for (TaskEntity.TaskStatus taskStatus: statusSet) {
                    log.debug("[finalizeRequests]: taskStatus=[{}], requestId=[{}]",
                            taskStatus, requestInfo.getRequestId());
                }
                continue;
            }

            if (statusSet.contains(TaskEntity.TaskStatus.FAILED)) {
                String error = "[finalizeRequests]: marked [FAILED] on request, workflowName=[%s], requestId=[%s]"
                        .formatted(requestInfo.getWorkflowName(), requestInfo.getRequestId());
                log.info(error);
                requestDBService.markFailed(requestInfo.getRequestId(), error);
            } else {
                log.info("[finalizeRequests]: marked [COMPLETED] on request, workflowName=[{}], requestId=[{}]",
                        requestInfo.getWorkflowName(), requestInfo.getRequestId());
                requestDBService.markSuccess(requestInfo.getRequestId());
            }
        }
    }

    /**
     * Get a list of pending tasks (QUEUED / RETRY ) and execute each via TaskExecutor
     * Transactional context is created in getAllPendingTasks that mark task to executing, and inside call() of
     * TaskExecutor that marks the task status to COMPLETED or FAILED as appropriate.
     *
     *
     */
    public void executeTasks() {

        log.info("[executeTasks]: entered");

        // A transaction context is created in getAllPendingTasks that marks task to executing
        List<TaskInfo> pendingTasks = taskDBService.getAllPendingTasks(true, Integer.MAX_VALUE);

        log.debug("[executeTasks]: pendingTask={}", pendingTasks);

        for (TaskInfo taskInfo: pendingTasks) {
            log.info("[executeTasks]: Executing task, taskInfo=[{}]", taskInfo);
            TaskExecutor taskExecutor = taskExecutorProvider.getObject(taskExecutorDBHelper,
                    workflowRegistry, taskInfo, taskUtil);
            log.debug("[executeTasks]: taskExecutor={}", taskExecutor);

            /*
            CompletableFuture.runAsync(() -> {
                try {
                    taskExecutor.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }, wfeExecutorService).exceptionally(e -> {
                log.error("[executeTasks]: issue in running the task in background, requestId={}, taskId={}, taskName={}, error={}",
                        taskInfo.getRequestId(), taskInfo.getTaskId(), taskInfo.getTaskName(), e.getMessage());
                return null;
            });

             */

            Future<TaskResult> taskResult = wfeExecutorService.submit(() -> {
                try {
                    // Thread.sleep(60000);
                    return taskExecutor.call();
                    // log.debug("NOT calling worker");

                } catch (Exception e) {
                    log.error("[executeTasks]: issue in running the task in background, requestId=[{}], taskId=[{}], taskName=[{}], error=[{}]",
                            taskInfo.getRequestId(), taskInfo.getTaskId(), taskInfo.getTaskName(), e.getMessage());
                    return TaskResult.getInstance(taskInfo.getTaskId(), TaskEntity.TaskStatus.FAILED, e.getMessage());

                } finally {
                    // clean up taskExecutor
                    taskExecutor.destroy();
                }

            });
            log.info("[executeTasks]: Task invoked, taskId={}, taskName={}", taskInfo.getTaskId(), taskInfo.getTaskName());

        }
    }

    public Future<String> testThread() {

        Future<String> taskResult = wfeExecutorService.submit(() -> {

            log.info("[testThread]: {} triggered", Thread.currentThread().getName());
            Thread.sleep(5000);
            log.info("[testThread]: {} working", Thread.currentThread().getName());
            return Instant.now().toString();
        });

        return taskResult;
    }

    @Transactional
    public void handleHungRequests() {

        log.info("[handleHungRequests]: entered");
        List<TaskInfo> taskInfoList = taskDBService.markHungTasksFailed(Integer.MAX_VALUE, 60);
        for (TaskInfo taskInfo: taskInfoList) {
            requestDBService.markFailed(taskInfo.getRequestId(),
                    "Hung request for taskId=[%s]".formatted(taskInfo.getTaskId()));
        }
        log.info("[handleHungRequests]: ended");
    }

    public RequestInfo getRequestInfo(String requestId) {

        Optional<RequestInfo> requestInfoOptional = requestDBService.getRequestDetailsByRequestId(requestId);

        return requestInfoOptional.orElseThrow(() -> new IllegalArgumentException(
                "[getRequestInfo]: No such request, requestId=%s".formatted(requestId)));
    }

    public RequestEntity.RequestStatus getRequestStatus(String requestId) {

        RequestInfo requestInfo = getRequestInfo(requestId);

        return requestInfo.getRequestStatus();
    }

}
