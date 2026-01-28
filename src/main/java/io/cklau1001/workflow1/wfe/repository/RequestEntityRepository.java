package io.cklau1001.workflow1.wfe.repository;

import com.fasterxml.jackson.databind.JsonNode;
import io.cklau1001.workflow1.wfe.model.RequestEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Repository
public interface RequestEntityRepository extends JpaRepository<RequestEntity, String> {

    @Query("SELECT req.requestId as requestId, req.workflowName as workflowName, req.requestStatus as requestStatus, " +
            "req.startTime as startTime, req.endTime as endTime, req.remark as remark " +
            "FROM RequestEntity req WHERE req.requestStatus = :requestStatus")
    List<Tuple> findByRequestStatus(@Param("requestStatus") RequestEntity.RequestStatus requestStatus);

    @Query("SELECT req.payload FROM RequestEntity req WHERE req.requestId = :requestId")
    Optional<JsonNode> getPayloadByRequestId(@Param("requestId") String requestId);
}
