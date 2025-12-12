package com.flipkart.krystal.vajram.graphql.samples.seller;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

/**
 * Vajram to fetch seller details using only SellerId.
 *
 * <p>This is called from Seller_GQlAggr with just the SellerId. It fetches all seller details and
 * returns them as FetchSellerDetails_GQlFields.
 *
 * <p><strong>Key Pattern:</strong> Entity ID → Fetch Details → Return Entity Fields
 *
 * <p>This is a simple data fetcher that:
 *
 * <ul>
 *   <li>Takes only SellerId as input (no execution context needed)
 *   <li>Fetches seller data from database/service
 *   <li>Returns all seller fields
 * </ul>
 */
@Vajram
public abstract class FetchSellerDetails extends ComputeVajramDef<FetchSellerDetails_GQlFields> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    // Seller ID from the entity - this is the only input needed
    @IfAbsent(FAIL)
    SellerId id;
  }

  /**
   * Fetches seller details based on the provided SellerId.
   *
   * <p>This method demonstrates:
   *
   * <ul>
   *   <li>Simple input: just SellerId
   *   <li>Fetching seller data (in real implementation, from database/service)
   *   <li>Building a complete entity response with all fields
   * </ul>
   *
   * @param id The seller ID to fetch details for
   * @return Complete Seller entity with all fields populated
   */
  @Output
  static FetchSellerDetails_GQlFields fetchSellerDetails(SellerId id) {
    String sellerId = id.value();

    // Debug logging
    System.out.println("=== FetchSellerDetails Vajram Called ===");
    System.out.println("Seller ID: " + sellerId);
    System.out.println();

    // In a real implementation, this would query a database or service
    // For now, we generate sample data based on the sellerId

    // Calculate metrics (in real implementation, fetch from database)
    float totalSales = calculateTotalSales(sellerId);
    int activeOrders = calculateActiveOrders(sellerId);
    String status = determineStatus(sellerId);
    float rating = calculateRating(totalSales, activeOrders);
    String createdDate = generateCreatedDate(sellerId);
    String metadata = buildMetadata(sellerId, totalSales, activeOrders);

    // Build and return the GQlFields wrapper class with all seller fields
    FetchSellerDetails_GQlFields result =
        FetchSellerDetails_GQlFields.builder()
            .name("Seller " + sellerId)
            .email(sellerId + "@example.com")
            .rating(rating)
            .totalSales(totalSales)
            .activeOrders(activeOrders)
            .status(status)
            .createdDate(createdDate)
            .metadata(metadata)
            .build();

    // Debug logging
    System.out.println("=== FetchSellerDetails Returning ===");
    System.out.println("Seller ID: " + id.value());
    System.out.println("Seller Name: " + result.name());
    System.out.println("Total Sales: " + result.totalSales());
    System.out.println();

    return result;
  }

  /**
   * Calculates total sales for the seller.
   *
   * <p>In a real implementation, this would query a database or analytics service.
   */
  private static float calculateTotalSales(String sellerId) {
    // Sample calculation - in real implementation, fetch from database
    return 10000.50f;
  }

  /** Calculates active orders for the seller. */
  private static int calculateActiveOrders(String sellerId) {
    // Sample calculation - in real implementation, fetch from database
    return 25;
  }

  /** Determines seller status based on performance metrics. */
  private static String determineStatus(String sellerId) {
    // Sample logic - in real implementation, fetch from database
    return "ACTIVE";
  }

  /** Calculates rating based on sales and orders. */
  private static float calculateRating(float totalSales, int activeOrders) {
    if (activeOrders == 0) return 3.0f;
    float avgOrderValue = totalSales / activeOrders;
    if (avgOrderValue > 200) return 4.8f;
    if (avgOrderValue > 100) return 4.5f;
    if (avgOrderValue > 50) return 4.0f;
    return 3.5f;
  }

  /** Generates a created date for the seller. */
  private static String generateCreatedDate(String sellerId) {
    // In real implementation, fetch from database
    return "2023-01-15";
  }

  /** Builds metadata string. */
  private static String buildMetadata(String sellerId, float totalSales, int activeOrders) {
    return "sellerId:"
        + sellerId
        + ";totalSales:"
        + totalSales
        + ";activeOrders:"
        + activeOrders
        + ";";
  }
}
