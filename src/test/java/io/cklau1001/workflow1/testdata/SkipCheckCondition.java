package io.cklau1001.workflow1.testdata;

import io.cklau1001.workflow1.wfe.component.Condition;
import io.cklau1001.workflow1.wfe.component.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SkipCheckCondition implements Condition {
    @Override
    public String id() {
        return "SkipCheckCondition";
    }

    @Override
    public boolean evaluate(Context context) {

        boolean result = (boolean) context.getCtx().getOrDefault("skip", false);

        log.debug("[evaluate]: result={}, requestId={}", result, context.getTaskId());
        return result;

    }
}
