package io.cklau1001.workflow1.engine;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.cklau1001.workflow1.testdata.PollableTask1;
import io.cklau1001.workflow1.testdata.Task1;
import io.cklau1001.workflow1.wfe.component.Step;
import io.cklau1001.workflow1.wfe.component.TaskExecutor;
import io.cklau1001.workflow1.wfe.component.TaskResult;
import io.cklau1001.workflow1.wfe.dto.RequestInfo;
import io.cklau1001.workflow1.wfe.dto.TaskInfo;
import io.cklau1001.workflow1.wfe.engine.WorkflowCoordinator;
import io.cklau1001.workflow1.wfe.engine.WorkflowRegistry;
import io.cklau1001.workflow1.wfe.model.RequestEntity;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.RequestDBService;
import io.cklau1001.workflow1.wfe.service.TaskDBService;
import io.cklau1001.workflow1.wfe.service.TaskExecutorDBHelper;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.mockito.Mockito.*;


@Slf4j
@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
public class WorkflowCoordinatorTest {

    private ListAppender<ILoggingEvent> listAppender;

    @InjectMocks
    private WorkflowCoordinator workflowCoordinator;

    @Mock
    private RequestDBService mockRequestDBService;

    @Mock
    private TaskDBService mockTaskDBService;

    @Mock
    private TaskExecutorDBHelper mockTaskExecutorDBHelper;

    @Mock
    private WorkflowRegistry mockWorkflowRegistry;

    @Mock
    private ObjectProvider<TaskExecutor> mockTaskExecutorProvider;

    @Mock
    private ExecutorService mockWfeExecutorService;

    @Mock
    private TaskExecutor mockTaskExectuor;

    @Test
    void newRequestTest() {
        // log.info("hello");

        RequestInfo requestInfo = RequestInfo.builder()
                .requestId("mock-request-id")
                .workflowName("mock-workflow-1")
                .build();

        when(mockRequestDBService.newRequest(requestInfo.getWorkflowName(), any())).thenReturn("mock-request-id");

        assertThat(workflowCoordinator.newRequest(requestInfo.getWorkflowName(), any())).isEqualTo("mock-request-id");
    }

    @Test
    void executeRequestsTest() {

        RequestInfo requestInfo = RequestInfo.builder()
                .requestId("mock-request-id")
                .workflowName("mock-workflow-1")
                .requestStatus(RequestEntity.RequestStatus.QUEUED)
                .build();

        Step firstStep = new Step("step1", "PollableTask1", null);
        PollableTask1 pollableTask1 = new PollableTask1();

        when(mockWorkflowRegistry.getTask(firstStep.getTaskName())).thenReturn(Optional.of(pollableTask1));
        when(mockWorkflowRegistry.getFirstStep(requestInfo.getWorkflowName(), requestInfo.getRequestId()))
                .thenReturn(Optional.of(firstStep));
        when(mockTaskDBService.newTask("mock-request-id", firstStep, null, 0)).thenReturn("mock-task-id");
        when(mockRequestDBService.findAllPendingRequests(Integer.MAX_VALUE)).thenReturn(List.of(requestInfo));

        workflowCoordinator.executeRequests();

        // requestDBService.markExecutingFromQueued(requestId);
        verify(mockRequestDBService, atLeast(1)).markExecutingFromQueued(requestInfo.getRequestId());
    }

    @Test
    void finalizeCompletedRequestsTest()
    {
        RequestInfo requestInfo = RequestInfo.builder()
                .requestId("mock-request-id")
                .requestStatus(RequestEntity.RequestStatus.EXECUTING)
                .workflowName("mock-workflow")
                .build();

        List<TaskInfo> taskInfoList = List.of(
                TaskInfo.builder().taskId("task-id-1").taskName("Task-1").taskStatus(TaskEntity.TaskStatus.COMPLETED).build(),
                TaskInfo.builder().taskId("task-id-2").taskName("Task-2").taskStatus(TaskEntity.TaskStatus.COMPLETED).build()
        );

        // requestDBService.findAllRequestsByStatus(RequestEntity.RequestStatus.EXECUTING);
        when(mockRequestDBService.findAllRequestsByStatus(RequestEntity.RequestStatus.EXECUTING)).thenReturn(List.of(requestInfo));
        when(mockTaskDBService.getAllTasksByRequestId(requestInfo.getRequestId())).thenReturn(taskInfoList);

        workflowCoordinator.finalizeRequests();

        verify(mockRequestDBService, atLeast(1)).markSuccess(requestInfo.getRequestId());
    }

