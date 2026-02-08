package io.cklau1001.workflow1.buyticket;

import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.Task;
import io.cklau1001.workflow1.wfe.component.TaskResult;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ReserveDinner implements Task {


    @Override
    public String name() {
        return "ReserveDinnerTask";
    }

    @Observed(contextualName = "ReserveDinner.execute")
    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {

        Map<String, Object> ctx = context.getCtx();
        if (!ctx.containsKey("mealtime") || !ctx.containsKey("restaurant")) {
            return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.FAILED, "Please provide mealtime and restaurant to book meal");
        }

        Map<String, Object> passingmap = context.get("passing", Map.class);
        passingmap = passingmap == null ? new HashMap<>() : passingmap;

        /*
           pass the task results to next steps

         */
        LocalDateTime mealtime = LocalDateTime.parse(context.get("mealtime", String.class));
        String restaurant = context.get("restaurant", String.class);

        passingmap.put("canReserveDinner", true);
        passingmap.put("mealtime", context.get("mealtime", String.class));
        passingmap.put("restaurant", context.get("restaurant", String.class));

        context.setPassingMap(Map.of("passing", passingmap));

        log.info("[ReserveDinner->execute]: mealtime={}, restaurant={}, requestId={}, taskId={}",
                 mealtime, restaurant, context.getRequestId(), context.getTaskId());

        return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, "Restaurant booked at restaurant=[%s]"
                .formatted(context.get("restaurant", String.class)));
    }
}
