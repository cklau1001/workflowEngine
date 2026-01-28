package io.cklau1001.workflow1.hamburger;

import io.cklau1001.workflow1.wfe.component.*;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MakeFries implements Task, Pollable {
    @Override
    public PollableConfig pollableConfig() {
        return new PollableConfig(5, 3);
    }

    @Override
    public String name() {
        return "MakeFriesTask";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {
        log.info("[MakeFriesTask]: entered: taskId={}, ctx={}", context.getTaskId(), context.showCtx());

        int retries = context.getOrDefault("retries", Integer.class, 1);
        TaskResult result = retries < 3 ? TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.RETRY, "MakeFriesTask retry") :
                TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, "French Fries received");
        log.info("[MakeFriesTask]: END: taskResult={}, ctx={}", result.getTaskStatus(), context.showCtx());
        return result;

    }
}
