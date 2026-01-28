package io.cklau1001.workflow1.wfe.component;

public record PollableConfig(
        int maxRetry,
        int retryIntervalInMinutes
) {

    public PollableConfig() {
        this(60, 5);
    }

}
