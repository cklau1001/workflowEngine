package io.cklau1001.workflow1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.Step;
import io.cklau1001.workflow1.wfe.dto.TaskInfo;
import io.cklau1001.workflow1.wfe.model.RequestEntity;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.repository.TaskEntityRepository;
import io.cklau1001.workflow1.wfe.service.RequestDBService;
import io.cklau1001.workflow1.wfe.service.TaskDBService;
import io.swagger.v3.core.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class TaskDBServiceTest {

    @InjectMocks
    TaskDBService taskDBService;

    @Mock
    TaskEntityRepository mockTaskEntityRepository;

    @Mock
    RequestDBService mockRequestDBService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getContextTest() throws IOException {

        Map<String, Object> ctxmap = Map.of(
                "proceed", true
        );

        JsonNode ctx = objectMapper.valueToTree(ctxmap);
        String taskId = "mock-task-id";


        Context context = taskDBService.getContext(taskId);
        assertThat(context.getTaskId()).isEqualTo(taskId);
        assertThat(context.get("proceed", Boolean.class)).isEqualTo(true);
    }

    @Test
    void updateContextTest() {

        Map<String, Object> taskmap = Map.of("taskId", "mock-task-id");
        TaskInfo taskInfo = TaskInfo.getInstanceFromMap(taskmap);
        TaskEntity taskEntity = TaskEntity.getInstanceFromTaskInfo(taskInfo);

        Map<String, Object> ctxmap = Map.of(
                "proceed", true
        );

        JsonNode ctx = objectMapper.valueToTree(ctxmap);

        taskEntity.setContext(ctx);

        Context context = new Context("mock-request-id", taskInfo.getTaskId());
        context.put("proceed", false);

        when(mockTaskEntityRepository.findById(context.getTaskId())).thenReturn(Optional.of(taskEntity));
        when(mockTaskEntityRepository.save(taskEntity)).thenReturn(taskEntity);

        taskDBService.updateContext(context);

        System.out.printf("context=%s %n", taskEntity.getContext());

        assertThat(taskEntity.getContext().get("proceed").asBoolean()).isEqualTo(false);
    }

    @Test
    void newTask_merge_initialContext() {

        JsonNode payload = objectMapper.valueToTree(Map.of("input1", "value1"));
        RequestEntity requestEntity = RequestEntity.builder()
                .requestId("mock-request-id")
                .requestStatus(RequestEntity.RequestStatus.QUEUED)
                .payload(payload)
                .build();

        Map<String, Object> initialContext = Map.of("passing-prop1", "passing-value1");

        Step firstStep = new Step("firstStep", "firstStepTask", null);
        when(mockRequestDBService.findRequestEntityByRequestId(eq(requestEntity.getRequestId()))).thenReturn(Optional.of(requestEntity));

        taskDBService.newTask(requestEntity.getRequestId(), firstStep, initialContext);
        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(mockTaskEntityRepository, times(1)).save(captor.capture());

        // log.info("taskEntity={}, context={}", captor.getValue(), captor.getValue().getContext());
        JsonNode actualContext = captor.getValue().getContext();
        assertThat(actualContext.get("passing-prop1").asText()).isEqualTo(initialContext.get("passing-prop1"));
        assertThat(actualContext.get("input1")).isEqualTo(payload.get("input1"));


    }
}
