package com.flipkart.krystal.vajram.lang.rust.parse;

import com.flipkart.krystal.vajram.lang.VajramLexer;
import com.flipkart.krystal.vajram.lang.VajramParser;
import com.flipkart.krystal.vajram.lang.VajramParser.Annotated_delegatable_logic_blockContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Annotated_logic_blockContext;
import com.flipkart.krystal.vajram.lang.VajramParser.AnnotationContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Computed_facetContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Dep_input_resolverContext;
import com.flipkart.krystal.vajram.lang.VajramParser.DependencyContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Dependency_invocationContext;
import com.flipkart.krystal.vajram.lang.VajramParser.ErrableTypeContext;
import com.flipkart.krystal.vajram.lang.VajramParser.ExprContext;
import com.flipkart.krystal.vajram.lang.VajramParser.FieldContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Func_callContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Func_call_in_output_logicContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Func_chainContext;
import com.flipkart.krystal.vajram.lang.VajramParser.GrouperContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Imports_declContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Injections_listContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Input_id_declarationContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Inputs_listContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Logic_blockContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Output_blockContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Param_listContext;
import com.flipkart.krystal.vajram.lang.VajramParser.ProgramContext;
import com.flipkart.krystal.vajram.lang.VajramParser.QualifiedNameContext;
import com.flipkart.krystal.vajram.lang.VajramParser.StatementContext;
import com.flipkart.krystal.vajram.lang.VajramParser.TypeContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Vajram_defContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Vajram_fileContext;
import com.flipkart.krystal.vajram.lang.VajramParser.Yield_statementContext;
import com.flipkart.krystal.vajram.lang.rust.ast.Accessor;
import com.flipkart.krystal.vajram.lang.rust.ast.Callers;
import com.flipkart.krystal.vajram.lang.rust.ast.Callers.Caller;
import com.flipkart.krystal.vajram.lang.rust.ast.ComputedFacet;
import com.flipkart.krystal.vajram.lang.rust.ast.DepInputResolver;
import com.flipkart.krystal.vajram.lang.rust.ast.Dependency;
import com.flipkart.krystal.vajram.lang.rust.ast.DependencyInvocation;
import com.flipkart.krystal.vajram.lang.rust.ast.Expr;
import com.flipkart.krystal.vajram.lang.rust.ast.Field;
import com.flipkart.krystal.vajram.lang.rust.ast.InjectionDecl;
import com.flipkart.krystal.vajram.lang.rust.ast.InputDecl;
import com.flipkart.krystal.vajram.lang.rust.ast.LogicBlock;
import com.flipkart.krystal.vajram.lang.rust.ast.OutputBlock;
import com.flipkart.krystal.vajram.lang.rust.ast.SourceLocation;
import com.flipkart.krystal.vajram.lang.rust.ast.Statement;
import com.flipkart.krystal.vajram.lang.rust.ast.TypeRef;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramAnnotation;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramDef;
import com.flipkart.krystal.vajram.lang.rust.ast.VajramFile;
import com.flipkart.krystal.vajram.lang.rust.diag.Diagnostics;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;

/**
 * Builds the vajram-lang AST ({@code com.flipkart.krystal.vajram.lang.rust.ast}) from an ANTLR
 * parse tree. Grammar rule {@code expr} collapses every alternative into one unlabeled context
 * class, so alternatives are told apart by which optional accessor methods are non-null (checked in
 * the grammar's own alternative order) rather than by polymorphic visitor dispatch.
 */
public final class AstBuilder {

  private final Diagnostics diagnostics;
  private String currentFile = "<unknown>";

  public AstBuilder(Diagnostics diagnostics) {
    this.diagnostics = diagnostics;
  }

