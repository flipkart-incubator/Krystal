package com.flipkart.krystal.vajram;

public record VajramID(String vajramId) implements DependencySpec {
  public static VajramID vajramID(String id){
    return new VajramID(id);
  }
}
