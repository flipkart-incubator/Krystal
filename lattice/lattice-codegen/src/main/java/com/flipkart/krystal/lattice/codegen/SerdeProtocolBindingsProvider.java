package com.flipkart.krystal.lattice.codegen;

import static com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindingScope.APP_LOGIC_SCOPE;
import static com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindingScope.NO_SCOPE;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider.ModelProtocolConfig;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindTo.Provider;
import com.flipkart.krystal.lattice.codegen.spi.DefaultSerdeProtocolProvider;
import com.flipkart.krystal.lattice.codegen.spi.LatticeAppCodeGenAttrsProvider;
import com.flipkart.krystal.lattice.core.headers.Header;
import com.flipkart.krystal.lattice.core.headers.StandardHeaderNames;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfoLite;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(BindingsProvider.class)
public final class SerdeProtocolBindingsProvider implements BindingsProvider {

  @Override
  public ImmutableList<Binding> bindings(LatticeCodegenContext context) {
    VajramCodeGenUtility util = context.codeGenUtility();
    ServiceLoader<LatticeAppCodeGenAttrsProvider> providers =
        ServiceLoader.load(LatticeAppCodeGenAttrsProvider.class, this.getClass().getClassLoader());
    Set<TypeElement> remotelyInvocableVajrams = new LinkedHashSet<>();
    for (LatticeAppCodeGenAttrsProvider provider : providers) {
      remotelyInvocableVajrams.addAll(provider.get(context).remotelyInvocableVajrams());
    }
    List<Binding> bindings = new ArrayList<>();
    for (TypeElement vajram : remotelyInvocableVajrams) {
      VajramInfoLite vajramInfoLite = util.computeVajramInfoLite(vajram);
      CodeGenType responseType = vajramInfoLite.responseType();
      TypeMirror responseMirror = responseType.javaModelType(util.processingEnv());
      if (!util.codegenUtil().isRawAssignable(responseMirror, Model.class)) {
        continue;
      }
      TypeElement responseElement =
          (TypeElement)
              requireNonNull(
                  util.codegenUtil().processingEnv().getTypeUtils().asElement(responseMirror));
      SupportedModelProtocols supportedModelProtocols =
          responseElement.getAnnotation(SupportedModelProtocols.class);
      if (supportedModelProtocols == null) {
        continue;
      }
      List<TypeElement> supportedModelProtocolElems =
          context
              .codeGenUtility()
              .codegenUtil()
              .getTypesFromAnnotationMember(supportedModelProtocols::value)
              .stream()
              .filter(tm -> util.codegenUtil().isRawAssignable(tm, SerdeProtocol.class))
              .map(
                  tm ->
                      (TypeElement)
                          requireNonNull(util.processingEnv().getTypeUtils().asElement(tm)))
              .toList();
      if (supportedModelProtocolElems.isEmpty()) {
        continue;
      }
      ClassName immutClassName = util.codegenUtil().getImmutClassName(responseElement);
      ClassName immutBuilderClassName =
          ClassName.get(immutClassName.packageName(), immutClassName.simpleName(), "Builder");
      ServiceLoader<ModelProtocolConfigProvider> configProviders =
          ServiceLoader.load(ModelProtocolConfigProvider.class, this.getClass().getClassLoader());
      TypeElement defaultSerializationProtocol = getDefaultSerializationProtocol(context);
      if (defaultSerializationProtocol == null) {
        throw util.errorAndThrow(
            "Could not determine default Serialization protocol of lattice app.",
            context.latticeAppTypeElement());
      }
      Map<String, ModelProtocolConfig> configs = new LinkedHashMap<>();
      for (ModelProtocolConfigProvider configProvider : configProviders) {
        ModelProtocolConfig config = configProvider.getConfig();
        if (config != null) {
          configs.put(requireNonNull(config.modelProtocolType().getCanonicalName()), config);
        }
      }
      List<CodeBlock> providingLogics = new ArrayList<>();
      if (!supportedModelProtocolElems.contains(defaultSerializationProtocol)) {
        throw util.errorAndThrow(
            "Response type "
                + responseElement
                + " of vajram "
                + vajram
                + " does not support the default serialization protocol "
                + defaultSerializationProtocol,
            vajram);
      } else {
        ModelProtocolConfig defaultConfig =
            configs.get(defaultSerializationProtocol.getQualifiedName().toString());
        if (defaultConfig == null) {
          throw util.errorAndThrow(
              "Unrecognized Serde protocol:  " + defaultSerializationProtocol,
              context.latticeAppTypeElement());
        }

        ClassName defaultModelBuilderName =
            ClassName.get(
                immutClassName.packageName(),
                immutClassName.simpleName() + defaultConfig.modelClassesSuffix());
        providingLogics.add(
            CodeBlock.of(
                """
if (null == acceptHeader){
              return $T._builder();
            }
            $T acceptHeaderValue = acceptHeader.value();
            if (null == acceptHeaderValue){
              return $T._builder();
            }
            return switch (acceptHeaderValue) {
""",
                defaultModelBuilderName,
                String.class,
                defaultModelBuilderName));
      }
      for (ModelProtocolConfigProvider configProvider : configProviders) {
        ModelProtocolConfig config = configProvider.getConfig();
        if (config != null) {
          configs.put(requireNonNull(config.modelProtocolType().getCanonicalName()), config);
          providingLogics.add(
              CodeBlock.of(
                  """
                case $L -> $T._builder();\
""",
                  config.httpContentType(),
                  ClassName.get(
                      immutClassName.packageName(),
                      immutClassName.simpleName() + config.modelClassesSuffix())));
        }
      }
      providingLogics.add(
          CodeBlock.of(
              """
                default -> throw new $T($S + acceptHeader.value());\
""",
              IllegalStateException.class,
              "API '" + vajramInfoLite.vajramId().id() + "' doesn't support the content type: "));
      providingLogics.add(CodeBlock.of("""
            };
"""));
      bindings.add(
          new SimpleBinding(
              ClassName.get(Header.class),
              CodeBlock.of("$T.$L", StandardHeaderNames.class, "ACCEPT"),
              new Provider(CodeBlock.of("null"), APP_LOGIC_SCOPE)));

      bindings.add(
          new ProviderBinding(
              immutClassName.simpleName() + "_Builder",
              immutBuilderClassName,
              List.of(
                  CodeBlock.of(
                      "@$T @$T($T.$L) $T acceptHeader",
                      Nullable.class,
                      Named.class,
                      StandardHeaderNames.class,
                      "ACCEPT",
                      Header.class)),
              providingLogics.stream().collect(CodeBlock.joining("\n")),
              NO_SCOPE));
    }

    return ImmutableList.copyOf(bindings);
  }

  private @Nullable TypeElement getDefaultSerializationProtocol(LatticeCodegenContext context) {
    List<TypeElement> protocols = new ArrayList<>();
    for (DefaultSerdeProtocolProvider defaultSerdeProtocolProvider :
        ServiceLoader.load(DefaultSerdeProtocolProvider.class, this.getClass().getClassLoader())) {
      TypeElement protocol = defaultSerdeProtocolProvider.getDefaultSerializationProtocol(context);
      if (protocol != null) {
        protocols.add(protocol);
      }
    }
    if (protocols.isEmpty()) {
      return null;
    } else if (protocols.size() > 1) {
      context
          .codeGenUtility()
          .error(
              "Found more than one default serialization protocol: " + protocols,
              context.latticeAppTypeElement());
    }
    return protocols.get(0);
  }
}
