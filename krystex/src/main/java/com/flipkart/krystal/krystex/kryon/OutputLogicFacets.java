package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Request;

record OutputLogicFacets(Request<Object> request, Facets allFacets) {}
