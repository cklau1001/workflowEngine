package io.cklau1001.workflow1.wfe.component;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Model the step transition that trueStep should go if condition is true, or falseStep otherwise like the ternary
 * operator
 *   condition ? trueStep : falseStep
 *
 */
@Slf4j
@Getter
public class TernaryTransition implements ITransition {

    private Condition condition;

    @NonNull
    private String trueStep;

    private String falseStep;

    public TernaryTransition(Condition condition, String trueStep, String falseStep) {
        if (trueStep == null) {
            throw new IllegalArgumentException("[TernaryTransition]: trueStep cannot be null");
        }

        this.condition = condition;
        this.trueStep = trueStep;
        this.falseStep = falseStep;
    }

    @Override
    public List<String> getNextStepIds(Context context) {

        List<String> nextStepList = null;
        if (condition != null && condition.check(context)) {
            nextStepList = List.of(trueStep);

        } else if (falseStep != null) {
            nextStepList = List.of(falseStep);
        }

        log.info("[getNextStepIds]: nextStepList={}, requestId={}, taskId={}",
                nextStepList, context.getRequestId(), context.getTaskId());
        return nextStepList;
    }
}
