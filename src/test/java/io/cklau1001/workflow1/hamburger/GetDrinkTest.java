package io.cklau1001.workflow1.hamburger;

import io.cklau1001.workflow1.wfe.component.Context;
import io.cklau1001.workflow1.wfe.component.TaskResult;
import io.cklau1001.workflow1.wfe.model.TaskEntity;
import io.cklau1001.workflow1.wfe.service.TaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class GetDrinkTest {

    private GetDrink getDrink;

    @Mock
    TaskUtil mockTaskUtil;

    @BeforeEach
    void setup() {
        getDrink = new GetDrink();
    }

    @Test
    void executeTest() {
        Context context = new Context("mock-request-id", "mock-task-id");
        context.put("GetDrinkTask:retries", 5);

        TaskResult result = getDrink.execute(context, mockTaskUtil);

        assertThat(result.getTaskStatus()).isEqualTo(TaskEntity.TaskStatus.RETRY);

        context.put("GetDrinkTask:retries", 2);
        result = getDrink.execute(context, mockTaskUtil);

        assertThat(result.getTaskStatus()).isEqualTo(TaskEntity.TaskStatus.COMPLETED);

    }
}
