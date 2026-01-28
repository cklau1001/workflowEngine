package io.cklau1001.workflow1.wfe.dto;

import io.cklau1001.workflow1.wfe.model.TaskEntity;
import jakarta.persistence.Tuple;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskInfo {

    private String taskId;
    private String taskName;
    private String requestId;
    private String workflowName;   // definition name
    private String stepId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int retryCount = 0;
    private LocalDateTime nextRetryAt;
    private TaskEntity.TaskStatus taskStatus;

    public static TaskInfo getInstanceFromTaskEntity(TaskEntity taskEntity) {

        Objects.requireNonNull(taskEntity);
        Objects.requireNonNull(taskEntity.getRequest());

        TaskInfo taskInfo = TaskInfo.builder()
                .taskId(taskEntity.getTaskId())
                .stepId(taskEntity.getStepId())
                .taskName(taskEntity.getTaskName())
                .requestId(taskEntity.getRequest().getRequestId())
                .workflowName(taskEntity.getRequest().getWorkflowName())
                .startTime(taskEntity.getStartTime())
                .endTime(taskEntity.getEndTime())
                .retryCount(taskEntity.getRetryCount())
                .nextRetryAt(taskEntity.getNextRetryAt())
                .taskStatus(taskEntity.getTaskStatus())
                .build();

        return taskInfo;
    }

    public static TaskInfo getInstanceFromMap (Map<String, Object> map) {

        int retryCount = map.get("retryCount") == null ? 0 : (Integer)  map.get("retryCount");

        TaskInfo taskInfo = TaskInfo.builder()
                .taskId((String) map.get("taskId"))
                .stepId((String) map.get("stepId"))
                .requestId((String) map.get("requestId"))
                .workflowName((String) map.get("workflowName"))
                .taskName((String) map.get("taskName"))
                .startTime((LocalDateTime) map.get("startTime"))
                .endTime((LocalDateTime) map.get("endTime"))
                .retryCount(retryCount)
                .nextRetryAt((LocalDateTime) map.get("nextRetryAt"))
                .taskStatus((TaskEntity.TaskStatus) map.get("taskStatus"))
                .build();

        return taskInfo;

    }

    @Override
    public String toString() {

        return ("[TaskInfo]: taskId=%s, taskName=%s, requestId=%s, workflowName=%s, taskStatus=%s, " +
                "start=%s, end=%s, retryCount=%s, nextRetryAt=%s").formatted(
                        taskId, taskName, requestId, workflowName, taskStatus, startTime, endTime,
                        retryCount, nextRetryAt
                );
    }
}
