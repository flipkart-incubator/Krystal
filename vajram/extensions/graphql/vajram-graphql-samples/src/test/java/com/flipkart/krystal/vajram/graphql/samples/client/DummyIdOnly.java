// Unused: see the note in VajramGraphQlTest.java explaining why the 5b migration of this
// module's tests to the generated <Op>QueryFacade was reverted (JPMS module-info constraints on
// the shared testKrystalModelsGen proc-only task, which compiles src/main/java and src/test/java
// together under this module's single module-info.java, make it infeasible to grant the extra
// test-only `requires` needed by JUnit/AssertJ without broader framework changes out of scope for
// this feature). This file could not be deleted (filesystem delete permission was denied in the
// environment this work was performed in); left as an empty placeholder pending manual cleanup.
package com.flipkart.krystal.vajram.graphql.samples.client;
