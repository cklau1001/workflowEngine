package io.cklau1001.workflow1.wfe.engine;

import io.cklau1001.workflow1.wfe.component.Step;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Define a workflow with a number of steps and transition
 *
 */
@Slf4j
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowDefinition {

    @NonNull
    private String name;        // name of the workflow

    @NonNull
    private Map<String, Step> steps;    // list of steps and transitions

    @NonNull
    private String startStepId;         // the first step

    public WorkflowDefinition(String name, List<Step> stepList, String startStepId) {

        Map<String, Step> newSteps = new HashMap<>();
        stepList.forEach(s -> newSteps.put(s.getStepId(), s));

        this.name = name;
        this.steps = newSteps;
        this.startStepId = startStepId;
    }

}
