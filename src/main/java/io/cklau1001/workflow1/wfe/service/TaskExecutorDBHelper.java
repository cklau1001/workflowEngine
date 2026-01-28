package io.cklau1001.workflow1.wfe.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.cklau1001.workflow1.wfe.component.*;
import io.cklau1001.workflow1.wfe.dto.TaskInfo;
import io.cklau1001.workflow1.wfe.engine.WorkflowRegistry;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * To assist TaskExecutor to perform all DB related work with a transactional context. Thus, TaskExecutor does not
 * need to create any transaction context, which is a workaround to the proxy nature of a bean.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutorDBHelper {

    private final RequestDBService requestDBService;
    private final TaskDBService taskDBService;
    private final WorkflowRegistry workflowRegistry;


    @Transactional
    public void failTask(@NonNull TaskInfo taskInfo, @NonNull Context context, @NonNull String errorMessage, String output) {

        log.info("[failTask]: entered: requestId={}, taskId={}, context={}",
                taskInfo.getRequestId(), taskInfo.getTaskId(), context);

        String finalOutput = output == null || output.isEmpty() ? errorMessage : output + "\n" + errorMessage;
        taskDBService.markFailed(taskInfo.getTaskId(), finalOutput);
        requestDBService.markFailed(taskInfo.getRequestId(), errorMessage);

    }

    @Transactional
    public void retryTask(@NonNull TaskInfo taskInfo, @NonNull Context context, @NonNull PollableConfig pollableConfig, String output) {

        log.info("[retryTask]: entered, taskInfo={}", taskInfo);

        if (taskInfo.getRetryCount() - 1 <= 0) {
            String error = "[retryTask]: exhausted all trial, requestId=[%s], taskId=[%s], taskName=[%s]"
                    .formatted(taskInfo.getRequestId(), taskInfo.getTaskId(), taskInfo.getTaskName());

            log.error(error);
            failTask(taskInfo, context, error, output);
            return;
        }

        // update task status from EXECUTING to RETRY
        taskDBService.markRetryFromExecuting(taskInfo.getTaskId(), LocalDateTime.ofInstant(
                Instant.now().plus(pollableConfig.retryIntervalInMinutes(), ChronoUnit.MINUTES),
                ZoneOffset.UTC
        ), output);
    }

    @Transactional
    public void completeTask(TaskInfo taskInfo, Context context, String output) {

        log.info("[completeTask]: entered: requestId={}, taskId={}, context={}",
                taskInfo.getRequestId(), taskInfo.getTaskId(), context);

        taskDBService.markSuccess(taskInfo.getTaskId(), output);
        // TODO : derive the next step
        createNextSteps(taskInfo, context);
    }

    public void createNextSteps(@NonNull TaskInfo taskInfo, @NonNull Context context) {

        log.info("[createNextSteps]: entered: requestId={}, taskId={}, context={}",
                taskInfo.getRequestId(), taskInfo.getTaskId(), context);

        Optional<Step> stepOptional = workflowRegistry.getStepFromStepId(taskInfo.getWorkflowName(),
                taskInfo.getStepId(), taskInfo.getRequestId());

        Step currentStep = stepOptional.orElseThrow(() -> {
            String error = "[createNextSteps]: Unable to find stepId from taskInfo, taskInfo=%s".formatted(taskInfo);
            log.error(error);
            return new IllegalArgumentException(error);
        });

        log.debug("[createNextSteps]: currentStep={}", currentStep);

        List<String> nextStepIds = currentStep.getNextStepIds(context);

        log.debug("[createNextSteps]: number of next steps to be created, length={}", nextStepIds.size());
        for (String nextStepId: nextStepIds) {
            log.debug("[createNextSteps]: currentStep={}, nextStepId={}, requestId={}, taskId={}",
                    currentStep, nextStepId, taskInfo.getRequestId(), taskInfo.getTaskId());
            Optional<Step> nextStepOptional = workflowRegistry.getStepFromStepId(taskInfo.getWorkflowName(),
                    nextStepId, taskInfo.getRequestId());
            if (nextStepOptional.isEmpty()) {
                log.warn("[createNextSteps]: no step for nextStepId - SKIP, currentStep=%s, nextStepId=%s, taskInfo=%s"
                        .formatted(currentStep ,nextStepOptional, taskInfo));
                continue;
            }
            Step nextStep = nextStepOptional.get();
            Map<String, Object> passingmap = context.getPassingMap();
            createTaskRecord(taskInfo, currentStep, nextStep, passingmap);

        }

    }

    public void createTaskRecord(@NonNull TaskInfo taskInfo, @NonNull Step currentStep, @NonNull Step nextStep, Map<String, Object> passingmap) {

        Optional<Task> optionalTask = workflowRegistry.getTask(nextStep.getTaskName());
        if (optionalTask.isEmpty()) {
            log.warn("[createTaskRecord]: no real task defined for the taskname of next step - SKIP, currentStep={}, taskName={}, nextStep={}",
                    currentStep, nextStep.getTaskName(), nextStep);
            return;
        }
        Task nextTask = optionalTask.get();

        String taskId;
        if (workflowRegistry.isPollableTask(nextTask)) {
            PollableConfig pollableConfig = ((Pollable) nextTask).pollableConfig();
            log.info("[createTaskRecord]: create a [Pollable] task record, currentStep={}, nextStep={}, taskName={}, " +
                            "maxRetry={}, retryInterval={}", currentStep, nextStep, taskInfo.getTaskName(),
                    pollableConfig.maxRetry(), pollableConfig.retryIntervalInMinutes());
            taskId = taskDBService.newTask(taskInfo.getRequestId(),
                    nextStep,
                    passingmap,
                    pollableConfig.maxRetry());
        } else {
            log.info("[createTaskRecord]: create a [Normal] task record, currentStep={}, nextStep={}, taskName={}",
                    currentStep, nextStep, taskInfo.getTaskName() );
            taskId = taskDBService.newTask(taskInfo.getRequestId(), nextStep, passingmap);
        }
        log.info("[createTaskRecord]: create new task, taskId=%s, nextStepId=%s, taskName=%s, requestId=%s"
                .formatted(taskId, nextStep.getStepId(), nextStep.getTaskName(), taskInfo.getRequestId()));

    }

    public Context getContext(String taskId) throws IOException {

        return taskDBService.getContext(taskId);
    }

    @Transactional
    public TaskResult processTaskResult(TaskInfo taskInfo, Task currentTask ,TaskResult result, Context context) {

        String output = result.getOutput();
        updateContext(context);

        // TODO support retry later
        switch (result.getTaskStatus()) {
            case COMPLETED -> completeTask(taskInfo ,context, output);
            case RETRY -> retryTask(taskInfo ,context, workflowRegistry.getPollableConfig(currentTask).orElse(new PollableConfig()), output);
            case FAILED -> failTask(taskInfo ,context, context.get("error", String.class), output);
        };

        // Derive the list of next steps, if any

        return result;

    }

    public void updateContext(Context context) {
        taskDBService.updateContext(context);
    }

    @Transactional
    public TaskResult handleError(TaskInfo taskInfo, String errorMessage, Context context) {

        Objects.requireNonNull(errorMessage, "[handleError]: errorMessage is null");
        Objects.requireNonNull(context, "[handleError]: context is null");

        log.error(errorMessage);
        TaskResult result = TaskResult.getInstance(taskInfo.getTaskId(),
                TaskEntity.TaskStatus.FAILED, errorMessage);

        failTask(taskInfo, context, errorMessage, null);

        return result;

    }

}
