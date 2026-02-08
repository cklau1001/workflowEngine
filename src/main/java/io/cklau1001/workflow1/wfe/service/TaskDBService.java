package io.cklau1001.workflow1.wfe.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.Step;
import io.cklau1001.workflow1.wfe.component.Task;
import io.cklau1001.workflow1.wfe.component.TaskExecutor;
import io.cklau1001.workflow1.wfe.dto.RequestInfo;
import io.cklau1001.workflow1.wfe.dto.TaskInfo;
import io.cklau1001.workflow1.wfe.engine.WorkflowRegistry;
import io.cklau1001.workflow1.wfe.model.RequestEntity;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.repository.TaskEntityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.util.TupleBackedMap;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;



@Slf4j
@Component
@RequiredArgsConstructor
public class TaskDBService {

    private final RequestDBService requestDBService;
    private final TaskEntityRepository taskEntityRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    /**
     * Create a new task for a given requestId and step
     *
     * @param requestId
     * @param firstStep
     * @return
     */
    @Transactional
    public String newTask(@NotNull String requestId, @NotNull Step firstStep, Map<String, Object> initialContext) {

        return newTask(requestId, firstStep, initialContext, 0);
    }

    /**
     * Create a new record for a Pollable task
     *
     * @param requestId
     * @param firstStep
     * @param retryCount
     * @return
     */

    @Transactional
    public String newTask(@NotNull String requestId, @NotNull Step firstStep, Map<String, Object> initialContext, int retryCount) {

        Objects.requireNonNull(requestId, "[newTask]: requestId is null");
        Objects.requireNonNull(firstStep, "[newTask]: firstStep is null");

        Optional<RequestEntity> requestEntityOptional = requestDBService.findRequestEntityByRequestId(requestId);


        RequestEntity requestEntity = requestEntityOptional.orElseThrow(() ->
                new IllegalArgumentException("Unable to find request of the given requestId, requestId=%s"
                        .formatted(requestId)));

        TaskEntity taskEntity = TaskEntity.builder()
                .taskId(UUID.randomUUID().toString())
                .request(requestEntity)
                .taskStatus(TaskEntity.TaskStatus.QUEUED)
                .stepId(firstStep.getStepId())
                .taskName(firstStep.getTaskName())
                .retryCount(retryCount)
                .nextRetryAt(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)) // invoke at once for new Pollable also
                .build();

