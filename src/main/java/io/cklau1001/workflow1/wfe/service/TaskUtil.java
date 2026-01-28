package io.cklau1001.workflow1.wfe.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * A utility class for Task to manage workflow metadata
 *
 */
@Slf4j
@Service
@AllArgsConstructor
public class TaskUtil {

    private final RequestDBService requestDBService;
    private final TaskDBService taskDBService;

    private static ObjectMapper objectMapper = new ObjectMapper();

    public Context getContextByTaskName(String requestId, String taskName) throws IOException {
        return taskDBService.getContextByRequestIdAndTaskName(requestId, taskName);
    }

    public TaskEntity.TaskStatus getTaskStatusByTaskName(String requestId, String taskName) {
        return taskDBService.getTaskStatusByRequestIdAndTaskName(requestId, taskName);
    }

}