    @Test
    void finalizeFailedRequestsTest()
    {
        RequestInfo requestInfo = RequestInfo.builder()
                .requestId("mock-request-id")
                .requestStatus(RequestEntity.RequestStatus.EXECUTING)
                .workflowName("mock-workflow")
                .build();

        List<TaskInfo> taskInfoList = List.of(
                TaskInfo.builder().taskId("task-id-1").taskName("Task-1").taskStatus(TaskEntity.TaskStatus.COMPLETED).build(),
                TaskInfo.builder().taskId("task-id-2").taskName("Task-2").taskStatus(TaskEntity.TaskStatus.FAILED).build()
        );

        // requestDBService.findAllRequestsByStatus(RequestEntity.RequestStatus.EXECUTING);
        when(mockRequestDBService.findAllRequestsByStatus(RequestEntity.RequestStatus.EXECUTING)).thenReturn(List.of(requestInfo));
        when(mockTaskDBService.getAllTasksByRequestId(requestInfo.getRequestId())).thenReturn(taskInfoList);

        workflowCoordinator.finalizeRequests();
        verify(mockRequestDBService, atLeast(1)).markFailed(eq(requestInfo.getRequestId()), anyString());
    }

    @Test
    void finalizeRunningRequestsTest()
    {
        RequestInfo requestInfo = RequestInfo.builder()
                .requestId("mock-request-id")
                .requestStatus(RequestEntity.RequestStatus.EXECUTING)
                .workflowName("mock-workflow")
                .build();

        List<TaskInfo> taskInfoList = List.of(
                TaskInfo.builder().taskId("task-id-1").taskName("Task-1").taskStatus(TaskEntity.TaskStatus.COMPLETED).build(),
                TaskInfo.builder().taskId("task-id-2").taskName("Task-2").taskStatus(TaskEntity.TaskStatus.EXECUTING).build()
        );

        // requestDBService.findAllRequestsByStatus(RequestEntity.RequestStatus.EXECUTING);
        when(mockRequestDBService.findAllRequestsByStatus(RequestEntity.RequestStatus.EXECUTING)).thenReturn(List.of(requestInfo));
        when(mockTaskDBService.getAllTasksByRequestId(requestInfo.getRequestId())).thenReturn(taskInfoList);

        workflowCoordinator.finalizeRequests();
        verify(mockRequestDBService, times(0)).markFailed(eq(requestInfo.getRequestId()), any());

    }

    /*
    @BeforeEach
    void beforeQueueTest() {
        // log.info("Before beforeQueueTest....");

        Logger logger = (Logger) LoggerFactory.getLogger("io.cklau1001");
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

     */

    @Test
    void finalizeQueuedRequestsTest(CapturedOutput capturedOutput)
    {
        RequestInfo requestInfo = RequestInfo.builder()
                .requestId("mock-request-id")
                .requestStatus(RequestEntity.RequestStatus.QUEUED)
                .workflowName("mock-workflow")
                .build();


        // requestDBService.findAllRequestsByStatus(RequestEntity.RequestStatus.EXECUTING);
        when(mockRequestDBService.findAllRequestsByStatus(RequestEntity.RequestStatus.EXECUTING)).thenReturn(List.of(requestInfo));
        when(mockTaskDBService.getAllTasksByRequestId(requestInfo.getRequestId())).thenReturn(List.of());

        workflowCoordinator.finalizeRequests();
        verify(mockRequestDBService, times(0)).markFailed(eq(requestInfo.getRequestId()), any());

        /*
            method-1 : to assert log messages by listAppender

        List<ILoggingEvent> loggingEvents = listAppender.list;
        assertThat(loggingEvents).extracting(ILoggingEvent::getFormattedMessage).anyMatch(l -> l.contains("SKIP"));


         */

        /*
          method-2 : by CapturedOutput
         */

        assertThat(capturedOutput.getOut()).contains("SKIP");
    }

    @Test
    void executeTasksTest(CapturedOutput capturedOutput) throws Exception {
        TaskInfo taskInfo = TaskInfo.builder()
                .taskId("mock-task-id")
                .taskName("mock-task")
                .taskStatus(TaskEntity.TaskStatus.QUEUED)
                .build();

        when(mockTaskDBService.getAllPendingTasks(eq(true), eq(Integer.MAX_VALUE))).thenReturn(List.of(taskInfo));
        when(mockTaskExecutorProvider
                .getObject(eq(mockTaskExecutorDBHelper), eq(mockWorkflowRegistry), eq(taskInfo)))
                .thenReturn(mockTaskExectuor);

        // need to use doAnswer() for testing a lambda inside submit()

        doAnswer(invocationOnMock -> {
            return CompletableFuture.completedFuture(TaskResult.getInstance(taskInfo.getTaskId(), TaskEntity.TaskStatus.COMPLETED, null));
        }).when(mockWfeExecutorService).submit(any(Callable.class));
/*
        when(mockWfeExecutorService.submit(any(Runnable.class)))
                .thenReturn(CompletableFuture.completedFuture(TaskResult.getInstance(taskInfo.getTaskId(), TaskEntity.TaskStatus.COMPLETED, null)));
*/
        workflowCoordinator.executeTasks();
        assertThat(capturedOutput.getOut()).contains("Task invoked");
    }
}
