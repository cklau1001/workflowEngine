package io.cklau1001.workflow1.config;

import io.cklau1001.workflow1.buyticket.IsTicketBought;
import io.cklau1001.workflow1.wfe.component.Step;
import io.cklau1001.workflow1.wfe.component.SimpleTransition;
import io.cklau1001.workflow1.wfe.component.TernaryTransition;
import io.cklau1001.workflow1.wfe.engine.WorkflowDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class WorkflowConfig {

    @Bean
    public WorkflowDefinition process1() {
        Step step1a = new Step("step1a", "Task1a", List.of (new SimpleTransition(null, "step2a")));
        Step step2a = new Step("step2a", "Task2a", null);

        Map<String, Step> stepMap = Map.of(
                "step1a", step1a,
                "step2a", step2a

        );

        return new WorkflowDefinition("BorrowBookProcess", stepMap, "step1a");

    }

    @Bean
    public WorkflowDefinition createWebServerProcess() {
        Step openWebServerRequest = new Step("OPEN_WEBSERVER_REQUEST",
                "OPEN_WEB_SERVER_REQUEST", List.of(new SimpleTransition(null, "POLL_WEBSERVER_REQUEST")) );
        Step pollWebServerRequest = new Step("POLL_WEBSERVER_REQUEST", "POLL_WEBSERVER_REQUEST", null);

        return new WorkflowDefinition("CreateWebServerProcess",
                List.of(openWebServerRequest, pollWebServerRequest),
                openWebServerRequest.getStepId());

    }

    /**
     * This illustrates how to create concurrent tasks and converge to a condition before moving to the next step.
     * The Pollable interface simulates a long running process that can be completed only after 3 trials.
     *
     * orderMeal >  CheckMealReady     > NotifyReady
     *                  GetDrink
     *                  MakeBurger
     *                  MakeFries
     *
     * CheckMealReady is the leader of the taskgroup (CheckMealReady, GetDrink, MakeBurger, MakeFries)
     *
     * @return
     */
    @Bean
    public WorkflowDefinition makeHamburgerProcess() {



        // first step
        Step orderMealStep = new Step("orderMealStep", "OrderMealTask", List.of(
                new SimpleTransition(null, "CheckMealReadyStep"),
                new SimpleTransition(null, "GetDrinkStep"),
                new SimpleTransition(null, "MakeBurgerStep"),
                new SimpleTransition(null, "MakeFriesStep")
        ));
        Step checkMealReadyStep = new Step("CheckMealReadyStep", "CheckMealReadyTask", List.of(
                new SimpleTransition(null, "NotifyMealStep")
        ));
        /*
           Let GetDrink move to CheckMealReady for burger and fries steps also so that 3 concurrent steps can be
           converged to one CheckMealReadyStep
         */
        Step getDrinkStep = new Step("GetDrinkStep", "GetDrinkTask", null);
        Step makeBurgerStep = new Step("MakeBurgerStep", "MakeBurgerTask", null);
        Step makeFriesStep = new Step("MakeFriesStep", "MakeFriesTask", null);
        // last step
        Step notifyMealStep = new Step("NotifyMealStep", "NotifyMealTask", null);

        return new WorkflowDefinition("OrderBurgeMeal",
                List.of(orderMealStep, getDrinkStep, makeBurgerStep, makeFriesStep, checkMealReadyStep, notifyMealStep),
                "orderMealStep");
    }

    /**
     * Create a workflow to book ticket
     *   bookTicket -->  branchTransition --- success ---> reserveDinner --->  NotifySuccess
     *                                    --- fail ------> NotifyNoTicket
     *
     * @return
     */
    @Bean
    public WorkflowDefinition bookTicketProcess() {

        Step NotifySuccessStep = new Step("NotifySuccessStep", "NotifyReservationTask", null);
        Step NotifyNoTicketStep = new Step("NotifyNoTicketStep", "NotifyReservationTask", null);

        TernaryTransition branchTransition = new TernaryTransition(new IsTicketBought(), "ReserveDinnerStep", "NotifyNoTicketStep" );

        Step ReserveDinnerStep = new Step("ReserveDinnerStep", "ReserveDinnerTask", List.of(
                new SimpleTransition(null, "NotifySuccessStep")
        ));
        Step BookTicketStep = new Step("BookTicketStep", "BookTicketTask", List.of(branchTransition));

        return new WorkflowDefinition("BookTicket",
                List.of(NotifySuccessStep, NotifyNoTicketStep, ReserveDinnerStep, BookTicketStep),
                "BookTicketStep");
    }

}
