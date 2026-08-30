package com.flipkart.krystal.vajram.lang.rust.ast;

import java.util.List;

/**
 * Grammar rule {@code dependency}: a facet declaration wired to another Vajram's output, e.g.
 * {@code UserInfo userInfo = getUserInfo(userId = userId);}.
 */
public record Dependency(
    List<String> annotations,
    TypeRef type,
    boolean fanout,
    String name,
    DependencyInvocation invocation,
    SourceLocation location)
    implements ComputedFacet {}
