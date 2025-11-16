package com.flipkart.krystal.vajram.codegen.common.intellij;

import static com.google.common.base.Throwables.getStackTraceAsString;

import com.flipkart.krystal.annos.ComputeDelegationMode;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.spi.AllVajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.AllVajramsCodeGeneratorProvider;
import com.google.auto.service.AutoService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(AllVajramsCodeGeneratorProvider.class)
public class VajramScopesGeneratorProvider implements AllVajramsCodeGeneratorProvider {

  private final Map<Path, Path> pathsToModuleCache = new HashMap<>();

  @Override
  public CodeGenerator create(AllVajramCodeGenContext codeGenContext) {
    return () -> {
      writeScopesToFiles(getScopes(getVajramDetails(codeGenContext)), codeGenContext);
    };
  }

  private List<VajramDetails> getVajramDetails(AllVajramCodeGenContext codeGenContext) {
    List<VajramDetails> vajramIdToModule = new ArrayList<>();
    for (VajramInfo vajramInfo : codeGenContext.vajramInfos()) {
      TypeElement typeElement = vajramInfo.vajramClassElem();
      VajramCodeGenUtility util = codeGenContext.util();
      ProcessingEnvironment processingEnv = util.processingEnv();
      try {
        FileObject vajramFile =
            processingEnv
                .getFiler()
                .getResource(
                    StandardLocation.SOURCE_PATH,
                    processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName(),
                    typeElement.getSimpleName());
        Path vajramFilePath = Path.of(vajramFile.toUri());
        Path vajramModule = findContainingModule(vajramFilePath);
        if (vajramModule != null) {
          vajramIdToModule.add(new VajramDetails(vajramInfo, vajramFilePath, vajramModule));
        }
      } catch (IOException e) {
        util.codegenUtil()
            .error(
                "Could not read vajram file to generate 'Vajram' scope xml for intellij ide platform"
                    + getStackTraceAsString(e),
                typeElement);
      }
    }
    return vajramIdToModule;
  }

  private List<ScopeInfo> getScopes(List<VajramDetails> vajramIdToModule) {
    List<ScopeInfo> scopes =
        List.of(
            new ScopeInfo("AllVajrams", vajramInfo -> vajramInfo.lite().isVajram()),
            new ScopeInfo(
                "IOVajrams",
                vajramInfo -> ComputeDelegationMode.SYNC.equals(vajramInfo.vajramDelegationMode())),
            new ScopeInfo("AllTraits", vajramInfo -> vajramInfo.lite().isTrait()));
    vajramIdToModule.forEach(
        (v) -> {
          VajramInfo vajramInfo = v.vajramInfo();
          Path srcRelativeVajramPath = v.modulePath().resolve("src").relativize(v.vajramFilePath());
          Path sourceSetRelativeVajramPath =
              srcRelativeVajramPath.subpath(1, srcRelativeVajramPath.getNameCount() - 1);
          for (ScopeInfo scope : scopes) {
            if (scope.memberShipCondition().test(vajramInfo)) {
              scope
                  .xml()
                  .append(
                      """
                        <scope name="%s" pattern="file:%s"/>

                      """
                          .formatted(scope.name(), sourceSetRelativeVajramPath));
            }
          }
        });
    return scopes;
  }

  private void writeScopesToFiles(List<ScopeInfo> scopes, AllVajramCodeGenContext codeGenContext) {
    for (ScopeInfo scope : scopes) {
      scope.writeOut(codeGenContext.util());
    }
  }

  private @Nullable Path findContainingModule(Path path) {
    Path containingModule = pathsToModuleCache.get(path);
    if (containingModule != null) {
      return containingModule;
    }
    if (path != null) {
      if (path.resolve("build.gradle").toFile().exists()) {
        containingModule = path;
      } else {
        containingModule = findContainingModule(path.getParent());
      }
    }
    if (containingModule != null) {
      pathsToModuleCache.put(path, containingModule);
    }
    return containingModule;
  }
}
