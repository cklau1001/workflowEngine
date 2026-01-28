package io.cklau1001.workflow1.buyticket;

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
public class NotifyReservation implements Task {


    @Override
    public String name() {
        return "NotifyReservationTask";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {

        Map<String, Object> passingmap = context.get("passing", Map.class);

        if (passingmap == null || passingmap.isEmpty()) {
            return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.FAILED, "No passingmap found");
        }

        if ((boolean) passingmap.getOrDefault("canReserveTicket", false)) {
            log.info("[NotifyReservation->execute]: ticket reserved, requestId={}", context.getRequestId());
        } else {
            log.info("[NotifyReservation->execute]: Sorry, cannot reserve the ticket, requestId={}", context.getRequestId());
        }

        if ((boolean) passingmap.getOrDefault("canReserveDinner", false)) {
            log.info("[NotifyReservation->execute]: Dinner reserved, requestId={}", context.getRequestId());
        } else {
            log.info("[NotifyReservation->execute]: Sorry, cannot reserve dinner, requestId={}", context.getRequestId());
        }

        return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED, null);
    }
}
