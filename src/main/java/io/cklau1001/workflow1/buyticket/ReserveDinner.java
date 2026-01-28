package io.cklau1001.workflow1.buyticket;

import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.Task;
import io.cklau1001.workflow1.wfe.component.TaskResult;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ReserveDinner implements Task {


    @Override
    public String name() {
        return "ReserveDinnerTask";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {
        Map<String, Object> passingmap = context.get("passing", Map.class);
        passingmap = passingmap == null ? new HashMap<>() : passingmap;

        /*
           pass the task results to next steps

         */
        passingmap.put("canReserveDinner", true);
        passingmap.put("mealdate", context.get("mealdate", String.class));
        passingmap.put("mealtime", context.get("mealtime", String.class));
        passingmap.put("restaurant", context.get("restaurant", String.class));

        context.setPassingMap(Map.of("passing", passingmap));

        log.info("[ReserveDinner->execute]: requestId={}, taskId={}",
                 context.getRequestId(), context.getTaskId());

        return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, "Restaurant booked at restaurant=[%s]"
                .formatted(context.get("restaurant", String.class)));
    }
}
