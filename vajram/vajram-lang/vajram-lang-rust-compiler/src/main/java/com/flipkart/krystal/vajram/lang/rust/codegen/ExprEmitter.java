package com.flipkart.krystal.vajram.lang.rust.codegen;

import com.flipkart.krystal.vajram.lang.rust.ast.Accessor;
import com.flipkart.krystal.vajram.lang.rust.ast.Expr;
import com.flipkart.krystal.vajram.lang.rust.ast.Statement;
import com.flipkart.krystal.vajram.lang.rust.ast.YieldStatement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Renders vajram-lang expressions as Rust source text, scoped to one Vajram's input/injection names
 * (so a bare reference to a facet or injected dependency resolves to {@code inputs.x} / {@code
 * deps.x} rather than a dangling free identifier).
 *
 * <p>Per the confirmed compiler strategy this is <b>structural transliteration, not semantic
 * transpilation</b>: Rust already supports the same fluent {@code a.b().c()} method-call syntax as
 * Java, so call chains carry over almost verbatim as plain Rust method calls - the compiler does
 * not try to know what any given method call "means". The only things actually desugared here are
 * vajram-lang's own operators:
 *
 * <ul>
 *   <li>{@code ~} (soon) in an accessor -&gt; {@code .await} inserted before the following call
 *   <li>{@code ?} (errable) in an accessor -&gt; no extra syntax; the method that follows (e.g.
 *       {@code default}, {@code valuePresent}) is just a trait method call on the {@code Result<T,
 *       VajramError>} that the errable facet already compiled to - see the {@code
 *       vajram_rt::Errable} trait in the bundled prelude.
 *   <li>{@code new Foo(args)} -&gt; {@code Foo::new(args)}
 *   <li>{@code #name(args)} -&gt; a call to a generated {@code <name>_key(args)} helper
 *   <li>the implicit lambda parameter {@code _} -&gt; renamed to {@code it} (Rust reserves {@code
 *       _} for a discard pattern, it cannot be referenced)
 * </ul>
 *
 * Anything this doesn't know how to map (e.g. Java stdlib idioms with no Rust equivalent) is still
 * emitted verbatim; it is expected to surface as a real {@code rustc} error rather than a silent
 * miscompile, which is intentional - see the plan's non-goals.
 */
public final class ExprEmitter {

  private final Set<String> inputNames;
  private final Set<String> injectionNames;
  private final Map<String, String> boundaryTypeNames;
  private final Set<String> fieldNames;
  private final Set<String> deferredFacetNames;
  private final String inputsRoot;
  private final String depsRoot;

  public ExprEmitter(
      Set<String> inputNames,
      Set<String> injectionNames,
      Map<String, String> boundaryTypeNames,
      Set<String> fieldNames) {
    this(inputNames, injectionNames, boundaryTypeNames, fieldNames, Set.of(), "inputs", "deps");
  }

  private ExprEmitter(
      Set<String> inputNames,
      Set<String> injectionNames,
      Map<String, String> boundaryTypeNames,
      Set<String> fieldNames,
      Set<String> deferredFacetNames,
      String inputsRoot,
      String depsRoot) {
    this.inputNames = inputNames;
    this.injectionNames = injectionNames;
    this.boundaryTypeNames = boundaryTypeNames;
    this.fieldNames = fieldNames;
    this.deferredFacetNames = deferredFacetNames;
    this.inputsRoot = inputsRoot;
    this.depsRoot = depsRoot;
  }

  public ExprEmitter withDeferredFacets(Set<String> names) {
    return new ExprEmitter(
        inputNames, injectionNames, boundaryTypeNames, fieldNames, names, inputsRoot, depsRoot);
  }

  public ExprEmitter inTaskScope(String taskInputs, String taskDeps) {
    return new ExprEmitter(
        inputNames,
        injectionNames,
        boundaryTypeNames,
        fieldNames,
        deferredFacetNames,
        taskInputs,
        taskDeps);
  }

  public String depsRoot() {
    return depsRoot;
  }

  public boolean isDeferredFacetName(String name) {
    return deferredFacetNames.contains(name);
  }

  /**
   * Whether a bare name is a locally owned computed field rather than an {@code Rc} boundary value.
   */
  public boolean isFieldName(String name) {
    return fieldNames.contains(name);
  }

  public String emit(Expr expr) {
    // Note: not a `switch` over the sealed Expr hierarchy - pattern-matching switch is a preview
    // feature on the project's Java 17 toolchain, so we use plain instanceof (stable since 16).
    if (expr instanceof Expr.VarUse v) {
      return emitVarUse(v);
    } else if (expr instanceof Expr.StringLiteral s) {
      return s.javaText() + ".to_string()";
    } else if (expr instanceof Expr.IntLiteral i) {
      return i.text();
    } else if (expr instanceof Expr.BoolLiteral b) {
      return Boolean.toString(b.value());
    } else if (expr instanceof Expr.Array a) {
      return "vec!["
          + a.elements().stream()
              .map(this::emitArrayElement)
              .reduce((x, y) -> x + ", " + y)
              .orElse("")
          + "]";
    } else if (expr instanceof Expr.Not n) {
      return "!(" + emit(n.operand()) + ")";
    } else if (expr instanceof Expr.BinaryOp b) {
      if ("+".equals(b.operator())) {
        return emitAdditionOperand(b.left(), true) + " + " + emitAdditionOperand(b.right(), false);
      }
      return emit(b.left()) + " " + b.operator() + " " + emit(b.right());
    } else if (expr instanceof Expr.MethodRef m) {
      return emit(m.target()) + "::" + m.member();
    } else if (expr instanceof Expr.MemberAccess m) {
      return emit(m.target()) + connector(m.accessor()) + m.member();
    } else if (expr instanceof Expr.ChainedCall c) {
      if (isStringPrefix(c)) {
        Expr.Call first = c.call().calls().get(0);
        return emit(c.target())
            + ".chars().take(("
            + "*"
            + emit(first.args().get(0))
            + ") as usize).collect::<String>()";
      }
      return emit(c.target()) + connector(c.accessor()) + emitChain(c.call());
    } else if (expr instanceof Expr.GrouperRef g) {
      return identifier(g.name()) + "_key";
    } else if (expr instanceof Expr.Call c) {
      return emitCall(c);
    } else if (expr instanceof Expr.FuncChain fc) {
      return emitChain(fc);
    }
    throw new IllegalStateException("Unhandled expr kind: " + expr.getClass());
  }

  private String emitArrayElement(Expr element) {
    if (element instanceof Expr.VarUse varUse
        && (inputNames.contains(varUse.name()) || injectionNames.contains(varUse.name()))) {
      return "(*" + emitVarUse(varUse) + ").clone()";
    }
    return emit(element);
  }

  private String emitAdditionOperand(Expr operand, boolean left) {
    if (!left && operand instanceof Expr.StringLiteral stringLiteral) {
      return stringLiteral.javaText();
    }
    if (!(operand instanceof Expr.VarUse varUse)) {
      return emit(operand);
    }
    String type = boundaryTypeNames.get(varUse.name());
    if (type == null) {
      return emit(operand);
    }
    String value = emit(varUse);
    if ("string".equals(type)) {
      return left ? "(*" + value + ").clone()" : value + ".as_str()";
    }
    return "*" + value;
  }

  private String emitVarUse(Expr.VarUse v) {
    if ("nil".equals(v.name())) {
      // vajram-lang's `nil` literal is just an identifier grammatically; Rust has no single
      // nameable "nil of any type" value, so it becomes a call to the prelude's generic `nil()`.
      return "nil()";
    }
    String name = resolve(v.name());
    // `expr?` (errable-suffixed var use) surfaces the underlying Result directly via the
    // Errable trait; a bare reference is just the value/binding itself.
    if (deferredFacetNames.contains(v.name())) {
      name += ".clone().await";
    }
    return v.errableSuffix() ? name + ".as_errable()" : name;
  }

  private static boolean isStringPrefix(Expr.ChainedCall call) {
    return call.accessor() == Accessor.DOT
        && call.call().calls().size() == 1
        && "first".equals(call.call().calls().get(0).name())
        && call.call().calls().get(0).args().size() == 1
        && call.call().calls().get(0).lambda() == null;
  }

  /**
   * Facets live on {@code inputs.}, injected dependencies on {@code deps.}; everything else (local
   * `let`-bound dependency results, lambda params) is already a bare local binding.
   */
  private String resolve(String name) {
    String id = identifier(name);
    if (injectionNames.contains(name)) {
      return depsRoot + "." + id;
    }
    if (inputNames.contains(name)) {
      return inputsRoot + "." + id;
    }
    return id;
  }

  private String connector(Accessor accessor) {
    return (accessor.hasSoon() ? ".await" : "") + ".";
  }

  private String emitChain(Expr.FuncChain chain) {
    List<Expr.Call> calls = chain.calls();
    List<Accessor> connectors = chain.connectors();
    StringBuilder sb = new StringBuilder(emitCall(calls.get(0)));
    for (int i = 1; i < calls.size(); i++) {
      sb.append(connector(connectors.get(i - 1))).append(emitCall(calls.get(i)));
    }
    return sb.toString();
  }

  private String emitCall(Expr.Call call) {
    String callee;
    if (call.isSpecial()) {
      // `#name(...)` / `new #name(...)` both construct a facet-group key value; must be checked
      // before the plain `isNew()` branch below since grouper names like `mod` are Rust keywords
      // and can't appear before `::` the way a type name normally would.
      callee = Naming.capitalize(call.name()) + "Key::new";
    } else if (call.isNew()) {
      callee = rustTypeName(call.name()) + "::new";
    } else {
      // A call's own name is a method/function name, not a variable reference - only bare
      // VarUse identifiers resolve against inputs/deps.
      callee = identifier(call.name());
    }
    if (call.lambda() != null) {
      return callee + "(|it| " + emitLambdaBody(call.lambda()) + ")";
    }
    String args = call.args().stream().map(this::emit).reduce((a, b) -> a + ", " + b).orElse("");
    return callee + "(" + args + ")";
  }

  private String emitLambdaBody(Expr.LambdaBody body) {
    return "{ " + emitBlockBody(body.statements(), body.yield()) + " }";
  }

  /** Shared by lambda bodies and Vajram output logic blocks: statements, then a trailing value. */
  public String emitBlockBody(List<Statement> statements, @Nullable YieldStatement yield) {
    StringBuilder sb = new StringBuilder();
    for (Statement statement : statements) {
      sb.append(emitStatement(statement)).append(' ');
    }
    if (yield != null) {
      sb.append(emitYieldValue(yield));
    }
    return sb.toString();
  }

  /** Wraps a Vajram result in shared ownership while preserving an existing owned binding. */
  public String emitOwnedBlockBody(List<Statement> statements, @Nullable YieldStatement yield) {
    StringBuilder sb = new StringBuilder();
    for (Statement statement : statements) {
      sb.append(emitStatement(statement)).append(' ');
    }
    if (yield == null) {
      sb.append("Rc::new(())");
    } else if (yield.values().size() == 1 && yield.values().get(0) instanceof Expr.VarUse varUse) {
      if (isDeferredFacetName(varUse.name())) {
        sb.append(emit(varUse));
      } else {
        sb.append("Rc::clone(&").append(emit(varUse)).append(")");
      }
    } else {
      sb.append("Rc::new(").append(emitYieldValue(yield)).append(")");
    }
    return sb.toString();
  }

  private String emitYieldValue(YieldStatement yield) {
    if (yield.values().size() == 1) {
      return emit(yield.values().get(0));
    }
    return "("
        + yield.values().stream().map(this::emit).reduce((a, b) -> a + ", " + b).orElse("")
        + ")";
  }

  private String emitStatement(Statement statement) {
    if (statement instanceof Statement.Assign a) {
      return "let " + identifier(a.decl().name()) + " = " + emit(a.value()) + ";";
    } else if (statement instanceof Statement.Expression e) {
      return emit(e.value()) + ";";
    } else if (statement instanceof Statement.Throw t) {
      return "return Err(VajramError::from(" + emit(t.value()) + "));";
    }
    throw new IllegalStateException("Unhandled statement kind: " + statement.getClass());
  }

  /**
   * Rust reserves {@code _} as a discard pattern; vajram-lang's implicit lambda arg becomes {@code
   * it}.
   */
  private static String identifier(String name) {
    return "_".equals(name) ? "it" : name;
  }

  private static String rustTypeName(String name) {
    return name;
  }
}
