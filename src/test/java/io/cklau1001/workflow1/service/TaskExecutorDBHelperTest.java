package io.cklau1001.workflow1.service;

import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.PollableConfig;
import io.cklau1001.workflow1.wfe.dto.TaskInfo;
import io.cklau1001.workflow1.wfe.engine.WorkflowRegistry;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.RequestDBService;
import io.cklau1001.workflow1.wfe.service.TaskDBService;
import io.cklau1001.workflow1.wfe.service.TaskExecutorDBHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class TaskExecutorDBHelperTest {

    @InjectMocks
    private TaskExecutorDBHelper taskExecutorDBHelper;

    @Mock
    private TaskDBService mockTaskDBService;

    @Mock
    private RequestDBService mockRequestDBService;

    @Mock
    private WorkflowRegistry mockWorkflowRegistry;

    private TaskInfo taskInfo;

    @BeforeEach
    void setUp() {
        taskInfo = TaskInfo.builder()
                .taskId("mock-task-id")
                .requestId("mock-request-id")
                .taskName("mock-task-name")
                .taskStatus(TaskEntity.TaskStatus.EXECUTING)
                .retryCount(3)
                .build();
    }

    /**
     * verify the retry interval is PollableConfig->retryIntervallinMillis afterwards
     *
     */
    @Test
    void retryTask_whenRetryNotExhausted() {

        PollableConfig pollableConfig = new PollableConfig(3, 5);
        Context context = new Context(taskInfo.getRequestId(), taskInfo.getTaskId());

        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);

        taskExecutorDBHelper.retryTask(taskInfo, context, pollableConfig, "output");
        verify(mockTaskDBService, times(1))
                .markRetryFromExecuting(eq(taskInfo.getTaskId()), captor.capture(), eq("output"));

        log.info("[retry]: currentTime={}, retryTime={}", currentTime, captor.getValue());

        assertThat(captor.getValue()).isAfter(currentTime.plusMinutes(pollableConfig.retryIntervalInMinutes()));
    }

    @Test
    void retryTask_whenRetryExhausted() {

        PollableConfig pollableConfig = new PollableConfig(3, 5);
        taskInfo.setRetryCount(0);
        Context context = new Context(taskInfo.getRequestId(), taskInfo.getTaskId());

        taskExecutorDBHelper.retryTask(taskInfo, context, pollableConfig, "output");
        verify(mockRequestDBService, times(1)).markFailed(eq(taskInfo.getRequestId()), anyString());
        verify(mockTaskDBService, times(1)).markFailed(eq(taskInfo.getTaskId()), anyString());
    }
}
