# Introduction

Honeycomb is an asynchronous orchestrator which can be used to manage the execution lifecycle of
asynchronous workflows involving hundreds of steps. The core features supported by honeycomb:

1. Workflow instance creation.
1. Managing content of payload of the workflow instance.
1. Understanding individual fields and nested fields in the workflow payload.
1. Tracking workflow instance progress at individual field level in the payload.
1. Support fork-join where one workflow forks into N instances of another workflow and then collects
   the results from all of them.
1. Maintain a call-hierarchy of parent to forked workflow instance.
1. Manage queueing and asynchronous communications among all asynchronous portions of the workflow.
1. Support observability by providing visual interfaces to the following data
   1. Workflow instance progress
   1. Forked workflow instance progress
   1. Parent-child relationships among workflow instance.
   1. Latencies, choke-points etc.