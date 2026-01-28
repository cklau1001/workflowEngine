package io.cklau1001.workflow1.wfe.component;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Define the task to be performed and the list of possible transition to various next step based on the data in
 * context
 *
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Step {

    /**
     * a user-friendly step name. A key to look up this instance from workflow definition.
     */
    @NonNull
    private String stepId; // the step name

    /**
     * a user-friendly task name. a key to look up the class implementation from workflow registry
     */
    @NonNull
    private String taskName;  // specify the target user-friendly task name for this step

    /**
     * a list of transitions, if any. Null or empty list means that this is the last step.
     *
     */
    private List<ITransition> transitions;

    /**
     * Get next step from the transitions, if provided
     *
     * @param context
     * @return
     */
    public List<String> getNextStepIds(Context context) {

        log.info("[getNextStepIds]: entered, context={}", context);

        if (transitions == null || transitions.isEmpty()) {
            log.info("[getNextStepIds]: This step is the last step because no transition is provided, " +
                    "returning an empty next step list, requestId={}, step={}", context.getTaskId(), this);
            return List.of();
        }

        List<String> nextStepIds = new ArrayList<>();

        log.debug("[getNextStepIds]: Going to check each transition, transitions-len={}, context={}", transitions.size(), context);
        for (ITransition transition: transitions) {
            List<String> localNextStepList = transition.getNextStepIds(context);
            if (localNextStepList == null || localNextStepList.isEmpty()) {
                log.info("[getNextStepIds]: no next step found, currentStep={}, requestId={}", stepId, context.getRequestId());
                continue;
            }
            nextStepIds.addAll(localNextStepList);
        }

        for (String nextStepId: nextStepIds) {
            log.debug("[getNextStepIds]: currentStepId={}, nextStepId={}", stepId, nextStepId);
        }
        log.debug("[getNextStepIds]:  nextStepIds-len={},context={}", nextStepIds.size() ,context);
        return nextStepIds;

    }

    @Override
    public String toString() {
        return "[Step]: stepId=%s, taskName=%s".formatted(stepId, taskName);
    }
}
