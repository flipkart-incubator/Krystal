package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getVajramImplClassName;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel;
import com.flipkart.krystal.vajram.codegen.models.ParsedVajramData;
import com.flipkart.krystal.vajram.codegen.models.VajramFacetsDef;
import com.flipkart.krystal.vajram.codegen.models.VajramFacetsDef.InputFilePath;
import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.models.VajramInfoLite;
import com.flipkart.krystal.vajram.codegen.utils.CodegenUtils;
import com.flipkart.krystal.vajram.codegen.utils.Constants;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

@Slf4j
public final class VajramCodeGenFacade {

  public static final String INPUTS_FILE_EXTENSION = ".vajram.yaml";
  private static final Object NIL = new Object();
  private final List<Path> srcDirs;
  private final Path compiledClassesDir;
  private final Path generatedSrcDir;
  private final Iterable<? extends File> compileClasspath;

  public static void main(String[] args) {
    Options options = new Options();

    String command = args[0];

    options.addOption(
        Option.builder("j")
            .longOpt("javaDir")
            .hasArg()
            .desc("Root Directory for all .java files")
            .required()
            .build());

    options.addOption(
        Option.builder("v")
            .longOpt("vajramFile")
            .hasArg()
            .desc("FilePath of the vajram input file for which request code will be generated")
            .required()
            .build());

    options.addOption(
        Option.builder("g")
            .longOpt("generatedSrcDir")
            .hasArg()
            .desc("Root Directory for generated source code")
            .build());

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    if ("codeVajramModels".equals(command)) {
      try {
        List<String> arguments = List.of(args).subList(1, args.length);
        CommandLine cmd = parser.parse(options, arguments.toArray(String[]::new));
        Path vajramInputFilePath = Path.of(cmd.getOptionValue("v"));
        Path vajramInputFileName = vajramInputFilePath.getFileName();
        if (vajramInputFileName == null
            || !vajramInputFileName.toString().endsWith(INPUTS_FILE_EXTENSION)) {
          return;
        }
        Path javaSrcDirPath = Path.of(cmd.getOptionValue("j"));
        Path generatedSrcDir;
        Path sourceSetPath =
            Optional.ofNullable(javaSrcDirPath.getParent())
                .orElseThrow(
                    () -> new IllegalStateException("Java src dir does not have a parent"));
        String sourceSetName =
            Optional.ofNullable(sourceSetPath.getFileName()).map(Path::toString).orElseThrow();
        Path projectDir =
            Optional.ofNullable(sourceSetPath.getParent()).map(Path::getParent).orElseThrow();
        if (cmd.getOptionValue("g") == null) {
          generatedSrcDir =
              projectDir.resolve(
                  Path.of("build", "generated", "sources", "vajrams", sourceSetName, "java"));
        } else {
          generatedSrcDir = Path.of(cmd.getOptionValue("g"));
        }
        new VajramCodeGenFacade(
                List.of(javaSrcDirPath),
                projectDir.resolve(Path.of("build", "classes", "java", sourceSetName)),
                generatedSrcDir)
            .codeGenModels(javaSrcDirPath, javaSrcDirPath.relativize(vajramInputFilePath));
      } catch (ParseException e) {
        log.error("Command line options could not be parsed", e);
        formatter.printHelp("Vajram Code Generator", options);
        //noinspection CallToSystemExit
        System.exit(1);
      } catch (IOException e) {
        log.error("Error reading vajramInputFile", e);
        //noinspection CallToSystemExit
        System.exit(1);
      }
    }
  }

  public static void codeGenVajramImpl(
      Set<? extends File> srcDirs,
      String compiledDir,
      String destinationDir,
      Iterable<? extends File> classpath)
      throws Exception {
    new VajramCodeGenFacade(
            srcDirs.stream().map(File::toPath).toList(),
            Path.of(compiledDir),
            Path.of(destinationDir),
            classpath)
        .codeGenVajramImpl();
  }

  public VajramCodeGenFacade(List<Path> srcDirs, Path compiledClassesDir, Path generatedSrcDir) {
    this(srcDirs, compiledClassesDir, generatedSrcDir, Collections.emptyList());
  }

