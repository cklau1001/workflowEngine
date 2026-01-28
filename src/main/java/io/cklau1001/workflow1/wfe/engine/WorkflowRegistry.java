package io.cklau1001.workflow1.wfe.engine;

import io.cklau1001.workflow1.wfe.component.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
// @RequiredArgsConstructor
public class WorkflowRegistry {

    private final Map<String, Task> taskMap = new HashMap<>();
    private final Map<String, Condition> conditionMap = new HashMap<>();
    private final Map<String, WorkflowDefinition> definitionMap = new HashMap<>();

    // inject list of tasks and conditions defined by users
    public WorkflowRegistry(List<Task> tasklist,
                            List<Condition> conditionList,
                            List<WorkflowDefinition> definitionList) {

        tasklist.forEach(t -> taskMap.put(t.name(), t));
        conditionList.forEach(condition -> conditionMap.put(condition.id(), condition));
        definitionList.forEach(d -> definitionMap.put(d.getName(), d));

        definitionMap.forEach((key, value) -> log.debug("[WorkflowRegistry->new]: WORKFLOW: {}={}", key, value));
        taskMap.forEach((key, value) -> log.debug("[WorkflowRegistry->new]: TASK: {}={}", key, value));
    }

    public void addDefinition(WorkflowDefinition definition) {

        definitionMap.put(definition.getName(), definition);
    }

    public Optional<WorkflowDefinition> getDefinition(String definitionName) {
        return Optional.ofNullable(definitionMap.get(definitionName));
    }

    public Optional<Task> getTask(String taskName) {
        return Optional.ofNullable(taskMap.get(taskName));
    }

    public Optional<Condition> getCondition(String conditionId) {
        return Optional.of(conditionMap.get(conditionId));
    }

    public Optional<Step> getStepFromStepId(String workflowName, String stepId, String requestId) {

        Objects.requireNonNull(workflowName, "workflowName is null");
        Objects.requireNonNull(stepId, "stepId is null");
        Objects.requireNonNull(requestId, "requestId is null");

        log.debug("[getStepFromStepId]: entered, workflowName={}, stepId={}, requestId={}",
                workflowName, stepId, requestId);
        Optional<WorkflowDefinition> definitionOptional = getDefinition(workflowName);
        WorkflowDefinition definition = definitionOptional.orElseThrow(() -> new IllegalArgumentException(
                "[getStepFromTaskInfo]: unable to get definition, workflowName=%s, stepId=%s, requestId=%s"
                        .formatted(workflowName, stepId, requestId)
        ));

        Step step = definition.getSteps().get(stepId);

        log.debug("[getStepFromStepId]: step obtained, step={}", step);
        return Optional.ofNullable(step);

    }

    public Optional<Step> getFirstStep(String workflowName, String requestId) {

        Objects.requireNonNull(workflowName, "workflowName is null");
        Objects.requireNonNull(requestId, "requestId is null");


        Optional<WorkflowDefinition> definitionOptional = getDefinition(workflowName);
        WorkflowDefinition definition = definitionOptional.orElseThrow(() -> new IllegalArgumentException(
                "[getFirstStep]: unable to get definition, workflowName=%s, requestId=%s"
                        .formatted(workflowName, requestId)
        ));

        String firstStepId = definition.getStartStepId();
        Step firstStep = definition.getSteps().get(firstStepId);

        return Optional.ofNullable(firstStep);

    }

    public Optional<PollableConfig> getPollableConfig(@NonNull Task task) {

        return Optional.ofNullable(isPollableTask(task) ? ((Pollable) task).pollableConfig() : null);

    }

    public boolean isPollableTask(@NonNull Task task) {
        return task instanceof Pollable && ((Pollable) task).pollableConfig() != null;
    }


}
