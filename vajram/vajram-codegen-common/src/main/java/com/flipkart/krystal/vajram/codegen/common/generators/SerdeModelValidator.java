package com.flipkart.krystal.vajram.codegen.common.generators;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.model.ModelRoot;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Validates a model's compatibility with a specific serde protocol. Each serde module (e.g., Json,
 * Protobuf3) should instantiate this validator and call {@link #validate} to enforce:
 *
 * <ul>
 *   <li>If the protocol's {@link ModelProtocol#modelsNeedToBePure()} returns {@code true}, the
 *       model must be pure (except auto-generated REQUEST models)
 *   <li>All nested Model fields must support the same SerDe protocol
 * </ul>
 */
public final class SerdeModelValidator {

  private final CodeGenUtility util;
  private final TypeElement modelRootType;
  private final ModelProtocol protocol;
  private final String protocolName;

  public SerdeModelValidator(
      CodeGenUtility util, TypeElement modelRootType, ModelProtocol protocol) {
    this.util = util;
    this.modelRootType = modelRootType;
    this.protocol = protocol;
    this.protocolName = protocol.getClass().getSimpleName();
  }

  /**
   * Validates the model for serde protocol compatibility. If the protocol requires purity, checks
   * that the model is pure (skipping auto-generated REQUEST models). Then validates that all nested
   * Model fields support the same protocol.
   */
  public void validate(List<ExecutableElement> modelMethods) {
    ModelRoot modelRoot = modelRootType.getAnnotation(ModelRoot.class);
    if (modelRoot != null && protocol.modelsNeedToBePure()) {
      if (!modelRoot.pure()) {
        util.error(
            "Model '%s' supports serde protocol '%s' which requires purity (pure = true)."
                .formatted(modelRootType.getQualifiedName(), protocolName),
            modelRootType);
      }
    }
    for (ExecutableElement method : modelMethods) {
      TypeMirror returnType = method.getReturnType();
      // Unwrap Optional
      if (util.isOptional(returnType)) {
        returnType = util.getOptionalInnerType(returnType);
      }
      checkFieldForNestedModels(method, returnType);
    }
  }

  private void checkFieldForNestedModels(ExecutableElement method, TypeMirror type) {
    String fieldName = method.getSimpleName().toString();

    if (util.isRawAssignable(type, Model.class)) {
      validateModelSupportsProtocol(method, type, fieldName);
    } else if (util.isListType(type)) {
      TypeMirror elementType = util.getContentType(type);
      if (util.isRawAssignable(elementType, Model.class)) {
        validateModelSupportsProtocol(method, elementType, fieldName);
      }
    } else if (util.isMapType(type)) {
      TypeMirror valueType = util.getMapValueType(type);
      if (util.isRawAssignable(valueType, Model.class)) {
        validateModelSupportsProtocol(method, valueType, fieldName);
      }
    }
  }

  private void validateModelSupportsProtocol(
      ExecutableElement method, TypeMirror type, String fieldName) {
    Element element = util.processingEnv().getTypeUtils().asElement(type);
    if (element instanceof TypeElement typeElement) {
      ModelRoot fieldModelRoot = typeElement.getAnnotation(ModelRoot.class);
      if (fieldModelRoot == null) {
        util.error(
            "Field '%s' in model '%s' references type '%s' which extends Model but does not have @ModelRoot annotation."
                .formatted(
                    fieldName, modelRootType.getQualifiedName(), typeElement.getQualifiedName()),
            method);
        return;
      }
      if (!util.typeExplicitlySupportsProtocol(typeElement, protocol.getClass())) {
        util.error(
            "Field '%s' in model '%s' references model '%s' which does not support serde protocol '%s'. "
                    .formatted(
                        fieldName,
                        modelRootType.getQualifiedName(),
                        typeElement.getQualifiedName(),
                        protocolName)
                + "All nested Model fields must support the same serde protocols as the parent model.",
            method);
      }
    }
  }
}