  /**
   * Parses and builds the AST for every Vajram declaration in one {@code .vajram} source file.
   * Returns no declarations when that source contains a parsing error.
   */
  public List<VajramFile> buildAll(Path path, String source) {
    currentFile = path.toString();
    int errorsBefore = diagnostics.errorCount();
    VajramLexer lexer = new VajramLexer(CharStreams.fromString(source, currentFile));
    VajramParser parser = new VajramParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(
        new BaseErrorListener() {
          @Override
          public void syntaxError(
              Recognizer<?, ?> recognizer,
              Object offendingSymbol,
              int line,
              int charPositionInLine,
              String msg,
              RecognitionException e) {
            diagnostics.error(new SourceLocation(currentFile, line, charPositionInLine + 1), msg);
          }
        });
    ProgramContext program = parser.program();
    if (diagnostics.errorCount() > errorsBefore) {
      return List.of();
    }
    Vajram_fileContext file = program.vajram_file();
    List<String> packageSegments = toQualifiedName(file.package_decl().qualifiedName());
    List<VajramFile.ImportDecl> imports = file.imports_decl().stream().map(this::toImport).toList();
    return file.vajram_def().stream()
        .map(vajram -> toVajramFile(path, packageSegments, imports, vajram))
        .toList();
  }

  /** Parses one declaration, retaining the original API for single-Vajram callers. */
  public java.util.Optional<VajramFile> build(Path path, String source) {
    List<VajramFile> vajrams = buildAll(path, source);
    return vajrams.size() == 1 ? java.util.Optional.of(vajrams.get(0)) : java.util.Optional.empty();
  }

  private VajramFile toVajramFile(
      Path path,
      List<String> packageSegments,
      List<VajramFile.ImportDecl> imports,
      Vajram_defContext vd) {
    return new VajramFile(path, packageSegments, imports, toVajramDef(vd));
  }

  private VajramFile.ImportDecl toImport(Imports_declContext ctx) {
    return new VajramFile.ImportDecl(
        ctx.ID().getText(), toQualifiedName(ctx.qualifiedName()), ctx.FANOUT() != null);
  }

  private List<String> toQualifiedName(QualifiedNameContext ctx) {
    return ctx.ID().stream().map(TerminalNode::getText).toList();
  }

  private VajramDef toVajramDef(Vajram_defContext ctx) {
    List<InputDecl> inputs =
        ctx.inputs_decl().inputs_list() == null
            ? List.of()
            : toInputsList(ctx.inputs_decl().inputs_list());
    TypeRef outputType = toErrableType(ctx.output_decl().errableType());
    Callers callers = toCallers(ctx);
    List<InjectionDecl> injections =
        ctx.injection_decl() == null || ctx.injection_decl().injections_list() == null
            ? List.of()
            : toInjectionsList(ctx.injection_decl().injections_list());
    List<ComputedFacet> computedFacets =
        ctx.computed_facet().stream().map(this::toComputedFacet).toList();
    return new VajramDef(
        ctx.ID().getText(),
        ctx.annotation().stream().map(this::toVajramAnnotation).toList(),
        inputs,
        outputType,
        callers,
        injections,
        computedFacets,
        toOutputBlock(ctx.output_block()),
        loc(ctx));
  }

  private VajramAnnotation toVajramAnnotation(AnnotationContext ctx) {
    if (ctx.annotation_param_list() == null) {
      return new VajramAnnotation(ctx.ID().getText(), List.of());
    }
    return new VajramAnnotation(
        ctx.ID().getText(),
        ctx.annotation_param_list().annotation_arg().stream()
            .map(
                argument ->
                    new VajramAnnotation.Argument(argument.ID().getText(), toExpr(argument.expr())))
            .toList());
  }

  private ComputedFacet toComputedFacet(Computed_facetContext ctx) {
    if (ctx.field() != null) {
      return toField(ctx.field());
    }
    return toDependency(ctx.dependency());
  }

  private Field toField(FieldContext ctx) {
    return new Field(
        ctx.annotation().stream().map(this::annotationName).toList(),
        toType(ctx.type()),
        ctx.FANOUT() != null,
        ctx.ID().getText(),
        toExpr(ctx.expr()),
        loc(ctx));
  }

  private @Nullable Callers toCallers(Vajram_defContext ctx) {
    if (ctx.permissions() == null || ctx.permissions().callers() == null) {
      return null;
    }
    var callers = ctx.permissions().callers();
    List<String> annotations = callers.annotation().stream().map(this::annotationName).toList();
    if (callers.PUBLIC() != null) {
      return new Callers.Public(annotations);
    }
    List<Caller> entries = new ArrayList<>();
    List<String> pendingAnnotations = new ArrayList<>();
    for (ParseTree child : callers.children) {
      if (child instanceof AnnotationContext annotation) {
        pendingAnnotations.add(annotationName(annotation));
      } else if (child instanceof TerminalNode terminal
          && terminal.getSymbol().getType() == VajramLexer.ID) {
        entries.add(new Caller(List.copyOf(pendingAnnotations), terminal.getText()));
        pendingAnnotations = new ArrayList<>();
      }
    }
    return new Callers.Named(entries);
  }

