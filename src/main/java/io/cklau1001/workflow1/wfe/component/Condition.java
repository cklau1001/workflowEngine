package io.cklau1001.workflow1.wfe.component;

/**
 * Define the interface to evaluate a condition based on the context value. If the result is true, transition to
 * next step
 *
 */
public interface Condition {

    public String id();   // return the condition name
    public boolean evaluate(Context context);

    public default boolean check(Context context) {
        if (context == null) return false;
        return evaluate(context);
    }
}
