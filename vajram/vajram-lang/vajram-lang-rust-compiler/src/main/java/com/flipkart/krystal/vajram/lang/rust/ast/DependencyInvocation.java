package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Grammar rule {@code dependency_invocation}: a call to another Vajram, e.g. {@code
 * getUserInfo(userId = userId)}, optionally fanned out ({@code *}) and optionally followed by an
 * errable fallback call ({@code ?default(...)}).
 */
public record DependencyInvocation(
    boolean fanout,
    String vajramName,
    List<DepInputResolver> resolvers,
    Expr.@Nullable Call errableFallback,
    List<AnnotatedBlock> extraAnnotatedBlocks,
    SourceLocation location) {

  /** A trailing {@code `annotation { ... }} attached to a dependency invocation, e.g. skipIf. */
  public record AnnotatedBlock(List<String> annotations, LogicBlock block) {}
}
