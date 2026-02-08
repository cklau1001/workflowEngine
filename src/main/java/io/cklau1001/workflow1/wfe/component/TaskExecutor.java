package io.cklau1001.workflow1.wfe.component;

import io.cklau1001.workflow1.wfe.dto.TaskInfo;
import io.cklau1001.workflow1.wfe.engine.WorkflowRegistry;
import io.cklau1001.workflow1.wfe.service.TaskExecutorDBHelper;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * The class to execute a task. For any request or task related database update, TaskExecutorDBHelper can handle that
 * By separating the worker to trigger a user-provided task that may set up its own transaction context, this approach
 * can better separate the transaction context between two.
 *
 */
@Slf4j
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Getter
// @Setter
public class TaskExecutor implements ITaskExecutor, Callable<TaskResult> {

    private final TaskExecutorDBHelper taskExecutorDBHelper;
    private final WorkflowRegistry workflowRegistry;
    private final TaskInfo taskInfo;
    private final TaskUtil taskUtil;
    private final ObservationRegistry observationRegistry;


    public TaskExecutor(TaskExecutorDBHelper taskExecutorDBHelper,
                        WorkflowRegistry workflowRegistry,
                        TaskInfo taskInfo,
                        TaskUtil taskUtil,
                        ObservationRegistry observationRegistry) {

        this.taskExecutorDBHelper = taskExecutorDBHelper;
        this.workflowRegistry = workflowRegistry;
        this.taskInfo = taskInfo;
        this.taskUtil = taskUtil;
        this.observationRegistry = observationRegistry;

    }

    public Context getContext() throws IOException {

        log.info("[getContext]: entered: requestId={}, taskId={}", taskInfo.getRequestId(), taskInfo.getTaskId());
        return taskExecutorDBHelper.getContext(taskInfo.getTaskId());
    }

    public TaskResult call() throws Exception {

        /*
           create a new trace ID
         */
        return Observation.createNotStarted("taskexecutor.call", observationRegistry)
                .highCardinalityKeyValue("requestid", taskInfo.getRequestId())
                //.highCardinalityKeyValue("taskid", taskInfo.getTaskId())
                //.highCardinalityKeyValue("taskname", taskInfo.getTaskName())
                //.highCardinalityKeyValue("workflow", taskInfo.getWorkflowName())
                //.contextualName("taskexecutor." + taskInfo.getTaskName() + "-" + taskInfo.getTaskId())
                 .observeChecked(this::actualcall);
                //.observeChecked(() -> new TaskResult());

    }
    /**
     * Trigger the actual task, which is also the entry
     * The method is called inside a worker thread. Any exception message cannot be printed out. Need log.error() to
     * output the error explicitly.
     *
     * The task should have executing status
     *
     */
    public TaskResult actualcall() throws Exception {

        log.info("[call]: entered: {}", taskInfo);


        // Get the task from taskName
        Optional<Task> optionalTask = workflowRegistry.getTask(taskInfo.getTaskName());
        Context context = getContext();

        if (optionalTask.isEmpty()) {
            String error = "[call]: Unknown task from registry, taskName=%s"
                    .formatted(taskInfo.getTaskName());
            return taskExecutorDBHelper.handleError(taskInfo, error, context);
        }

        Task currentTask = optionalTask.get();
        // add retry information to context
        createRuntimeContext(currentTask, context);

        /*
           Execute the task and mark Failed for any exception caught
         */

        try {
            TaskResult result = currentTask.execute(context, taskUtil);
            return taskExecutorDBHelper.processTaskResult(taskInfo, currentTask, result, context);
        } catch (Exception e) {
            String error = "[call]: issue in executing task, requestId=%s, taskId=%s, taskName=%s, error=%s".formatted(
                    taskInfo.getRequestId(), taskInfo.getTaskId(), taskInfo.getTaskName(), e.getMessage()
            );
            return taskExecutorDBHelper.handleError(taskInfo, error, context);

        }


    }

    @Observed(contextualName = "taskexecutor.createRuntimeContext")
    public void createRuntimeContext(@NonNull Task task, @NonNull Context context) {

        PollableConfig pollableConfig;
        if (task instanceof  Pollable) {
            pollableConfig = ((Pollable) task).pollableConfig();
        } else {
            pollableConfig = new PollableConfig(0, 0);
        }

        log.info("[createRuntimeContext]: retryCount={}, taskId={}, taskName={}",
                taskInfo.getRetryCount(), taskInfo.getTaskId(), taskInfo.getTaskName());

        context.put("retryCount", taskInfo.getRetryCount());
        context.put("retries", pollableConfig.maxRetry() - taskInfo.getRetryCount() + 1);
        context.put("taskId", taskInfo.getTaskId());
    }

    /**
     * Place code to perform any clean up of the prototype bean, if necessary.
     *
     */

    public void destroy() {
        log.info("[destroy]: entered: requestId={}, taskName={} taskId={}",
                taskInfo.getRequestId(), taskInfo.getTaskName(), taskInfo.getTaskId());
    }
}
