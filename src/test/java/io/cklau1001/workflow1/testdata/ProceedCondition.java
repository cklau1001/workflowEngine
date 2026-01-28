package io.cklau1001.workflow1.testdata;

import io.cklau1001.workflow1.wfe.component.Condition;
import io.cklau1001.workflow1.wfe.component.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProceedCondition implements Condition {

    @Override
    public String id() {
        return "ProceedCondition";
    }

    @Override
    public boolean evaluate(Context context) {

        boolean result = (boolean) context.getCtx().getOrDefault("proceed", false);
        log.debug("[evaluate]: result={}, requestId={}", result, context.getTaskId());
        return result;
    }
}
