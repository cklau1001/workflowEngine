package io.cklau1001.workflow1.testdata;

import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.Task;
import io.cklau1001.workflow1.wfe.component.TaskResult;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@NoArgsConstructor
@Component
public class Task1 implements Task {


    @Override
    public String name() {
        return "Task1";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {

        log.info("[execute]: excuting {}, set proceed to true", name());

        context.put("proceed", true);

        return new TaskResult(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, null);
    }
}
