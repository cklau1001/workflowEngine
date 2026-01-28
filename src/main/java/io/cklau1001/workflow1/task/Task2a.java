package io.cklau1001.workflow1.task;

import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.Task;
import io.cklau1001.workflow1.wfe.component.TaskResult;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class Task2a implements Task {
    @Override
    public String name() {
        return "Task2a";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {
        log.info("[execute]: complete Task2a, taskId={}", context.getTaskId());
        return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, "done");
    }
}
