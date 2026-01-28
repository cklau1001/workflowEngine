package io.cklau1001.workflow1.wfe.component;

import io.cklau1001.workflow1.wfe.service.TaskDBService;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Define the actual work to be performed that can be linked up with the step.
 *
 */

public  interface Task {


    public String name();  // return user friendly of task name
    public TaskResult execute(Context context, TaskUtil taskUtil);

}
