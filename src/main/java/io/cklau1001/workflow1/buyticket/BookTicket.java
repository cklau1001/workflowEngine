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
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
public class BookTicket implements Task {

    private Random random = new Random();

    @Override
    public String name() {
        return "BookTicketTask";
    }

    @Observed(contextualName = "BookTicket.execute")
    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {

        Map<String, Object> ctx = context.getCtx();
        if (!ctx.containsKey("filmtime") || !ctx.containsKey("cinema")) {
            return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.FAILED, "Please provide date, time and place to reserve tickets");
        }

        // String filmdate = (String) ctx.get("filmdate");
        LocalDateTime filmtime =  LocalDateTime.parse((String) ctx.get("filmtime"));
        String cinema = (String) ctx.get("cinema");

        boolean canReserveTicket = random.nextBoolean();

        context.put("canReserveTicket", canReserveTicket);

        // pass the following information to next step
        Map<String, Object> passingmap = Map.of(
                "passing", Map.of("canReserveTicket", canReserveTicket,
                        "filmtime", filmtime,
                        "cinema", cinema
                        )
        );

        context.setPassingMap(passingmap);

        log.info("[BookTicket->execute]: canReserveTicket={}, filetime={}, requestId={}, taskId={}",
                canReserveTicket, filmtime, context.getRequestId(), context.getTaskId());
        return TaskResult.getInstance(context.getTaskId(), TaskEntity.TaskStatus.COMPLETED,
                "ticket booked at cinema=[%s]".formatted(cinema));
    }

}
