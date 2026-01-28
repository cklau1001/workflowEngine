# Introduction
Every process is a workflow that is a state transition machine in general. Though there are off-the-shelf package for BPM. This
project is aimed at showing how to implement a reusable workflow engine that can be used by different projects. In this way, 
the engine can help manage the state transition, whilst developers can simply create their own steps, conditions and transitions and plug into this engine and lay down the workflow
declaratively.

# Values
- This implementation only needs to integrate with a database of choice. It can then allow developers to define their
workflow. 
- The engine manage the state transition of each step. Developers can focus on the business logic on top, how one step 
  should be moved to another.
- With workflow status tracked in the database, multiple workflow engine processes can be spawned that allows for horizontal scaling.
- It supports concurrent tasks by default. Serial, parallel workflows or hybrid workflows can be created.

# Idea
- **Workflow definition** defines the list of steps and their transitions from start to the end.
- **Step** defines the work needed and which next step should be based on the condition logic.
  - A step can contain multiple transitions that tell which next step will go. If multiple conditions are met, multiple
    steps will be triggered next, which are parallel work in fact.
- **Transition** defines which step should go next and under what condition. It can return a list of next steps.
- **Condition** defines the logic that evaluate to either true or false based on the task context information.
- **Task** defines the actual work behind a step. In other words, step defines the high-level view of a piece of work and how it moves to the next step. 
  The actual work is a task that is associated with a step. By decoupling a step and task, every step can loosely attach to different task, that
  enhances the flexibility when defining the workflow. A task can be retried muliple times, which is called a Pollable task.
- **Context** holds the runtime information of each task. It facilitates the information sharing among tasks. For example, taskA
  can only continue after taskB store a variableB in its context. A task can only update its own context, though it can read the context of
  other tasks through a well-defined interface. Each context is kept at the task level.
  It supports passing of information to the following ones, which can be a means for information sharing among following steps and the last step.
- **Request** is a real-time execution of a workflow definition that consists of multiple steps defined in the workflow definition. 

# Design
- The workflow engine is modelled based on the idea above and all the task status and context are persisted in a backend database.
- A workflow scheduler is triggered to scan the database and triggered the tasks either in QUEUED or RETRY states.
- Each request and task consist of the status below:
```shell

Request
  QUEUED ----> EXECUTING ---success --> COMPLETED # when all tasks are COMPLETED only
                           OR
                         --- failed --> FAILED  # when any task is FAILED
                         
Task
  QUEUED ----> EXECUTING -- SUCCESS ---> COMPLETED
                  ^             OR
                 RETRY <--- if Pollable  # support retry with maxRetry and RetryInterval
                               OR
                            ---- failed --> FAILED                                         

```
- Each task is executed in a thread. If a step contains multiple next steps, the latter can then be triggered in parallel, making
  concurrent execution of tasks by default.

# How to use
1. Define the workflow that consists of the required steps and their transitions.
2. Create the task that represents the business logic of that step by implementing the Task interface.
3. Create the condition on what runtime information have to be evaluated in a branching decision.
4. Link the condition and the next steps together that forms a transition.
5. Associate all related transitions back to a step that forms part of the workflow.

# Examples
The <package>/config/WorkflowConfig illustrates how to define a workflow, particularly
- **bookTicketProcess** shows how to use TernaryTransition to route to next step based on a boolean condition of ReserveTicket
- **makeHamburgerProcess** shows how to define a task that can be retried ( Pollable )


