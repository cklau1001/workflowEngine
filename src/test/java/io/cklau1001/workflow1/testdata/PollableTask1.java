package io.cklau1001.workflow1.testdata;

import io.cklau1001.workflow1.wfe.component.*;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@NoArgsConstructor
@Component
public class PollableTask1 implements Task, Pollable {
    @Override
    public PollableConfig pollableConfig() {
        return new PollableConfig(1, 60);
    }

    @Override
    public String name() {
        return "PollableTask1";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {

        log.info("[execute]: entered");
        return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, null);
    }
}
