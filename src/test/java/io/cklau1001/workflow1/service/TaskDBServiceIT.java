package io.cklau1001.workflow1.service;

import io.cklau1001.workflow1.wfe.service.TaskDBService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TaskDBServiceIT {

    @Autowired
    TaskDBService taskDBService;

    @Test
    void getContextTest() throws IOException {

        taskDBService.getContext("5e6423d8-23d9-4a15-b24d-4bf7b177d18d");
    }

    @Test
    void getTaskStatusByRequestIdAndTaskNameTest() {

        // requestId=d2a9ea50-b198-4051-a065-937f27044969, taskId=5e6423d8-23d9-4a15-b24d-4bf7b177d18d
        taskDBService.getTaskStatusByRequestIdAndTaskName("d2a9ea50-b198-4051-a065-937f27044969", "MakeBurgerTask");
    }
}