  private List<InputDecl> toInputsList(Inputs_listContext ctx) {
    List<InputDecl> result = new ArrayList<>();
    String pendingGrouper = null;
    List<String> pendingAnnotations = new ArrayList<>();
    for (ParseTree child : ctx.children) {
      if (child instanceof GrouperContext grouper) {
        pendingGrouper = grouperName(grouper);
      } else if (child instanceof AnnotationContext annotation) {
        pendingAnnotations.add(annotationName(annotation));
      } else if (child instanceof Input_id_declarationContext decl) {
        result.add(
            new InputDecl(
                pendingGrouper,
                List.copyOf(pendingAnnotations),
                toErrableType(decl.errableType()),
                decl.ID().getText()));
        pendingGrouper = null;
        pendingAnnotations = new ArrayList<>();
      }
    }
    return result;
  }

  private List<InjectionDecl> toInjectionsList(Injections_listContext ctx) {
    List<InjectionDecl> result = new ArrayList<>();
    List<String> pendingAnnotations = new ArrayList<>();
    for (ParseTree child : ctx.children) {
      if (child instanceof AnnotationContext annotation) {
        pendingAnnotations.add(annotationName(annotation));
      } else if (child
          instanceof
          com.flipkart.krystal.vajram.lang.VajramParser.Injection_id_declarationContext decl) {
        result.add(
            new InjectionDecl(
                List.copyOf(pendingAnnotations),
                toErrableType(decl.errableType()),
                decl.ID().getText()));
        pendingAnnotations = new ArrayList<>();
      }
    }
    return result;
  }

  private String grouperName(GrouperContext ctx) {
    return ctx.ID().getText();
  }

  private String annotationName(AnnotationContext ctx) {
    return ctx.ID().getText();
  }

  private TypeRef toErrableType(ErrableTypeContext ctx) {
    TypeRef base = toType(ctx.type());
    return ctx.ERRABLE() == null
        ? base
        : new TypeRef(base.name(), base.typeArgs(), base.grouperType(), true, base.soon());
  }

  private TypeRef toType(TypeContext ctx) {
    String name;
    boolean grouperType = false;
    if (ctx.non_param_type().ID() != null) {
      name = ctx.non_param_type().ID().getText();
    } else if (ctx.non_param_type().STRING() != null) {
      name = "string";
    } else if (ctx.non_param_type().VOID() != null) {
      name = "void";
    } else {
      name = grouperName(ctx.non_param_type().grouper());
      grouperType = true;
    }
    List<TypeRef> typeArgs = ctx.type().stream().map(this::toType).toList();
    return new TypeRef(name, typeArgs, grouperType, ctx.ERRABLE() != null, ctx.SOON() != null);
  }

  private Dependency toDependency(DependencyContext ctx) {
    List<String> annotations = ctx.annotation().stream().map(this::annotationName).toList();
    return new Dependency(
        annotations,
        toType(ctx.type()),
        ctx.FANOUT() != null,
        ctx.ID().getText(),
        toDependencyInvocation(ctx.dependency_invocation()),
        loc(ctx));
  }

  private DependencyInvocation toDependencyInvocation(Dependency_invocationContext ctx) {
    List<DepInputResolver> resolvers =
        ctx.dep_input_resolver().stream().map(this::toDepInputResolver).toList();
    Expr.Call errableFallback = ctx.ERRABLE() != null ? toFuncCall(ctx.func_call(), false) : null;
    List<DependencyInvocation.AnnotatedBlock> extraBlocks =
        ctx.annotated_logic_block().stream().map(this::toAnnotatedBlock).toList();
    return new DependencyInvocation(
        ctx.FANOUT() != null,
        ctx.ID().getText(),
        resolvers,
        errableFallback,
        extraBlocks,
        loc(ctx));
  }

