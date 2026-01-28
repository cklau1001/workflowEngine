package io.cklau1001.workflow1.wfe.dto;

import io.cklau1001.workflow1.wfe.model.RequestEntity;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The DTO to capture a new workflow request
 *
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestInfo {

    private String requestId;
    private String workflowName;
    private RequestEntity.RequestStatus requestStatus;
    private List<TaskInfo> taskInfoList;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String remark;

    public static RequestInfo getInstanceFromMap(Map<String, Object> map) {

        return RequestInfo.builder()
                .requestId((String) map.get("requestId"))
                .workflowName((String) map.get("workflowName"))
                .requestStatus((RequestEntity.RequestStatus) map.get("requestStatus"))
                .startTime((LocalDateTime) map.get("startTime"))
                .endTime((LocalDateTime) map.get("endTime"))
                .remark((String) map.get("remark"))
                .build();
    }

    public static RequestInfo getInstanceFromRequestEntity(RequestEntity requestEntity) {

        return RequestInfo.builder()
                .requestId(requestEntity.getRequestId())
                .workflowName(requestEntity.getWorkflowName())
                .startTime(requestEntity.getStartTime())
                .endTime(requestEntity.getEndTime())
                .remark(requestEntity.getRemark())
                .requestStatus(requestEntity.getRequestStatus())
                .build();
    }
}
