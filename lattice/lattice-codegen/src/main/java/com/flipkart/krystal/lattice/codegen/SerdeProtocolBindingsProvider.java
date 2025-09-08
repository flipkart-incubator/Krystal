package com.flipkart.krystal.lattice.codegen;

import static com.flipkart.krystal.lattice.codegen.spi.BindingsProvider.BindingScope.REQUEST;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider.ModelProtocolConfig;
import com.flipkart.krystal.lattice.codegen.spi.BindingsProvider;
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
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
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
    Map<String, ModelProtocolConfig> configs =
        ServiceLoader.load(ModelProtocolConfigProvider.class, this.getClass().getClassLoader())
            .stream()
            .map(ServiceLoader.Provider::get)
            .collect(
                Collectors.toMap(
                    cp ->
                        requireNonNull(
                            cp.getConfig().serdeProtocol().getClass().getCanonicalName()),
                    ModelProtocolConfigProvider::getConfig));
    Map<String, TypeElement> responseTypeElems = new LinkedHashMap<>();
    Map<String, List<String>> responseToVajramsMapping = new LinkedHashMap<>();
    for (TypeElement vajram : remotelyInvocableVajrams) {
      VajramInfoLite vajramInfoLite = util.computeVajramInfoLite(vajram);
      CodeGenType responseType = vajramInfoLite.responseType();
      TypeMirror responseMirror = responseType.javaModelType(util.processingEnv());
      if (!util.codegenUtil().isRawAssignable(responseMirror, Model.class)) {
        continue;
      }
      TypeElement responseTypeElem =
          (TypeElement)
              requireNonNull(
                  util.codegenUtil().processingEnv().getTypeUtils().asElement(responseMirror));
      String responseCanonicalName = responseTypeElem.getQualifiedName().toString();
      responseTypeElems.put(responseCanonicalName, responseTypeElem);
      responseToVajramsMapping
          .computeIfAbsent(responseCanonicalName, s -> new ArrayList<>())
          .add(vajramInfoLite.vajramId().id());
    }
    List<Binding> bindings = new ArrayList<>();
    for (Entry<String, TypeElement> entry : responseTypeElems.entrySet()) {
      String responseCanonicalName = entry.getKey();
      TypeElement responseElement = entry.getValue();
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
      TypeElement defaultSerializationProtocol = getDefaultSerializationProtocol(context);
      if (defaultSerializationProtocol == null) {
        throw util.codegenUtil()
            .errorAndThrow(
                "Could not determine default Serialization protocol of lattice app.",
                context.latticeAppTypeElement());
      }
      List<CodeBlock> providingLogics = new ArrayList<>();
      if (!supportedModelProtocolElems.contains(defaultSerializationProtocol)) {
        throw util.codegenUtil()
            .errorAndThrow(
                "Response type "
                    + responseElement
                    + " of vajram(s): "
                    + responseToVajramsMapping.get(responseCanonicalName)
                    + " does not support the default serialization protocol "
                    + defaultSerializationProtocol,
                responseElement);
      } else {
        ModelProtocolConfig defaultConfig =
            configs.get(defaultSerializationProtocol.getQualifiedName().toString());
        if (defaultConfig == null) {
          throw util.codegenUtil()
              .errorAndThrow(
                  """
              Unrecognized Serde protocol: %s. \
              Please check if the relevant protocol \
              specific libraries are added to the annotationProcessor \
              classpath of the project."""
                      .formatted(defaultSerializationProtocol),
                  context.latticeAppTypeElement());
        }

        ClassName defaultModelBuilderName =
            ClassName.get(
                immutClassName.packageName(),
                immutClassName.simpleName() + defaultConfig.serdeProtocol().modelClassesSuffix());
        providingLogics.add(
            CodeBlock.of(
                """
                if (null == acceptHeader){
                  return $T._builder();
                }
                var acceptHeaderValues = new $T<>(acceptHeader.values());
                if (acceptHeaderValues.contains("*/*")){
                  return $T._builder();
                }
                """,
                defaultModelBuilderName,
                LinkedHashSet.class,
                defaultModelBuilderName));
      }
      for (TypeElement modelProtocol : supportedModelProtocolElems) {
        ModelProtocolConfig config = configs.get(modelProtocol.getQualifiedName().toString());
        if (config == null) {
          util.codegenUtil()
              .note(
                  """
              Skipping creation of binding for %s as protocol config for this \
              protocol was not found in the annotation processor class path"""
                      .formatted(modelProtocol.getQualifiedName()));
          continue;
        }
        configs.put(requireNonNull(config.serdeProtocol().getClass().getCanonicalName()), config);
        providingLogics.add(
            CodeBlock.of(
                """
                if(acceptHeaderValues.contains($S)) {
                  return $T._builder();
                }""",
                config.serdeProtocol().contentType(),
                ClassName.get(
                    immutClassName.packageName(),
                    immutClassName.simpleName() + config.serdeProtocol().modelClassesSuffix())));
      }
      providingLogics.add(
          CodeBlock.of(
              """
              {
                throw new $T($S + acceptHeader.values());
              }
              """,
              IllegalStateException.class,
              "Response type "
                  + responseCanonicalName
                  + " of vajrams: "
                  + responseToVajramsMapping.get(responseCanonicalName)
                  + " doesn't support any of the content types: "));
      bindings.add(
          new ProviderMethod(
              immutClassName.simpleName() + "_Builder",
              immutBuilderClassName,
              List.of(
                  ParameterSpec.builder(
                      ClassName.get(Header.class)
                          .annotated(AnnotationSpec.builder(Nullable.class).build())
                          .annotated(
                              AnnotationSpec.builder(Named.class)
                                  .addMember(
                                      "value",
                                      CodeBlock.of("$T.$L", StandardHeaderNames.class, "ACCEPT"))
                                  .build()),
                      "acceptHeader")),
              providingLogics.stream().collect(CodeBlock.joining(" else ")),
              REQUEST));
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
          .codegenUtil()
          .error(
              "Found more than one default serialization protocol: " + protocols,
              context.latticeAppTypeElement());
    }
    return protocols.get(0);
  }
}
