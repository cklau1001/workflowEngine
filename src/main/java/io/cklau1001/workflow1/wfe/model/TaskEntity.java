package io.cklau1001.workflow1.wfe.model;

import io.cklau1001.workflow1.wfe.dto.TaskInfo;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.scheduling.config.Task;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@Slf4j
@Entity
@Table(name = "TASK_ENTITY", indexes = {
    @Index(name = "idx_task_taskstatus", columnList = "taskStatus")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEntity extends BaseEntity {

    public enum TaskStatus {
        QUEUED,
        EXECUTING,
        SUSPENDED, // TODO: To be implemented
        RETRY,
        COMPLETED,
        FAILED;
    }
    @Id
    @Column(name = "TASK_ID")
    private String taskId;

    @Column(name = "STEP_ID")
    private String stepId;    // the step that represents this task

    @ManyToOne
    @JoinColumn(name = "requestId")
    private RequestEntity request;

    @Column(name = "TASK_NAME")
    private String taskName;    // task name is the same as step name which can identify the list of Transitions

    @Column(name = "START_TIME")
    private LocalDateTime startTime;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "TASK_STATUS")
    private TaskStatus taskStatus;

    @Column(name = "NEXT_RETRY_AT")
    private LocalDateTime nextRetryAt;   // if task supports retry

    @Column(name = "RETRY_COUNT")
    private Integer retryCount;     // initialize by the max retry trials and decrement per retry

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "TASK_OUTPUT", columnDefinition = "jsonb")
    private JsonNode taskOutput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    private JsonNode context;  // store the context of a workflow request

    @Column(name = "remark", length = 2000)
    private String remark;


    public static TaskEntity getInstanceFromTaskInfo(TaskInfo taskInfo) {

        RequestEntity request = RequestEntity.builder().requestId(taskInfo.getRequestId()).build();

        TaskEntity task = TaskEntity.builder()
                .taskId(taskInfo.getTaskId())
                .taskName(taskInfo.getTaskName())
                .startTime(taskInfo.getStartTime())
                .endTime(taskInfo.getEndTime())
                .retryCount(taskInfo.getRetryCount())
                .nextRetryAt(taskInfo.getNextRetryAt())
                .request(request)
                .taskStatus(taskInfo.getTaskStatus())
                .build();

        return task;
    }

    @Override
    public String toString() {
        return "[TaskEntity]: taskId=%s, taskName=%s, requestId=%s, stepId=%s, taskStatus=%s, start=%s, end=%s, retryCount=%s, nextRetryAt=%s"
                .formatted(taskId, taskName, getRequest().getRequestId(), stepId, taskStatus ,startTime, endTime, retryCount, nextRetryAt);
    }

}
