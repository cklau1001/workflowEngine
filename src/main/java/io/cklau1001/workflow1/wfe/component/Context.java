package io.cklau1001.workflow1.wfe.component;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * The class to track the local state for each task.
 *
 * Context is better created at task level instead of request level to simplify concurrency management. Each task
 * manage its own context. If a task needs to "read" the context of another task, taskUtil can help.
 *
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class Context {

    @NonNull
    private final String requestId;

    @NonNull
    private final String taskId;

    private final Map<String, Object> ctx = new HashMap<>();

    private Map<String, Object> passingMap;

    public <T> T get(String key, Class<T> type) {

        return type.cast(ctx.get(key));
    }

    public <T> T getOrDefault(String key, Class<T> type, Object defaultValue) {

        return type.cast(ctx.getOrDefault(key, defaultValue));
    }

    public void put(String key, Object data) {
        ctx.put(key, data);
    }

    // only append
    public void update(Map<String, Object> map) {

        if (map == null || map.isEmpty()) {
            return;
        }

        ctx.putAll(map);
    }

    @Override
    public String toString() {
        return "[Context]: requestId=%s, taskId=%s".formatted(requestId, taskId);
    }

    public String showCtx() {return ctx.toString(); }
}