        if (initialContext == null) {
            taskEntity.setContext(requestEntity.getPayload());
        } else {
            TypeReference<Map<String, Object>> typeref = new TypeReference<>() {};
            Map<String, Object> payloadMap = objectMapper.convertValue(requestEntity.getPayload(), typeref);
            payloadMap.putAll(initialContext);

            JsonNode ctx = objectMapper.valueToTree(payloadMap);
            taskEntity.setContext(ctx);
        }
        taskEntityRepository.save(taskEntity);
        return taskEntity.getTaskId();
    }

    public void markSuccess(String taskId, String remark) {

        Objects.requireNonNull(taskId, "[markSuccess]: taskId is null");

        log.info("[markSuccess]: entered, taskId={}, remark={}", taskId, remark);

        TaskEntity task = getTaskEntity(taskId);
        addRemark(task, remark);
        task.setEndTime(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        task.setTaskStatus(TaskEntity.TaskStatus.COMPLETED);

        taskEntityRepository.save(task);
    }

    public void markFailed(String taskId, String remark) {

        Objects.requireNonNull(taskId, "[markFailed]: taskId is null");
        TaskEntity task = getTaskEntity(taskId);
        addRemark(task, remark);
        task.setEndTime(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        task.setTaskStatus(TaskEntity.TaskStatus.FAILED);

        taskEntityRepository.save(task);
    }

    public void markExecutingFromQueued(String taskId, int retryCount) {

        Objects.requireNonNull(taskId, "[markExecutingFromQueued]: taskId is null");
        TaskEntity task = getTaskEntity(taskId);
        task.setStartTime(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        task.setTaskStatus(TaskEntity.TaskStatus.EXECUTING);
        task.setRetryCount(retryCount);

        taskEntityRepository.save(task);

    }

    public void markRetryFromExecuting(String taskId, LocalDateTime nextRetryAt, String remark) {

        Objects.requireNonNull(taskId, "[markRetryFromExecuting]: taskId is null");
        TaskEntity task = getTaskEntity(taskId);

        if (task.getRetryCount() - 1 <= 0) {
            String error = "[markRetryFromExecuting]: All retries have been exhausted, taskId=%s".formatted(taskId);
            log.error(error);
            markFailed(taskId, error);
            return;
        }

        addRemark(task, remark);
        task.setRetryCount(task.getRetryCount() - 1);
        task.setNextRetryAt(nextRetryAt);
        task.setTaskStatus(TaskEntity.TaskStatus.RETRY);

        taskEntityRepository.save(task);

    }

    public void markExecutingFromRetry(String taskId) {

        Objects.requireNonNull(taskId, "[markExecutingFromRetry]: taskId is null");
        TaskEntity task = getTaskEntity(taskId);
        task.setTaskStatus(TaskEntity.TaskStatus.EXECUTING);

        taskEntityRepository.save(task);
    }

    public void suspendTask(String taskId, String remark) {

        Objects.requireNonNull(taskId, "[suspendTask]: taskId is null");
        TaskEntity task = getTaskEntity(taskId);
        addRemark(task, remark);
        taskEntityRepository.save(task);
    }

    public void resumeTask(String taskId, String remark) {

        Objects.requireNonNull(taskId, "[resumeTask]: taskId is null");
        TaskEntity task = getTaskEntity(taskId);
        addRemark(task, remark);
        task.setTaskStatus(TaskEntity.TaskStatus.QUEUED);

        taskEntityRepository.save(task);
    }

    private TaskEntity getTaskEntity(String taskId) {

        Objects.requireNonNull(taskId, "[getTaskEntity]: taskId is null");
        Optional<TaskEntity> optionalTask = taskEntityRepository.findById(taskId);
        TaskEntity task = optionalTask.orElseThrow(() -> new IllegalArgumentException("Unknown taskId, taskId=%s"
                .formatted(taskId)));
        return task;
    }

    private void addRemark(TaskEntity task, String remark) {

        if (remark != null) {
            String originalRemark = task.getRemark() == null ? "" : task.getRemark();
            task.setRemark(originalRemark.isEmpty() ? remark : originalRemark + "\n" + remark);
        }

    }

    public Optional<TaskInfo> getTaskByTaskId(String taskId) {
        Objects.requireNonNull(taskId, "[getTaskByTaskId]: taskId is null");
        Optional<TaskEntity> taskEntityOptional = taskEntityRepository.findById(taskId);

        TaskInfo taskInfo = null;

        if (taskEntityOptional.isPresent()) {
            taskInfo = TaskInfo.getInstanceFromTaskEntity(taskEntityOptional.get());
        }

        return Optional.ofNullable(taskInfo);

    }

    public Optional<TaskInfo> getLastTaskByRequestId(String requestId) {

        Objects.requireNonNull(requestId, "[getLastTaskByRequestId]: requestId is null");

        Tuple resultset = taskEntityRepository.getLastTaskByRequestId(requestId);
        TaskInfo taskInfo = null;
        if (resultset != null) {
            taskInfo = TaskInfo.getInstanceFromMap(new TupleBackedMap(resultset));
        }

        return Optional.ofNullable(taskInfo);
    }

    /**
     * Get a list of QUEUED or RETRY tasks that update them to EXECUTING as appropriate
     * Since db update is needed, the Task entity is a better choice for subsequent update to save DB connection
     * align with the ORM model.
     *
     * @param markExeuting  - true if the queued tasks have to be updated to executing also
     * @return
     */
    @Transactional
    public List<TaskInfo> getAllPendingTasks(boolean markExeuting, int rowsToFetch) {

        log.info("[getAllPendingTasks]: entered, rowsToFetch={}", rowsToFetch);

        // Get all records if rowsToFetch is not set properly
        rowsToFetch = rowsToFetch <= 0 ? Integer.MAX_VALUE : rowsToFetch;

        // Use JOIN FETCH to avoid N+1 query issue
        String sql = "SELECT t " +
                "FROM TaskEntity t JOIN FETCH t.request req WHERE t.taskStatus in ('QUEUED', 'RETRY') " +
                "ORDER BY t.createdDate";

        sql = rowsToFetch == Integer.MAX_VALUE ? sql : sql + " LIMIT " + rowsToFetch + " OFFSET 0";

        TypedQuery<TaskEntity> query = entityManager.createQuery(sql, TaskEntity.class);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

        List<TaskEntity> executingSet = new ArrayList<>();
        LocalDateTime currentTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        for (TaskEntity taskEntity: query.getResultList()) {

            // check nextRetryAt for Pollable tasks
            if (taskEntity.getNextRetryAt() != null && taskEntity.getNextRetryAt().isAfter(currentTime)) {
                log.info("[getAllPendingTasks]: This Pollable task is not ready to run - SKIP, currentTime=[{}], taskEntity=[{}]",
                        currentTime, taskEntity);
                continue;
            }

            if (markExeuting) {
                taskEntity.setTaskStatus(TaskEntity.TaskStatus.EXECUTING);
                taskEntity.setStartTime(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
                taskEntityRepository.save(taskEntity);
                log.debug("[getAllPendingTasks]: update task to EXECUTING status, taskId={}, taskName={}, requestId={}",
                        taskEntity.getTaskId(), taskEntity.getTaskName(), taskEntity.getRequest().getRequestId());
            }

            executingSet.add(taskEntity);
        }

        // debug
        executingSet.forEach(t -> log.debug("[getAllPendingTasks]: EXECUTING_TASK_ENTITY, {}", t));
        List<TaskInfo> taskInfoList = executingSet.stream().map(TaskInfo::getInstanceFromTaskEntity).toList();

        return taskInfoList;
    }


    public List<TaskInfo> getAllTasksByRequestId(String requestId) {
        Objects.requireNonNull(requestId);

        List<Tuple> resultset = taskEntityRepository.findAllTasksbyRequestId(requestId);

        // debug
        resultset.forEach(t -> t.getElements()
                .forEach((e) -> log.debug("[getAllTasksByRequestId]: {}={}", e.getAlias(),t.get(e.getAlias()))));

        return resultset.stream().map(t -> TaskInfo.getInstanceFromMap(new TupleBackedMap(t))).toList();

    }

    @Transactional
    public List<TaskInfo> markHungTasksFailed(int rowsToFetch, int threshold) {

        log.info("[markHungTasksFailed]: entered, rowsToFetch={}, threshold={}", rowsToFetch, threshold);

        // Get all records if rowsToFetch is not set properly
        rowsToFetch = rowsToFetch <= 0 ? Integer.MAX_VALUE : rowsToFetch;

        // Use JOIN FETCH to avoid N+1 query issue
        String sql = "SELECT t " +
                "FROM TaskEntity t JOIN FETCH t.request req " +
                "WHERE t.taskStatus = 'EXECUTING' AND t.startTime < :minStartTime " +
                "ORDER BY t.createdDate ASC";

        sql = rowsToFetch == Integer.MAX_VALUE ? sql : sql + " LIMIT " + rowsToFetch + " OFFSET 0";
        LocalDateTime earliestStart = LocalDateTime.ofInstant(Instant.now().minus(threshold, ChronoUnit.MINUTES), ZoneOffset.UTC);

        log.debug("[markHungTasksFailed]: earliestStart={}, sql={}", earliestStart, sql);
        TypedQuery<TaskEntity> query = entityManager.createQuery(sql, TaskEntity.class);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        query.setParameter("minStartTime", earliestStart);

        List<TaskEntity> resultset = query.getResultList();
        for (TaskEntity taskEntity: resultset) {
            taskEntity.setTaskStatus(TaskEntity.TaskStatus.FAILED);
            taskEntity.setEndTime(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
            taskEntityRepository.save(taskEntity);
            log.debug("[markHungTasksFailed]: mark [FAILED] on hung task, taskId=[{}], taskName=[{}], requestId=[{}]",
                    taskEntity.getTaskId(), taskEntity.getTaskName(), taskEntity.getRequest().getRequestId());
        }

        return resultset.stream().map(TaskInfo::getInstanceFromTaskEntity).toList();
    }

    public Context getContext(String taskId) throws IOException {

        log.info("[getContext]: entered: taskId={}", taskId);
        Objects.requireNonNull(taskId, "[getContext]: taskId is null");

        // Optional<JsonNode> contextOptional = taskEntityRepository.getContextByTaskId(taskId);
        Tuple taskmap = taskEntityRepository.getTaskInfoMapByTaskId(taskId);

        if (taskmap == null) {
            throw new IllegalArgumentException("[getContext]: No such task, taskId=%s".formatted(taskId));
        }

        Context context = new Context(taskmap.get("requestId", String.class), taskId);

        if (taskmap.get("context") != null) {
            JsonNode jsonNode = taskmap.get("context", JsonNode.class);
            TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
            Map<String, Object> ctx = objectMapper.readValue(objectMapper.treeAsTokens(jsonNode), typeRef);
            context.update(ctx);
        } else {
            log.info("[getContext]: No context found in database, taskId={}", taskId);
        }

        log.info("[getContext]: context created, requestId={}, taskId={}", context.getRequestId(), context.getTaskId());
        return context;

    }

    public Tuple getTaskInfoMapByRequestIdAndTaskName(String requestId, String taskName) {

        log.info("[getTaskInfoMapByRequestIdAndTaskName]: entered: requestId={}, taskName={}", requestId, taskName);

        Objects.requireNonNull(requestId, "[getTaskInfoMapByRequestIdAndTaskName]: requestId is null");
        Objects.requireNonNull(taskName, "[getTaskInfoMapByRequestIdAndTaskName]: taskName is null");

         Tuple taskmap = taskEntityRepository.getTaskInfoMapByRequestIdAndTaskName(requestId, taskName);

         if (taskmap == null) {
             throw new IllegalArgumentException(
                     "[getTaskInfoByRequestIdAndTaskName]: No such task from the given requestId and taskName, requestId=%s, taskName=%s".formatted(requestId, taskName));
         }

         return taskmap;
    }

    public Context getContextByRequestIdAndTaskName(String requestId, String taskName) throws IOException {

        Tuple taskmap = getTaskInfoMapByRequestIdAndTaskName(requestId, taskName);

        Context context = new Context(requestId, taskmap.get("taskId", String.class));

        if (taskmap.get("context") != null) {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
            Map<String, Object> ctx = objectMapper.readValue(objectMapper.treeAsTokens(taskmap.get("context", JsonNode.class)), typeRef);
            context.update(ctx);
        }

        return context;
    }

    public TaskEntity.TaskStatus getTaskStatusByRequestIdAndTaskName(String requestId, String taskName)  {

        Tuple taskmap = getTaskInfoMapByRequestIdAndTaskName(requestId, taskName);

        return taskmap.get("taskStatus", TaskEntity.TaskStatus.class);

    }

    public void updateContext(Context context) {

        log.info("[updateContext]: entered: requestId={}, taskId={}", context.getRequestId(), context.getTaskId());
        Objects.requireNonNull(context.getTaskId(), "[updateContext]: getTaskID() is null");
        Optional<TaskEntity> taskEntityOptional = taskEntityRepository.findById(context.getTaskId());

        TaskEntity task = taskEntityOptional.orElseThrow(() -> new IllegalArgumentException("No such task, taskId=%s"
                .formatted(context.getTaskId())));

        JsonNode jsonNode = objectMapper.valueToTree(context.getCtx());
        task.setContext(jsonNode);

        taskEntityRepository.save(task);
    }

}
