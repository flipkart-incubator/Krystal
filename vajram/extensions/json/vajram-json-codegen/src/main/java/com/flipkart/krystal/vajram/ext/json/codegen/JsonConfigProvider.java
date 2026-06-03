package com.flipkart.krystal.vajram.ext.json.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.EMPTY_CODE_BLOCK;
import static com.flipkart.krystal.vajram.json.Json.JSON;

import com.fasterxml.jackson.core.type.TypeReference;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ModelRootInfo;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.vajram.json.Json;
import com.flipkart.krystal.vajram.json.JsonConfig;
import com.flipkart.krystal.vajram.json.JsonConfig.Creator;
import com.flipkart.krystal.vajram.json.JsonConfig.SerdeOutputType;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(ModelProtocolConfigProvider.class)
public class JsonConfigProvider implements ModelProtocolConfigProvider {

  @Override
  public ModelProtocolConfig getConfig() {
    return new JsonProtocolConfig();
  }

  private static class JsonProtocolConfig implements ModelProtocolConfig {

    @Override
    public ModelProtocol modelProtocol() {
      return JSON;
    }

    @Override
    public CodeBlock createDeserializationExpression(
        CodeBlock valueExpression, TypeMirror type, CodeGenUtility util) {
      return CodeBlock.of(
          "$L != null ? $T.$L.deserialize($L, $L, $L) : null",
          valueExpression,
          Json.class,
          "JSON",
          valueExpression,
          createDeserializableTypeInfo(type, util),
          createJsonConfig(util.getAnnotationMirror(type, JsonConfig.class), util));
    }

    @Override
    public CodeBlock createSerializationExpression(
        CodeBlock valueExpression, TypeMirror type, CodeGenUtility util) {
      Optional<ModelRootInfo> modelRootInfo = util.asModelRoot(type);
      return CodeBlock.of(
          """
                    $T.$L.serialize(
                      $L,
                      _model -> {
                        $L
                        return null;
                      },
                      $L)\
          """,
          Json.class,
          "JSON",
          valueExpression,
          modelRootInfo.isPresent() && !util.isEnumModel(modelRootInfo.get().element())
              ? CodeBlock.of(
                  """
                            if (_model instanceof $T _t) {
                              return new $T(_t);
                            }\
              """,
                  modelRootInfo.get().type(),
                  util.getImmutTypeName(type, JSON))
              : EMPTY_CODE_BLOCK,
          createJsonConfig(util.getAnnotationMirror(type, JsonConfig.class), util));
    }

    private CodeBlock createDeserializableTypeInfo(TypeMirror typeMirror, CodeGenUtility util) {
      TypeName jsonType = toJsonType(typeMirror, util);
      if (jsonType instanceof ParameterizedTypeName) {
        return CodeBlock.of("new $T<$T>(){}", TypeReference.class, jsonType);
      } else {
        return CodeBlock.of("$T.class", jsonType);
      }
    }

    private CodeBlock createJsonConfig(
        @Nullable AnnotationMirror annotationMirror, CodeGenUtility util) {
      if (annotationMirror == null) {
        return CodeBlock.of("null");
      }
      JsonConfig jsonConfig =
          util.annotationFromMirror(
              annotationMirror,
              stringAnnotationValueMap ->
                  Creator.create(
                      SerdeOutputType.valueOf(
                          ((Element) stringAnnotationValueMap.get("serializeAs").getValue())
                              .getSimpleName()
                              .toString())));
      return CodeBlock.of(
          "$T.Creator.create($T.$L)",
          JsonConfig.class,
          SerdeOutputType.class,
          jsonConfig.serializeAs());
    }

    private TypeName toJsonType(TypeMirror typeMirror, CodeGenUtility util) {
      if (util.isModelRoot(typeMirror)) {
        return util.replaceTypeWith(typeMirror, util.getImmutTypeName(typeMirror, JSON));
      }
      return TypeName.get(util.getOptionalInnerType(typeMirror));
    }
  }
}
