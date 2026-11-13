package com.flipkart.krystal.keka.rust.resolve;

import com.flipkart.krystal.keka.rust.ast.ComputedFacet;
import com.flipkart.krystal.keka.rust.ast.DepInputResolver;
import com.flipkart.krystal.keka.rust.ast.Dependency;
import com.flipkart.krystal.keka.rust.ast.DependencyInvocation;
import com.flipkart.krystal.keka.rust.ast.Expr;
import com.flipkart.krystal.keka.rust.ast.Field;
import com.flipkart.krystal.keka.rust.ast.LogicBlock;
import com.flipkart.krystal.keka.rust.ast.OutputBlock;
import com.flipkart.krystal.keka.rust.ast.Statement;
import com.flipkart.krystal.keka.rust.ast.VajramDef;
import java.util.List;
import java.util.function.Consumer;

/** Visits every expression, at any depth, in a vajram's facets and output block. */
final class ExprWalker {

  private ExprWalker() {}

  static void walk(VajramDef vajram, Consumer<Expr> visitor) {
    for (ComputedFacet facet : vajram.computedFacets()) {
      if (facet instanceof Field field) {
        walk(field.value(), visitor);
      } else if (facet instanceof Dependency dependency) {
        walk(dependency.invocation(), visitor);
      }
    }
    if (vajram.outputBlock() instanceof OutputBlock.Logic logic) {
      walk(logic.body(), visitor);
    } else if (vajram.outputBlock() instanceof OutputBlock.Delegate delegate) {
      walk(delegate.invocation(), visitor);
    }
  }

  private static void walk(DependencyInvocation invocation, Consumer<Expr> visitor) {
    for (DepInputResolver resolver : invocation.resolvers()) {
      if (resolver instanceof DepInputResolver.Stat stat) {
        stat.values().forEach(value -> walk(value, visitor));
      } else if (resolver instanceof DepInputResolver.Func func) {
        walk(func.body(), visitor);
      }
    }
    if (invocation.errableFallback() != null) {
      walk(invocation.errableFallback(), visitor);
    }
    invocation.extraAnnotatedBlocks().forEach(block -> walk(block.block(), visitor));
  }

  private static void walk(LogicBlock block, Consumer<Expr> visitor) {
    walk(block.statements(), block.yield() == null ? List.of() : block.yield().values(), visitor);
  }

  private static void walk(List<Statement> statements, List<Expr> yielded, Consumer<Expr> visitor) {
    for (Statement statement : statements) {
      if (statement instanceof Statement.Assign assign) {
        walk(assign.value(), visitor);
      } else if (statement instanceof Statement.Expression expression) {
        walk(expression.value(), visitor);
      } else if (statement instanceof Statement.Throw thrown) {
        walk(thrown.value(), visitor);
      }
    }
    yielded.forEach(value -> walk(value, visitor));
  }

  static void walk(Expr expr, Consumer<Expr> visitor) {
    visitor.accept(expr);
    if (expr instanceof Expr.Array array) {
      array.elements().forEach(element -> walk(element, visitor));
    } else if (expr instanceof Expr.Not not) {
      walk(not.operand(), visitor);
    } else if (expr instanceof Expr.BinaryOp binary) {
      walk(binary.left(), visitor);
      walk(binary.right(), visitor);
    } else if (expr instanceof Expr.MethodRef reference) {
      walk(reference.target(), visitor);
    } else if (expr instanceof Expr.MemberAccess access) {
      walk(access.target(), visitor);
    } else if (expr instanceof Expr.ChainedCall chained) {
      walk(chained.target(), visitor);
      walk(chained.call(), visitor);
    } else if (expr instanceof Expr.FuncChain chain) {
      chain.calls().forEach(call -> walk(call, visitor));
    } else if (expr instanceof Expr.Call call) {
      call.args().forEach(argument -> walk(argument, visitor));
      if (call.lambda() != null) {
        walk(
            call.lambda().statements(),
            call.lambda().yield() == null ? List.of() : call.lambda().yield().values(),
            visitor);
      }
    } else if (expr instanceof Expr.NewModel newModel) {
      newModel.arguments().forEach(argument -> walk(argument.value(), visitor));
    }
  }
}
