package io.cklau1001.workflow1.engine;

import io.cklau1001.workflow1.testdata.ProceedCondition;
import io.cklau1001.workflow1.testdata.Task1;
import io.cklau1001.workflow1.testdata.Task2;
import io.cklau1001.workflow1.wfe.component.*;
import io.cklau1001.workflow1.wfe.engine.WorkflowDefinition;
import io.cklau1001.workflow1.wfe.engine.WorkflowRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class WorkflowRegistryTest {

    private WorkflowRegistry workflowRegistry;

    @BeforeEach
    void init() {

        /* programmatically turning on debug logging
           Logger logger = (Logger) LoggerFactory.getLogger("io.cklau1001");
           logger.setLevel(Level.DEBUG);

         */

        Condition proceedCondition = new ProceedCondition();
        SimpleTransition ToStep2 = new SimpleTransition(proceedCondition, "step2");

        Step step1 = new Step("step1", "Task1", List.of(ToStep2));
        Step step2 = new Step("step2", "Task2", null);

        Task1 task1 = new Task1();
        Task2 task2 = new Task2();

        Map<String, Step> stepMap = Map.of(
                "step1", step1,
                "step2", step2
        );
        WorkflowDefinition step1ToStep2 = new WorkflowDefinition("Step1ToStep2", stepMap, "step1");

        workflowRegistry = new WorkflowRegistry(List.of(task1, task2), List.of(), List.of(step1ToStep2));
        // workflowRegistry.addDefinition(step1ToStep2);
    }

    @Test
    void workflowRegistryTest() {
        log.info("test1");

        Context context = new Context("mock-req-id", "mock-task-id");
        context.put("proceed", true);

        Optional<Step> firstStep1 = workflowRegistry.getFirstStep("Step1ToStep2", "mock-req-id");
        assertThat(firstStep1.isPresent()).isEqualTo(true);
        assertThat(firstStep1.orElseThrow().getTaskName()).isEqualTo("Task1");
        assertThat(firstStep1.orElseThrow().getTransitions().size()).isEqualTo(1);
        assertThat(firstStep1.orElseThrow().getNextStepIds(context).size()).isEqualTo(1);
        assertThat(firstStep1.orElseThrow().getNextStepIds(context).getFirst()).isEqualTo("step2");


    }
}
