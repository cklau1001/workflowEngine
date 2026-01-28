package io.cklau1001.workflow1.wfe.component;

import java.io.IOException;

/**
 * To enable final keyword on TaskExecutor that turn it into a template pattern and allow Spring proxy to work
 * properly with @Transactional, switch to JDK dynamic proxy by defining the interface below that can return the
 * TaskResult
 *
 */
public interface ITaskExecutor {

    public TaskResult call() throws Exception;
}
