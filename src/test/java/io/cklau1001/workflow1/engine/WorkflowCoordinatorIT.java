package io.cklau1001.workflow1.engine;

import io.cklau1001.workflow1.testdata.ProceedCondition;
import io.cklau1001.workflow1.wfe.component.Condition;
import io.cklau1001.workflow1.wfe.component.Step;
import io.cklau1001.workflow1.wfe.component.TaskExecutor;
import io.cklau1001.workflow1.wfe.component.SimpleTransition;
import io.cklau1001.workflow1.wfe.engine.WorkflowCoordinator;
import io.cklau1001.workflow1.wfe.engine.WorkflowDefinition;
import io.cklau1001.workflow1.wfe.engine.WorkflowRegistry;
import io.cklau1001.workflow1.wfe.service.RequestDBService;
import io.cklau1001.workflow1.wfe.service.TaskDBService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WorkflowCoordinatorIT {

    @Autowired
    private WorkflowRegistry workflowRegistry;

    @Autowired
    private WorkflowCoordinator workflowCoordinator;

    @Autowired
    private RequestDBService requestDBService;

    @Autowired
    private TaskDBService taskDBService;

    @Autowired
    private ObjectProvider<TaskExecutor> taskExecutorProvider;

    @Autowired
    private ExecutorService wfeExecutorService;

    @BeforeEach
    void init() {

        /* programmatically turning on debug logging
           Logger logger = (Logger) LoggerFactory.getLogger("io.cklau1001");
           logger.setLevel(Level.DEBUG);

         */

            Condition proceedCondition = new ProceedCondition();
            SimpleTransition ToStep2 = new SimpleTransition(proceedCondition, "step2");
            // Transition ToStep2 = new Transition(proceedCondition, null);

            Step step1 = new Step("step1", "Task1", List.of(ToStep2));
            // Step step1 = new Step("step1", "ErrorTask1", List.of(ToStep2));
            Step step2 = new Step("step2", "Task2", null);

            Map<String, Step> stepMap = Map.of(
                    "step1", step1,
                    "step2", step2
            );
            WorkflowDefinition step1ToStep2 = new WorkflowDefinition("workflow-1", stepMap, "step1");
            workflowRegistry.addDefinition(step1ToStep2);

    }

    @Test
    void newWorkflowTest() {
        // log.info("hello");

        Map<String, Object> payload = new HashMap<>();
        /*
        payload.put("burger", "large");
        payload.put("fries", "large");
        payload.put("drink", "coke");
         */

        payload.put("filmdate", "2026-01-27");
        payload.put("filmtime", "05:30 PM");
        payload.put("cinema", "cinema1");
        payload.put("mealdate", "2026-01-27");
        payload.put("mealtime", "08:00 PM");
        payload.put("restaurant", "restaurant1");

        // String requestId = workflowCoordinator.newRequest("workflow-1", payload);
        // String requestId = workflowCoordinator.newRequest("CreateWebServerProcess", payload);
        // String requestId = workflowCoordinator.newRequest("OrderBurgeMeal", payload);
        String requestId = workflowCoordinator.newRequest("BookTicket", payload);

        log.info("requestId={}", requestId);

    }

    @Test
    void executeRequestsTest() {
        workflowCoordinator.executeRequests();
    }

    @Test
    void executeTasksTest() throws Exception {
        workflowCoordinator.executeTasks();
        Thread.sleep(3000);
    }

    @Test
    void finalizeRequestsTest() {
        workflowCoordinator.finalizeRequests();
    }

    @Test
    void handleHungRequestsTest() {
        workflowCoordinator.handleHungRequests();
    }

    @Test
    void threadTest() throws ExecutionException, InterruptedException {
        Future<String> result = workflowCoordinator.testThread();
        // log.info("[threadTest]: result={}", result.get());
        // Thread.sleep(6000);
    }
}
