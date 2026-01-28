package io.cklau1001.workflow1.wfe.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.cklau1001.workflow1.wfe.dto.RequestInfo;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Entity
@Table(name = "REQUEST_ENTITY", indexes = {
        @Index(name = "idx_request_requeststatus", columnList = "requestStatus")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestEntity extends BaseEntity {

    public enum RequestStatus {
        QUEUED,
        EXECUTING,
        SUSPENDED, // TODO: To be implemented
        COMPLETED,
        FAILED
    }
    @Id
    @Column(name = "REQUEST_ID")
    private String requestId;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskEntity> tasks = new ArrayList<>();

    @Column(name = "WORKFLOW_NAME")
    private String workflowName;   // the workflow definition ID

    @Column(name = "START_TIME")
    private LocalDateTime startTime;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "REQUEST_STATUS")
    private RequestStatus requestStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "remark", length = 2000)
    private String remark;

    public void addTask(TaskEntity taskEntity) {

        Objects.requireNonNull(taskEntity);
        log.info("[addTask]: entered, taskId={}, taskName={}", taskEntity.getTaskId(), taskEntity.getTaskName());
        taskEntity.setRequest(this);

        getTasks().add(taskEntity);
    }

    @Override
    public String toString() {

        return "[RequestEntity]: requestId=%s, workflowName=%s, requestStatus=%s, start=%s, end=%s".formatted(
                requestId, workflowName, requestStatus, startTime, endTime
        );
    }

    public static RequestEntity getInstanceFromRequestInfo(RequestInfo requestInfo) {

        RequestEntity requestEntity = RequestEntity.builder()
                .requestId(requestInfo.getRequestId())
                .requestStatus(requestInfo.getRequestStatus())
                .workflowName(requestInfo.getWorkflowName())
                .startTime(requestInfo.getStartTime())
                .endTime(requestInfo.getEndTime())
                .remark(requestInfo.getRemark())
                .build();
        return requestEntity;
    }
}
