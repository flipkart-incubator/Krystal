# Caramel Workflow definition Framework
The caramel project aims to create modern a workflow definition platform which includes a domain specific language which can be used by developers to write complex synchronous/asynchronous workflows/pipelines.
This Workflow DSL will be build on top of exisint high level Programming Langugages (starting with Java) to give developers a familiar development experience.

## Definitions
1.  A workflow is a combination of multiple sync/async computations, each of which could be workflows themselves to any levels of depth forming a workflow tree. 
    The lifecycle of a workflow execution need not be restricted to a single execution context (Process/POD/VM etc.) and can span across multiple execution contexts spread across time.
1.  A **workflow definition** defines the top-down orchestration design of a workflow.
1.  A **channel** is a logical abstraction representing a two-way data-transfer pipe - the forward direction is called an input channel and is used to send a request to a workflow
    and the reverse direction is called an output channel and is used to return the output of the workflow to it's caller. The input and output channels together compose a channel. 
    All workflow receive and return data over channels.

