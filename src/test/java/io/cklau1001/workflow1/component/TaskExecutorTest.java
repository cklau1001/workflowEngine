package io.cklau1001.workflow1.component;

import io.cklau1001.workflow1.wfe.component.*;
import io.cklau1001.workflow1.wfe.dto.TaskInfo;
import io.cklau1001.workflow1.wfe.engine.WorkflowRegistry;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.RequestDBService;
import io.cklau1001.workflow1.wfe.service.TaskDBService;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class TaskExecutorTest {

    @Spy
    @InjectMocks
    private TaskExecutor taskExecutor;

    @Mock
    private TaskDBService mockTaskDBService;

    @Mock
    private RequestDBService mockRequestDBService;

    @Mock
    private WorkflowRegistry mockWorkflowRegistry;

    @Mock
    private TaskInfo mockTaskInfo;

    @Mock
    private Task mockTask;

    @Mock
    private Step currentStep;

    @Mock
    Step nextStep;

    @Test
    void triggerTaskTest() throws Exception {
        // log.info("hello");

        TaskResult mockResult = TaskResult.getInstance("mock-task-id", TaskEntity.TaskStatus.COMPLETED, null);
        Context ctx = new Context("mock-request-id", "mock-task-id");

        when(mockTaskInfo.getTaskName()).thenReturn("mock-task-name");
        when(mockTaskInfo.getWorkflowName()).thenReturn("mock-workflow");
        when(mockTaskInfo.getRequestId()).thenReturn("mock-request-id");
        when(mockTaskInfo.getStepId()).thenReturn("current_step");

        when(nextStep.getStepId()).thenReturn("next_step");
        String nextStepId = nextStep.getStepId();  // To avoid Unfinished stubbing in mockito
        when(currentStep.getNextStepIds(ctx)).thenReturn(List.of(nextStepId));

        when(mockWorkflowRegistry.getStepFromStepId(mockTaskInfo.getWorkflowName(), mockTaskInfo.getStepId(), mockTaskInfo.getRequestId()))
                .thenReturn(Optional.of(currentStep));

        when(mockWorkflowRegistry.getStepFromStepId(mockTaskInfo.getWorkflowName(), nextStep.getStepId(), mockTaskInfo.getRequestId()))
                .thenReturn(Optional.of(nextStep));

        when(mockWorkflowRegistry.getTask(anyString())).thenReturn(Optional.of(mockTask));
        when(mockTask.execute(ctx, any())).thenReturn(mockResult);
        when(mockTaskDBService.newTask("mock-request-id", nextStep, null)).thenReturn("next-task-id");
        when(taskExecutor.getContext()).thenReturn(ctx);

        taskExecutor.call();

        verify(mockTaskDBService, atLeast(1)).newTask("mock-request-id", nextStep, null);
    }

}