  private DependencyInvocation.AnnotatedBlock toAnnotatedBlock(Annotated_logic_blockContext ctx) {
    return new DependencyInvocation.AnnotatedBlock(
        ctx.annotation().stream().map(this::annotationName).toList(),
        toLogicBlock(ctx.logic_block()));
  }

  private DepInputResolver toDepInputResolver(Dep_input_resolverContext ctx) {
    if (ctx.dep_input_resolver_stat() != null) {
      var stat = ctx.dep_input_resolver_stat();
      return new DepInputResolver.Stat(
          stat.ID().stream().map(TerminalNode::getText).toList(),
          stat.FANOUT() != null,
          stat.expr().stream().map(this::toExpr).toList());
    }
    var func = ctx.dep_input_resolver_func();
    return new DepInputResolver.Func(
        func.ID().stream().map(TerminalNode::getText).toList(),
        func.FANOUT() != null,
        new LogicBlock(
            func.statement().stream().map(this::toStatement).toList(),
            toYield(func.yield_statement())));
  }

  private OutputBlock toOutputBlock(Output_blockContext ctx) {
    if (ctx.dependency_invocation() != null) {
      return new OutputBlock.Delegate(toDependencyInvocation(ctx.dependency_invocation()));
    }
    Annotated_delegatable_logic_blockContext adlb = ctx.annotated_delegatable_logic_block();
    boolean soon = adlb.completion_time() != null && adlb.completion_time().SOON() != null;
    boolean later = adlb.completion_time() != null && adlb.completion_time().LATER() != null;
    return new OutputBlock.Logic(
        adlb.annotation().stream().map(this::annotationName).toList(),
        soon,
        later,
        toLogicBlock(adlb.logic_block()));
  }

  private LogicBlock toLogicBlock(Logic_blockContext ctx) {
    return new LogicBlock(
        ctx.statement().stream().map(this::toStatement).toList(), toYield(ctx.yield_statement()));
  }

  private com.flipkart.krystal.vajram.lang.rust.ast.@Nullable YieldStatement toYield(
      @Nullable Yield_statementContext ctx) {
    if (ctx == null) {
      return null;
    }
    return new com.flipkart.krystal.vajram.lang.rust.ast.YieldStatement(
        ctx.expr().stream().map(this::toExpr).toList());
  }

  private Statement toStatement(StatementContext ctx) {
    if (ctx.assign_stat() != null) {
      var stat = ctx.assign_stat();
      Input_id_declarationContext decl = stat.input_id_declaration();
      if (decl == null) {
        return new Statement.Expression(toExpr(stat.expr()));
      }
      return new Statement.Assign(
          InputDecl.of(toErrableType(decl.errableType()), decl.ID().getText()),
          toExpr(stat.expr()));
    }
    return new Statement.Throw(toExpr(ctx.throw_stat().expr()));
  }

  private Expr.Call toFuncCall(Func_callContext ctx, boolean unused) {
    List<Expr> args = ctx.param_list() == null ? List.of() : toArgs(ctx.param_list());
    Expr.LambdaBody lambda =
        ctx.annotated_logic_block() == null ? null : toLambdaBody(ctx.annotated_logic_block());
    return new Expr.Call(ctx.ID().getText(), args, lambda, false, false);
  }

  private Expr.LambdaBody toLambdaBody(Annotated_logic_blockContext ctx) {
    LogicBlock block = toLogicBlock(ctx.logic_block());
    return new Expr.LambdaBody(false, false, block.statements(), block.yield());
  }

  private Expr.LambdaBody toLambdaBody(Annotated_delegatable_logic_blockContext ctx) {
    boolean soon = ctx.completion_time() != null && ctx.completion_time().SOON() != null;
    boolean later = ctx.completion_time() != null && ctx.completion_time().LATER() != null;
    LogicBlock block = toLogicBlock(ctx.logic_block());
    return new Expr.LambdaBody(soon, later, block.statements(), block.yield());
  }

  private Expr.Call toCall(Func_call_in_output_logicContext ctx, boolean isNew, boolean isSpecial) {
    List<Expr> args = ctx.param_list() == null ? List.of() : toArgs(ctx.param_list());
    Expr.LambdaBody lambda =
        ctx.annotated_delegatable_logic_block() == null
            ? null
            : toLambdaBody(ctx.annotated_delegatable_logic_block());
    return new Expr.Call(ctx.ID().getText(), args, lambda, isNew, isSpecial);
  }

