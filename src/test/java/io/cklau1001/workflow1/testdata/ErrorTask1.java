package io.cklau1001.workflow1.testdata;

import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.Task;
import io.cklau1001.workflow1.wfe.component.TaskResult;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ErrorTask1 implements Task {
    @Override
    public String name() {
        return "ErrorTask1";
    }

    @Override
    public TaskResult execute(Context context, TaskUtil taskUtil) {

      log.info("[execute]: entered");
      throw new RuntimeException("Mock Exception");
    }
}
