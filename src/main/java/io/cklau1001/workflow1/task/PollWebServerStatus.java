package io.cklau1001.workflow1.task;

import io.cklau1001.workflow1.wfe.component.*;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PollWebServerStatus implements Task, Pollable {
    @Override
    public PollableConfig pollableConfig() {
        return new PollableConfig();
    }

    @Override
    public String name() {
        return "POLL_WEBSERVER_REQUEST";
    }

    @Override
    @Transactional  // simulate to create a new trasnaction context
    public TaskResult execute(Context context, TaskUtil task) {

        /*
        TODO
        Get the list of execution ids for webserver creation from context
        check the status of each execution and proceed as below :
           FAILED -> Fail this task
           IN-PROGRESS -> return RETRY status

        all-COMPLETED -> return COMPLETE status

         */
        String executionId = context.get(  CreateWebServer.taskName + "/executionId", String.class);

        int retryCount = context.get("retryCount", Integer.class);
        int retries = context.get("retries", Integer.class);

        log.info("[PollWebServerStatus->execute]: retryCount={}, retries={}, executionId={}",
                retryCount ,retries, executionId);

        TaskEntity.TaskStatus taskStatus = retries >= 3 ? TaskEntity.TaskStatus.COMPLETED : TaskEntity.TaskStatus.RETRY;
        return TaskResult.getInstance(context.getTaskId(), taskStatus, "Trail=[%s]: webserver-executionId=%s"
                .formatted(retries ,executionId));
    }
}
