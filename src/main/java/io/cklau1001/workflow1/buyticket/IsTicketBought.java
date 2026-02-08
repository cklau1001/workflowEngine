package io.cklau1001.workflow1.buyticket;

import io.cklau1001.workflow1.wfe.component.Condition;
import io.cklau1001.workflow1.wfe.component.Context;

public class IsTicketBought implements Condition {
    @Override
    public String id() {
        return "IsTicketBought";
    }

    @Override
    public boolean evaluate(Context context) {
        return context.getOrDefault("canReserveTicket", Boolean.class, false);

    }
}
