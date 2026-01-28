package io.cklau1001.workflow1.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.engine.WorkflowRegistry;
import io.cklau1001.workflow1.wfe.repository.RequestEntityRepository;
import io.cklau1001.workflow1.wfe.service.RequestDBService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class RequestDBServiceTest {

    @InjectMocks
    private RequestDBService requestDBService;

    @Mock
    private RequestEntityRepository mockRequestEntityRepository;

    @Mock
    private WorkflowRegistry mockWorkflowRegistry;

    private static final ObjectMapper objectMapper = new ObjectMapper();

}
