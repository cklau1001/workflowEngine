package io.cklau1001.workflow1.wfe.component;

import java.util.List;

/**
 * The interface to define how a step is moved to another. Currently, SimpleTransition and TernaryTransitions
 * are provided that should cater for all scenarios. Nevertheless, one can implement this interface that return
 * multiple next steps accordingly.
 *
 *
 */
public interface ITransition {

    /**
     * Return a list of next step Ids based on the current values in context
     *
     * @param context
     * @return
     */
    public List<String> getNextStepIds(Context context);
}