  public VajramCodeGenFacade(
      List<Path> srcDirs,
      Path compiledClassesDir,
      Path generatedSrcDir,
      Iterable<? extends File> compileClasspath) {
    this.compiledClassesDir = compiledClassesDir;
    this.srcDirs = Collections.unmodifiableList(srcDirs);
    this.generatedSrcDir = generatedSrcDir;
    this.compileClasspath = compileClasspath;
  }

  private void codeGenModels(Path srcDir, Path vajramInputRelativePath) throws IOException {
    codeGenModels(toVajramInfo(new InputFilePath(srcDir, vajramInputRelativePath)));
  }

  private void codeGenModels(VajramInfo inputFile) {
    try {
      VajramCodeGenerator vajramCodeGenerator =
          new VajramCodeGenerator(inputFile, Collections.emptyMap(), (ProcessingEnvironment) NIL);
      codeGenRequest(vajramCodeGenerator);
      codeGenUtil(vajramCodeGenerator);
    } catch (Throwable e) {
      throw new RuntimeException(
          "Could not generate code for file %s"
              .formatted(inputFile.packageName() + '.' + inputFile.vajramId()),
          e);
    }
  }

  private void codeGenRequest(VajramCodeGenerator vajramCodeGenerator) throws IOException {
    File vajramJavaDir =
        Paths.get(generatedSrcDir.toString(), vajramCodeGenerator.getPackageName().split("\\."))
            .toFile();
    if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
      String vajramRequestJavaCode = vajramCodeGenerator.codeGenVajramRequest();
      File vajramRequestSourceFile =
          new File(vajramJavaDir, vajramCodeGenerator.getRequestClassName() + Constants.JAVA_EXT);
      Files.writeString(
          vajramRequestSourceFile.toPath(),
          vajramRequestJavaCode,
          CREATE,
          TRUNCATE_EXISTING,
          WRITE);
    }
  }

  @SuppressWarnings("unchecked")
  private void codeGenVajramImpl() throws Exception {
    ImmutableList<VajramInfo> vajramInfos = getInputDefinitions();
    ClassLoader systemClassLoader = checkNotNull(VajramID.class.getClassLoader());
    List<URL> urls = new ArrayList<>();
    // Adding src dirs to classloader urls
    srcDirs.forEach(
        path -> {
          try {
            urls.add(path.toFile().toURI().toURL());
          } catch (MalformedURLException e) {
            log.error("Malformed url {}", path.toFile().toURI());
            throw new RuntimeException(e);
          }
        });
    // Adding all compile classpath directories to classloader urls
    compileClasspath.forEach(
        file -> {
          try {
            urls.add(file.toURI().toURL());
          } catch (MalformedURLException e) {
            log.error("Malformed url {}", file.toURI());
            throw new RuntimeException(e);
          }
        });
    // Adding compiled classes dir to classloader urls
    urls.add(compiledClassesDir.toFile().toURI().toURL());

    //noinspection ClassLoaderInstantiation
    try (URLClassLoader urlcl = new URLClassLoader(urls.toArray(URL[]::new), systemClassLoader)) {
      Map<VajramID, VajramInfoLite> vajramDefs = new HashMap<>();
      // The input defs are first fetched from config file. If
      // it is not found, then the VajramImpl class is loaded and "getInputDefinitions" method is
      // invoked to fetch the vajram input definitions
      vajramInfos.forEach(
          vajramInfo -> {
            Optional<ParsedVajramData> parsedVajramData = ParsedVajramData.fromVajram(vajramInfo);
            if (parsedVajramData.isEmpty()) {
              log.warn("VajramImpl codegen will be skipped for {}. ", vajramInfo.vajramId());
            } else {
              String vajramName = parsedVajramData.get().vajramName();
              vajramDefs.put(
                  VajramID.vajramID(vajramName), new VajramInfoLite(vajramName, (TypeName) NIL));
            }
          });
      // add vajram input dependency vajram definitions from dependent modules/jars
      vajramInfos.forEach(
          vajramInfo -> {
            vajramInfo
                .facetStream()
                .forEach(
                    inputDef -> {
                      if (inputDef instanceof DependencyModel dependency) {
                        String depVajramClass = dependency.depVajramId().vajramId();
                        String[] splits = Constants.DOT_PATTERN.split(depVajramClass);
                        String vajramName = splits[splits.length - 1];
                        if (!vajramDefs.containsKey(vajramName)) {
                          try {
                            Class<? extends Vajram<?>> vajramClass =
                                (Class<? extends Vajram<?>>) urlcl.loadClass(depVajramClass);
                            vajramDefs.put(
                                VajramID.vajramID(vajramName),
                                new VajramInfoLite(
                                    vajramName, TypeName.get(getVajramResponseType(vajramClass))));
                          } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                          }
                        }
                      }
                    });
          });
      vajramInfos.forEach(
          vajramInfo -> {
            // check to call VajramImpl codegen if Vajram class exists
            if (vajramDefs.containsKey(vajramInfo.vajramId())) {
              try {
                VajramCodeGenerator vajramCodeGenerator =
                    new VajramCodeGenerator(vajramInfo, vajramDefs, (ProcessingEnvironment) NIL);
                codeGenVajramImpl(vajramCodeGenerator);
              } catch (Throwable e) {
                throw new RuntimeException(
                    "Could not generate vajram impl for file %s"
                        .formatted(vajramInfo.packageName() + '.' + vajramInfo.vajramId()),
                    e);
              }
            }
          });
    } catch (IOException e) {
      throw new RuntimeException("Exception while generating vajram impl", e);
    }
  }

  /**
   * To get the response type of vajram, we need to retrieve the actual type mapped to the type
   * parameter of the {@link Vajram} interface. A vajram might not directly extend the {@link
   * Vajram} interface. It might do so via a more elaboarate hierarchy ({@link ComputeVajram}/{@link
   * IOVajram}, AbstractVajram etc.) where each intermediate type can be a class or an interface and
   * have more than one type variable, each with its own type variable name. For example: <br>
   * <br>
   *
   * <p>{@code XYZVajram<A,P> extends Vajram<P>}<br>
   * <br>
   *
   * <p>This means we cannot just use {@link Class#getGenericInterfaces()}, instead we need to
   * iteratively traverse the type hierarchy till we reach {@link Vajram}, and at each level map the
   * type variable with the parent's type variables
   *
   * <p>To understand this method's implementation, see <a
   * href="https://stackoverflow.com/a/25974010" >this </a>
   *
   * @return The response Type of the vajram represented by {@code vajramClass}
   */
  private Type getVajramResponseType(Class<? extends Vajram<?>> vajramClass) {
    return getGenericClassParameter(vajramClass);
  }

  private static Class<?> getGenericClassParameter(final Class<?> parameterizedSubClass) {
    // a mapping from type variables to actual values (classes)
    Map<TypeVariable<?>, Class<?>> mapping = new HashMap<>();

    List<Class<?>> klasses = List.of(parameterizedSubClass);
    while (!klasses.isEmpty()) {
      List<Class<?>> newSuperKlasses = new ArrayList<>();
      for (Class<?> klass : klasses) {
        List<Type> superTypes =
            Stream.concat(
                    Arrays.stream(klass.getGenericInterfaces()),
                    Stream.of(klass.getGenericSuperclass()))
                .toList();
        for (Type type : superTypes) {
          if (type instanceof ParameterizedType parType) {
            Type rawType = parType.getRawType();
            if (rawType == Vajram.class) {
              // found
              Type t =
                  parType
                      .getActualTypeArguments()[
                      // Since Vajram interface has exactly one type param
                      0];
              if (t instanceof Class<?>) {
                return (Class<?>) t;
              } else {
                return Optional.ofNullable(mapping.get((TypeVariable<?>) t))
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                "Could not find mapping for type variable %s".formatted(t)));
              }
            }
            // resolve
            Type[] vars = ((GenericDeclaration) (parType.getRawType())).getTypeParameters();
            Type[] args = parType.getActualTypeArguments();
            for (int i = 0; i < vars.length; i++) {
              if (args[i] instanceof Class<?>) {
                mapping.put((TypeVariable<?>) vars[i], (Class<?>) args[i]);
              } else {
                TypeVariable<?> tVar = (TypeVariable<?>) (args[i]);
                mapping.put(
                    (TypeVariable<?>) vars[i],
                    Optional.ofNullable(mapping.get(tVar))
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "Could not find mapping for type variable %s"
                                        .formatted(tVar))));
              }
            }
            newSuperKlasses.add((Class<?>) rawType);
          } else {
            newSuperKlasses.add((Class<?>) type);
          }
        }
      }
      klasses = newSuperKlasses;
    }
    throw new IllegalArgumentException(
        "no generic supertype for " + parameterizedSubClass + " of type " + Vajram.class);
  }

  private void codeGenVajramImpl(VajramCodeGenerator vajramCodeGenerator) throws IOException {
    File vajramJavaDir =
        Paths.get(generatedSrcDir.toString(), vajramCodeGenerator.getPackageName().split("\\."))
            .toFile();
    if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
      String vajramImplCode = vajramCodeGenerator.codeGenVajramImpl();
      File vajramImplSourceFile =
          new File(
              vajramJavaDir,
              getVajramImplClassName(vajramCodeGenerator.getVajramName()) + Constants.JAVA_EXT);
      Files.writeString(
          vajramImplSourceFile.toPath(), vajramImplCode, CREATE, TRUNCATE_EXISTING, WRITE);
    }
  }

  private void codeGenUtil(VajramCodeGenerator vajramCodeGenerator) throws IOException {
    File vajramJavaDir =
        Paths.get(generatedSrcDir.toString(), vajramCodeGenerator.getPackageName().split("\\."))
            .toFile();
    if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
      String inputUtilJavaCode = vajramCodeGenerator.codeGenInputUtil();
      File inputUtilSourceFile =
          new File(
              vajramJavaDir,
              CodegenUtils.getInputUtilClassName(vajramCodeGenerator.getVajramName())
                  + Constants.JAVA_EXT);
      Files.writeString(
          inputUtilSourceFile.toPath(), inputUtilJavaCode, CREATE, TRUNCATE_EXISTING, WRITE);
    }
  }

  ImmutableList<VajramInfo> getInputDefinitions() throws Exception {
    Collection<VajramInfo> vajramInputFiles = new ArrayList<>();
    Set<VajramFacetsDef.InputFilePath> inputFiles = getInputsYamlFiles();
    for (VajramFacetsDef.InputFilePath inputFile : inputFiles) {
      vajramInputFiles.add(toVajramInfo(inputFile));
    }
    return ImmutableList.copyOf(vajramInputFiles);
  }

  private static VajramInfo toVajramInfo(InputFilePath inputFile) throws IOException {
    String fileName = String.valueOf(checkNotNull(inputFile.relativeFilePath().getFileName()));
    String vajramName = fileName.substring(0, fileName.length() - INPUTS_FILE_EXTENSION.length());
    File vajramInputFile = inputFile.absolutePath().toFile();
    String packageName = CodegenUtils.getPackageFromPath(inputFile.relativeFilePath());
    return toVajramInfo(vajramInputFile, vajramName, packageName);
  }

  public static VajramInfo toVajramInfo(File vajramInputFile, String vajramName, String packageName)
      throws IOException {
    VajramFacetsDef vajramFacetsDef = VajramFacetsDef.from(vajramInputFile);
    TypeName responseType = vajramFacetsDef.output().toTypeName();
    System.out.println("******************************");
    System.out.println(responseType);
    System.out.println("******************************");
    throw new RuntimeException();
    //    return new VajramInfo(
    //        vajramName,
    //        packageName,
    //        vajramFacetsDef.inputs().stream()
    //            .map(InputDef::toInputDefinition)
    //            .collect(toImmutableList()),
    //        vajramFacetsDef.dependencies().stream()
    //            .map(DependencyDef::toInputDefinition)
    //            .collect(toImmutableList()),
    //        responseType,
    //        (TypeElement) NIL);
  }

  private Set<VajramFacetsDef.InputFilePath> getInputsYamlFiles() throws IOException {
    Set<VajramFacetsDef.InputFilePath> inputFilePaths = new LinkedHashSet<>();
    for (Path srcDir : srcDirs) {
      if (srcDir.toFile().isDirectory()) {
        try (Stream<Path> vajramInputPathStream =
            Files.find(
                srcDir,
                100,
                (path, fileAttributes) -> { // Get all vajram_input files
                  if (!fileAttributes.isRegularFile()) {
                    return false;
                  }
                  Path fileName = path.getFileName();
                  // All vajram inputs files should have '.vajram_inputs.yaml' extension
                  return fileName != null && fileName.toString().endsWith(INPUTS_FILE_EXTENSION);
                })) {
          vajramInputPathStream.forEach(
              p ->
                  inputFilePaths.add(
                      new VajramFacetsDef.InputFilePath(srcDir, srcDir.relativize(p))));
        }
      }
    }
    return inputFilePaths;
  }
}