  private List<Expr> toArgs(Param_listContext ctx) {
    return ctx.expr().stream().map(this::toExpr).toList();
  }

  private Expr.FuncChain toFuncChain(Func_chainContext ctx) {
    List<Expr.Call> calls =
        ctx.func_call_in_output_logic().stream().map(c -> toCall(c, false, false)).toList();
    List<Accessor> connectors = ctx.accessor().stream().map(this::toAccessor).toList();
    return new Expr.FuncChain(calls, connectors);
  }

  private Accessor toAccessor(VajramParser.AccessorContext ctx) {
    boolean soon = ctx.SOON() != null;
    boolean errable = ctx.ERRABLE() != null;
    boolean dot = ctx.DOT() != null;
    if (soon && errable && dot) {
      return Accessor.SOON_ERRABLE_DOT;
    }
    if (soon && errable) {
      return Accessor.SOON_ERRABLE;
    }
    if (soon && dot) {
      return Accessor.SOON_DOT;
    }
    if (errable && dot) {
      return Accessor.ERRABLE_DOT;
    }
    if (soon) {
      return Accessor.SOON;
    }
    if (errable) {
      return Accessor.ERRABLE;
    }
    return Accessor.DOT;
  }

  /**
   * Grammar rule {@code expr} has one unlabeled context class for every alternative, so
   * alternatives are distinguished by which optional accessor is non-null, checked in the same
   * order as the grammar's own alternatives.
   */
  private Expr toExpr(ExprContext ctx) {
    int childExprs = ctx.expr().size();
    if (childExprs == 0) {
      if (ctx.var_use() != null) {
        return new Expr.VarUse(ctx.var_use().ID().getText(), ctx.var_use().ERRABLE() != null);
      }
      if (ctx.STRING_LITERAL() != null) {
        return new Expr.StringLiteral(ctx.STRING_LITERAL().getText());
      }
      if (ctx.NUM_LITERAL() != null) {
        return new Expr.IntLiteral(ctx.NUM_LITERAL().getText());
      }
      if (ctx.bool() != null) {
        return new Expr.BoolLiteral(ctx.bool().TRUE() != null);
      }
      if (ctx.func_chain() != null) {
        return toFuncChain(ctx.func_chain());
      }
      if (ctx.func_call_in_output_logic() != null) {
        return toCall(ctx.func_call_in_output_logic(), ctx.NEW() != null, ctx.SPECIAL() != null);
      }
      if (ctx.grouper() != null) {
        return new Expr.GrouperRef(grouperName(ctx.grouper()));
      }
      if (ctx.array_expr() != null) {
        return new Expr.Array(ctx.array_expr().expr().stream().map(this::toExpr).toList());
      }
      diagnostics.error(loc(ctx), "Unrecognized primary expression: " + ctx.getText());
      return new Expr.VarUse(ctx.getText(), false);
    }
    if (childExprs == 1) {
      Expr target = toExpr(ctx.expr(0));
      if (ctx.NOT() != null) {
        return new Expr.Not(target);
      }
      if (ctx.accessor() != null && ctx.func_chain() != null) {
        return new Expr.ChainedCall(
            target, toAccessor(ctx.accessor()), toFuncChain(ctx.func_chain()));
      }
      if (ctx.accessor() != null && ctx.ID() != null) {
        return new Expr.MemberAccess(target, toAccessor(ctx.accessor()), ctx.ID().getText());
      }
      if (ctx.ID() != null) {
        return new Expr.MethodRef(target, ctx.ID().getText());
      }
      diagnostics.error(loc(ctx), "Unrecognized unary/postfix expression: " + ctx.getText());
      return target;
    }
    // childExprs == 2: binary '+' or '=='
    Expr left = toExpr(ctx.expr(0));
    Expr right = toExpr(ctx.expr(1));
    String op = ctx.PLUS() != null ? "+" : "==";
    return new Expr.BinaryOp(left, op, right);
  }

  private SourceLocation loc(ParserRuleContext ctx) {
    return new SourceLocation(
        currentFile, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine() + 1);
  }
}
