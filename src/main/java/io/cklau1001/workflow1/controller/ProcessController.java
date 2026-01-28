package io.cklau1001.workflow1.controller;

import io.cklau1001.workflow1.dto.BorrowBookRequest;
import io.cklau1001.workflow1.dto.BorrowBookResponse;
import io.cklau1001.workflow1.wfe.engine.WorkflowCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProcessController {

    private final WorkflowCoordinator workflowCoordinator;

    @PostMapping("/book/borrow")
    public ResponseEntity<?> borrowBook(@RequestBody BorrowBookRequest borrowBookRequest) {

        String requestId = workflowCoordinator.newRequest("BorrowBookProcess", borrowBookRequest);


        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new BorrowBookResponse(requestId, borrowBookRequest));
    }
}
