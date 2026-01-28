package io.cklau1001.workflow1.hamburger;

import io.cklau1001.workflow1.wfe.component.*;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotifyMeal implements Task {

    @Override
    public String name() {
        return "NotifyMealTask";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {
        log.info("[NotifyMealTask]: entered: taskId={}, ctx={}", context.getTaskId(), context.showCtx());

        return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, "Meal ready");
    }
}
