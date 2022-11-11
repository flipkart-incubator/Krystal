package com.flipkart.krystal.vajram.utils;

import java.util.Random;

public final class RandomStringGenerator {

  @SuppressWarnings("SpellCheckingInspection")
  private static final String SUFFIX_CHARS = "abcdefghjkmnpqrstuvwxy3456789";

  private static final RandomStringGenerator INSTANCE = new RandomStringGenerator();

  private final Random random = new Random();

  public static RandomStringGenerator instance() {
    return INSTANCE;
  }

  public String generateRandomString(int length) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < length; i++) {
      stringBuilder.append(SUFFIX_CHARS.charAt(random.nextInt(SUFFIX_CHARS.length())));
    }
    return stringBuilder.toString();
  }

  private RandomStringGenerator() {}
}
