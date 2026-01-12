package com.flipkart.krystal.vajram.graphql.samples.seller;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.samples.input.SellerInput;

/**
 * Vajram to fetch Seller ID(s) from SellerInput based on filtering criteria.
 *
 * <p>This is used with @idFetcher directive. It parses SellerInput, applies filtering logic, and
 * returns SellerId(s), which the framework then uses to automatically fetch the full Seller
 * entity(ies).
 *
 * <p><strong>Pattern:</strong> Input Type → Filter/Parse → Return ID(s) → Framework fetches
 * Entity(ies)
 */
@Vajram
public abstract class FetchSellerId extends ComputeVajramDef<SellerId> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    // Custom input type - the aggregator extracts and coerces this from GraphQL arguments
    @IfAbsent(FAIL)
    SellerInput input;
  }

  /**
   * Parses SellerInput and returns SellerId based on filtering criteria.
   *
   * <p>This method demonstrates:
   *
   * <ul>
   *   <li>Accessing all SellerInput fields (sellerId, startDate, endDate, requestId)
   *   <li>Applying filtering logic based on input criteria
   *   <li>Returning a SellerId object that the framework uses to fetch the full Seller entity
   * </ul>
   *
   * <p>In a real implementation, this would:
   *
   * <ul>
   *   <li>Query a database/search service based on the input criteria
   *   <li>Apply date range filtering
   *   <li>Return matching seller IDs
   * </ul>
   *
   * @param input The seller input containing filtering criteria
   * @return SellerId extracted/calculated from the input
   */
  @Output
  static SellerId fetchSellerId(SellerInput input) {
    // Extract sellerId from input (required field)
    String sellerId = input.sellerId();

    // Apply filtering logic based on other input fields
    // In a real implementation, you would:
    // 1. Query database/search service with startDate, endDate filters
    // 2. Apply business logic based on requestId
    // 3. Return matching seller IDs

    // For now, we validate and return the sellerId from input
    // If sellerId is missing or invalid, you could throw an error or return a default
    if (sellerId == null || sellerId.isBlank()) {
      throw new IllegalArgumentException("sellerId is required in SellerInput");
    }

    // Log filtering criteria for debugging
    System.out.println("=== FetchSellerId Vajram Called ===");
    System.out.println("Seller ID: " + sellerId);
    System.out.println("Start Date: " + input.startDate());
    System.out.println("End Date: " + input.endDate());
    System.out.println("Request ID: " + input.requestId());
    System.out.println();

    // Return SellerId - the framework will use this to fetch the full Seller entity
    return new SellerId(sellerId);
  }
}
