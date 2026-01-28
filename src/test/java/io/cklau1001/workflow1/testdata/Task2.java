package io.cklau1001.workflow1.testdata;

import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.Task;
import io.cklau1001.workflow1.wfe.component.TaskResult;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@NoArgsConstructor
@Component
public class Task2 implements Task {
    @Override
    public String name() {
        return "Task2";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {

        log.info("[execute]: excuting {}", name());
        return new TaskResult(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, null);

    }
}
