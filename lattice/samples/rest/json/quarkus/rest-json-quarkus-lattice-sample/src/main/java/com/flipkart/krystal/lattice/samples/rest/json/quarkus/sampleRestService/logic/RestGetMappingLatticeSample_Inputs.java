package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.facets.InputsForVajram;
import com.flipkart.krystal.facets.VajramInputs;
import com.flipkart.krystal.lattice.ext.rest.api.PathParam;
import com.flipkart.krystal.lattice.ext.rest.api.QueryParam;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse;
import com.flipkart.krystal.model.IfAbsent;

@InputsForVajram(
    parentPackage =
        "com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic",
    vajramId = "RestGetMappingLatticeSample")
public interface RestGetMappingLatticeSample_Inputs extends VajramInputs<JsonResponse> {
  @IfAbsent(FAIL)
  @PathParam
  String fullPath();

  @QueryParam
  String name();

  @QueryParam
  String age();
}
