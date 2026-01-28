package io.cklau1001.workflow1.hamburger;

import io.cklau1001.workflow1.wfe.component.*;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class CheckMealReady implements Task, Pollable {

    @Override
    public PollableConfig pollableConfig() {
        return new PollableConfig(20, 5);
    }

    @Override
    public String name() {
        return "CheckMealReadyTask";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {
        log.info("[CheckMealReadyTask]: entered: taskId={}, context={}", context.getTaskId(), context.showCtx());

        TaskEntity.TaskStatus burgerStatus = taskUtil.getTaskStatusByTaskName(context.getRequestId(), "MakeBurgerTask");
        TaskEntity.TaskStatus friesStatus = taskUtil.getTaskStatusByTaskName(context.getRequestId(), "MakeFriesTask");
        TaskEntity.TaskStatus drinkStatus = taskUtil.getTaskStatusByTaskName(context.getRequestId(), "GetDrinkTask");

        boolean burgerReady = burgerStatus == TaskEntity.TaskStatus.COMPLETED;
        boolean friesReady = friesStatus == TaskEntity.TaskStatus.COMPLETED;
        boolean drinkReady = drinkStatus == TaskEntity.TaskStatus.COMPLETED;

        TaskResult taskResult;
        if (burgerReady & friesReady & drinkReady) {
            taskResult = TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, "Meal is ready");
        } else {
            taskResult = TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.RETRY, "Meal is not ready");
        }

        // pass mealReady information to next step (NotifyMeal)
        context.setPassingMap(Map.of("mealReady", true));

        log.info("[CheckMealReadyTask]:  burgerReady={}, friesReady={}, drinkReady={}, taskResult={}",
                burgerReady, friesReady, drinkReady, taskResult.getTaskStatus());
        return taskResult;
    }
}
