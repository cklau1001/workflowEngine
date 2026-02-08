package io.cklau1001.workflow1.controller;

import io.cklau1001.workflow1.aop.TimerWorkflow;
import io.cklau1001.workflow1.dto.BookTicketRequest;
import io.cklau1001.workflow1.dto.BorrowBookRequest;
import io.cklau1001.workflow1.dto.BorrowBookResponse;
import io.cklau1001.workflow1.dto.ProcessResponse;
import io.cklau1001.workflow1.wfe.dto.RequestInfo;
import io.cklau1001.workflow1.wfe.engine.WorkflowCoordinator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.TraceContext;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProcessController {

    private final WorkflowCoordinator workflowCoordinator;
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    @Observed(contextualName = "ProcessController.healthcheck")
    @GetMapping("/keepalive")
    public ResponseEntity<String> healthcheck() {

        TraceContext context = tracer.currentTraceContext().context();
        String traceId = context != null ? context.traceId() : "No-trace-id";
        log.info("[healthcheck]: traceId={}", traceId);
        return ResponseEntity.ok("traceId=" +traceId);
    }

    @Observed(contextualName = "ProcessController.borrowBook")
    @PostMapping("/book/borrow")
    public ResponseEntity<BorrowBookResponse> borrowBook(@RequestBody BorrowBookRequest borrowBookRequest) {

        String requestId = workflowCoordinator.newRequest("BorrowBookProcess", borrowBookRequest);


        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new BorrowBookResponse(requestId, borrowBookRequest));
    }

    @Observed(contextualName = "ProcessController.bookTicket")
    @TimerWorkflow(value = "bookticket.frequency", workflow = "bookticket")
    @PostMapping("/bookticket")
    public ResponseEntity<ProcessResponse> bookTicket(@RequestBody BookTicketRequest bookTicketRequest) {
        String requestId = workflowCoordinator.newRequest("BookTicket", bookTicketRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ProcessResponse(requestId, "BookTicket"));

    }

    @Observed(contextualName = "ProcessController.getRequestInfo")
    @GetMapping("/bookticket/{requestId}")
    public ResponseEntity<RequestInfo> getRequestInfo(@PathVariable("requestId") String requestId) {

        RequestInfo requestInfo = workflowCoordinator.getRequestInfo(requestId);

        return ResponseEntity.ok(requestInfo);
    }

}
