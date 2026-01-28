package io.cklau1001.workflow1.wfe.component;

import io.cklau1001.workflow1.wfe.model.TaskEntity;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * capture the result of a Task
 *
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResult {

    @NonNull
    private String taskId;

    @NonNull
    private TaskEntity.TaskStatus taskStatus;

    private String output;

    public static TaskResult getInstance(String taskId,
                                         TaskEntity.TaskStatus taskStatus,
                                         String output) {
        return new TaskResult(taskId, taskStatus, output);
    }

}
