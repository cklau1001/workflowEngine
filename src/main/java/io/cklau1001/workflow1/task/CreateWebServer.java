package io.cklau1001.workflow1.task;

import io.cklau1001.workflow1.wfe.component.*;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CreateWebServer implements Task, Pollable {

    public static String taskName = "OPEN_WEB_SERVER_REQUEST";

    @Override
    public PollableConfig pollableConfig() {
        return new PollableConfig();
    }

    @Override
    public String name() {

        return taskName;
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {

        /*
           TODO
           1 - create n webservers from Context information
           2 - trigger webserver endpoint to create n webservers
           3 - append the execution-id from each into a list
           4 - save the final list of execution-id into context that in turn persist in database
         */
        context.put(name() + "/executionId", "mock-execution-id");
        return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, null);
    }

}
