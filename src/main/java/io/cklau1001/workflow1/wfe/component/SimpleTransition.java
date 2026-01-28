package io.cklau1001.workflow1.wfe.component;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * Model the step that transition to next step if either condition is null or condition is true.
 * No further step returned otherwise.
 *
 */
@Slf4j
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SimpleTransition implements ITransition {

    /**
     * Condition to evaluate before moving to next step.
     * If it is null, meaning that the workflow can move to next step directly.
     */
    private Condition condition;

    @NonNull
    private String nextStepId;

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SimpleTransition that)) return false;

        return that.condition.equals(this.condition) && that.nextStepId.equals(this.nextStepId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nextStepId, condition);
    }

    @Override
    public String toString() {
        return "[Transition]: nextStepId=%s, condition=%s".formatted(nextStepId, condition);
    }

    @Override
    public List<String> getNextStepIds(Context context) {

        List<String> nextStepIdList = null;
        if (condition == null || condition.check(context)) {
            nextStepIdList = List.of(nextStepId);
        }

        log.info("[getNextStepIds]: nextStepIdList={}, requestId={}, taskId={}",
                nextStepIdList, context.getRequestId(), context.getTaskId());
        return nextStepIdList;
    }
}
