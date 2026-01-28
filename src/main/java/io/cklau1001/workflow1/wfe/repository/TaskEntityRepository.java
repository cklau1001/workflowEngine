package io.cklau1001.workflow1.wfe.repository;

import io.cklau1001.workflow1.wfe.model.TaskEntity;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskEntityRepository extends JpaRepository<TaskEntity, String> {


    // Use native query to implement LIMIT / OFFSET
    @Query(nativeQuery = true,
            value = "SELECT t.task_id as taskId, t.task_name as taskName, req.request_id as requestId, " +
                    "req.workflow_name as workflowName, t.task_status as taskStatus, t.remark as remark, t.stepId as stepId " +
                    "FROM task_entity t JOIN request_entity req on t.request_id = req.request_id " +
                    "WHERE t.request_id = :requestId " +
                    "ORDER BY t.CREATED_DATE DESC LIMIT 1"
    )
    Tuple getLastTaskByRequestId(@Param("requestId") String requestId);

    /**
     * Find all tasks by a given request ID
     *
     *
     * @param requestId
     * @return
     */
    @Query("SELECT t.taskId as taskId, req.requestId as requestId, req.workflowName as workflowName, " +
            "t.taskName as taskName, t.startTime as startTime, t.endTime as endTime, t.taskStatus as taskStatus, " +
            "t.retryCount as retryCount, t.nextRetryAt as nextRetryAt, t.remark as remark, t.stepId as stepId  " +
            "FROM RequestEntity req INNER JOIN TaskEntity t ON req.requestId = t.request.requestId " +
            "WHERE req.requestId = :requestId ORDER BY t.createdDate ASC")
    List<Tuple> findAllTasksbyRequestId(@Param("requestId") String requestId);


    @Query("SELECT t.taskId as taskId, t.context as context, t.request.requestId as requestId, " +
            "t.taskStatus as taskStatus " +
            "FROM TaskEntity t  " +
            "WHERE t.taskId = :taskId ")
    Tuple getTaskInfoMapByTaskId(@Param("taskId") String taskId);

    @Query("SELECT t.taskId as taskId, t.context as context, t.request.requestId as requestId, " +
            "t.taskStatus as taskStatus " +
            "FROM TaskEntity t  " +
            "WHERE t.taskName = :taskName AND t.request.requestId = :requestId")
    Tuple getTaskInfoMapByRequestIdAndTaskName(@Param("requestId") String requestId, @Param("taskName") String taskName);
}
