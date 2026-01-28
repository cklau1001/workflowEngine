package io.cklau1001.workflow1.hamburger;

import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.Task;
import io.cklau1001.workflow1.wfe.component.TaskResult;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderMeal implements Task {
    @Override
    public String name() {
        return "OrderMealTask";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {
        log.info("[OrderMealTask]: entered: taskId={}, context={}", context.getTaskId(), context.showCtx());
        return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, "Meal ordered");
    }
}
