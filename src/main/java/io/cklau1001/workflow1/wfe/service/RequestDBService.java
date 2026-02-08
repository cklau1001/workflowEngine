package io.cklau1001.workflow1.wfe.service;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.dto.RequestInfo;
import io.cklau1001.workflow1.wfe.engine.WorkflowDefinition;
import io.cklau1001.workflow1.wfe.engine.WorkflowRegistry;
import io.cklau1001.workflow1.wfe.model.RequestEntity;
import io.cklau1001.workflow1.wfe.repository.RequestEntityRepository;
import io.micrometer.observation.annotation.Observed;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.util.TupleBackedMap;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestDBService {

    // https://github.com/spring-projects/spring-framework/issues/15076
    // private final EntityManagerFactory emf;
    // EntityManager entityManager = emf.createSharedEntityManager(emf);

    private final EntityManager entityManager;

    private final RequestEntityRepository requestEntityRepository;
    private final WorkflowRegistry workflowRegistry;
    private final ObjectMapper objectMapper;

    // private static final ObjectMapper objectMapper = new ObjectMapper();


    public void markFailed(String requestId, String remark) {

        Objects.requireNonNull(requestId, "[markFailed]: requestId is null");
        RequestEntity request = getRequestEntity(requestId);

        if (remark != null) {
            String originalRemark = request.getRemark() == null ? "" : request.getRemark();
            request.setRemark(originalRemark.isEmpty() ? remark : originalRemark + "\n" + remark);
        }
        request.setEndTime(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        request.setRequestStatus(RequestEntity.RequestStatus.FAILED);
        requestEntityRepository.save(request);

    }

    public void markSuccess(String requestId) {

        Objects.requireNonNull(requestId, "[markSuccess]: requestId is null");
        RequestEntity request = getRequestEntity(requestId);
        request.setEndTime(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        request.setRequestStatus(RequestEntity.RequestStatus.COMPLETED);

        requestEntityRepository.save(request);

    }

    private RequestEntity getRequestEntity(String requestId) {

        Objects.requireNonNull(requestId, "[getRequestEntity]: requestId is null");
        Optional<RequestEntity> requestEntityOptional = requestEntityRepository.findById(requestId);
        RequestEntity request = requestEntityOptional.orElseThrow(() -> new IllegalArgumentException("Unknown request, requestId=%s"
                .formatted(requestId)));

        return request;
    }

    /**
     * Start a new workflow request of a given workflow name, set status to QUEUED
     *
     * @param workflowName
     * @return
     */
    @Observed(contextualName = "RequestDBService.newRequest")
    public String newRequest(String workflowName, Object payload) {

        Objects.requireNonNull(workflowName, "[newRequest]: workflowName is null");

        Optional<WorkflowDefinition> definitionOptional = workflowRegistry.getDefinition(workflowName);
        if (definitionOptional.isEmpty()) {
            throw new IllegalArgumentException("[newRequest]: un-recognized workflowName, workflowName=%s"
                    .formatted(workflowName));
        }

        JsonNode jsonPayload = payload != null ? objectMapper.valueToTree(payload) : null;

        RequestEntity request = new RequestEntity();
        request.setRequestId(UUID.randomUUID().toString());
        request.setWorkflowName(workflowName);
        request.setRequestStatus(RequestEntity.RequestStatus.QUEUED);
        request.setStartTime(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        request.setPayload(jsonPayload);

        requestEntityRepository.save(request);

        log.info("[newRequest]: request created, workflowName={}, requestId={}", request.getWorkflowName(), request.getRequestId());
        return request.getRequestId();
    }

    public void markExecutingFromQueued(String requestId) {

        Objects.requireNonNull(requestId, "[markExecutingFromQueued]: requestId is null");

        Optional<RequestEntity> requestEntityOptional = findRequestEntityByRequestId(requestId);
        RequestEntity requestEntity = requestEntityOptional.orElseThrow(() ->
                new IllegalArgumentException("[markExecutingFromQueued]: No such request, requestId=%s"
                        .formatted(requestId)));

        requestEntity.setRequestStatus(RequestEntity.RequestStatus.EXECUTING);
        requestEntity.setStartTime(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        requestEntityRepository.save(requestEntity);
    }

    /**
     * Get all QUEUED requests and locking them for update
     *
     * @return
     */
    @Observed(contextualName = "requestDBService.findAllPendingRequests")
    @Transactional
    public List<RequestInfo> findAllPendingRequests(int rowsToFetch) {

        log.info("[findAllPendingRequests]: entered: rowsToFetch={}", rowsToFetch);

        // Get all records if rowsToFetch is not set properly
        rowsToFetch = rowsToFetch <= 0 ? Integer.MAX_VALUE : rowsToFetch;

        /*
        Strictly speaking, fetching a RequestEntity, followed by subsequent status update is another alternative.
        Since first task has to be created and there are a number of validation, better to get a subset of result.

        The big rule: ensure short-lived database objects for consistency

         */
        String sql = "SELECT req.requestId as requestId, req.workflowName as workflowName, req.requestStatus as requestStatus, " +
                "req.startTime as startTime, req.endTime as endTime, req.remark as remark " +
                "FROM RequestEntity req WHERE req.requestStatus = :requestStatus " +
                "ORDER BY req.createdDate ASC";

        sql = rowsToFetch == Integer.MAX_VALUE ? sql : sql + " LIMIT " + rowsToFetch + " OFFSET 0";

        TypedQuery<Tuple> query = entityManager.createQuery(sql, Tuple.class);
        query.setParameter("requestStatus", RequestEntity.RequestStatus.QUEUED);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<Tuple> results = query.getResultList();

        List<Map<String, Object>> resultset = new ArrayList<>();
        for (Tuple tuple: results) {
            Map<String, Object> resultMap = new HashMap<>();
            for (TupleElement<?> element: tuple.getElements()) {
                log.debug("[findAllPendingRequests]: TUPLE {}={}", element.getAlias(), tuple.get(element.getAlias()));

                if (tuple.get(element.getAlias()) != null) {
                    resultMap.put(element.getAlias(), tuple.get(element.getAlias()));
                }
            }
            resultset.add(resultMap);
        }

        // List<Map<String, Object>> resultset = requestEntityRepository.findAllPendingRequests();
        resultset.forEach(e -> e.forEach(
                (p,v) -> log.debug("[findAllPendingRequests]: {}={}", p, v))
        );
        return resultset.stream().map(RequestInfo::getInstanceFromMap).toList();

    }

    public List<RequestInfo> findAllRequestsByStatus(RequestEntity.RequestStatus requestStatus) {

        Objects.requireNonNull(requestStatus, "[findAllRequestsByStatus]: requestStatus is null");
        List<Tuple> resultset = requestEntityRepository.findByRequestStatus(requestStatus);

        // debug
        resultset.forEach(t -> t.getElements()
                .forEach(te -> log.debug("[findAllRequestsByStatus]: {}={}", te.getAlias(), t.get(te.getAlias()))));

        return resultset.stream().map(t -> RequestInfo.getInstanceFromMap(new TupleBackedMap(t))).toList();

    }

    public Optional<RequestEntity> findRequestEntityByRequestId(String requestId) {

        Objects.requireNonNull(requestId, "[findRequestEntityByRequestId]: requestId is null");
        return requestEntityRepository.findById(requestId);
    }

    public Optional<RequestInfo> findRequestByRequestId(String requestId) {

        Objects.requireNonNull(requestId, "[findRequestByRequestId]: requestId is null");
        Optional<RequestEntity> requestEntityOptional = findRequestEntityByRequestId(requestId);

        RequestInfo result = requestEntityOptional.map(RequestInfo::getInstanceFromRequestEntity).orElse(null);

        return Optional.ofNullable(result);
    }

    public Optional<JsonNode> getPayloadByRequestId(String requestId) {

        Objects.requireNonNull(requestId, "[getPayloadByRequestId]: requestId cannot be null");

        return requestEntityRepository.getPayloadByRequestId(requestId);

    }

    @Observed(contextualName = "RequestDBService.getRequestDetailsByRequestId")
    public Optional<RequestInfo> getRequestDetailsByRequestId(String requestId) {
        Objects.requireNonNull(requestId, "[getRequestDetailsByRequestId]: requestId cannot be null");

        Optional<RequestEntity> requestEntityOptional = requestEntityRepository.findById(requestId);

        return requestEntityOptional.map(RequestInfo::getInstanceFromRequestEntity).or(Optional::empty);

    }

    public void setRemark(String requestId, String remark) {

        Objects.requireNonNull(requestId, "[setRemark]: requestId is null");
        Objects.requireNonNull(remark, "[setRemark]: remark is null");

        Optional<RequestEntity> requestEntityOptional = findRequestEntityByRequestId(requestId);
        RequestEntity requestEntity = requestEntityOptional.orElseThrow(() ->
                new IllegalArgumentException("[setRemark]: No such request, requestId=%s"
                        .formatted(requestId)));

        requestEntity.setRemark(remark);
        requestEntityRepository.save(requestEntity);

    }
}
