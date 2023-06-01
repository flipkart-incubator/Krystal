package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.DOT;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getVajramImplClassName;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Arrays.stream;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.codegen.models.ParsedVajramData;
import com.flipkart.krystal.vajram.codegen.models.VajramInputFile;
import com.flipkart.krystal.vajram.codegen.models.VajramInputsDef;
import com.flipkart.krystal.vajram.codegen.models.VajramInputsDef.InputFilePath;
import com.flipkart.krystal.vajram.codegen.utils.CodegenUtils;
import com.flipkart.krystal.vajram.codegen.utils.Constants;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  private final List<Path> srcDirs;
  private final Path compiledClassesDir;
  private final Path generatedSrcDir;
  private Iterable<? extends File> compileClasspath;

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
        CommandLine cmd = parser.parse(options, Arrays.copyOfRange(args, 1, args.length));
        Path vajramInputFilePath = Path.of(cmd.getOptionValue("v"));
        if (!vajramInputFilePath.getFileName().toString().endsWith(INPUTS_FILE_EXTENSION)) {
          return;
        }
        Path javaSrcDirPath = Path.of(cmd.getOptionValue("j"));
        Path generatedSrcDir;
        if (cmd.getOptionValue("g") == null) {
          Path sourceSetPath = javaSrcDirPath.getParent();
          String sourceSetName = sourceSetPath.getFileName().toString();
          generatedSrcDir =
              sourceSetPath
                  .getParent()
                  .getParent()
                  .resolve(
                      Path.of("build", "generated", "sources", "vajrams", sourceSetName, "java"));
        } else {
          generatedSrcDir = Path.of(cmd.getOptionValue("g"));
        }
        new VajramCodeGenFacade(List.of(javaSrcDirPath), null, generatedSrcDir)
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

  public static void codeGenModels(Set<? extends File> srcDirs, String destinationDir)
      throws Exception {
    new VajramCodeGenFacade(
            srcDirs.stream().map(File::toPath).toList(), null, Path.of(destinationDir))
        .codeGenModels();
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

  private void codeGenModels() throws Exception {
    ImmutableList<VajramInputFile> inputFiles = getInputDefinitions();
    for (VajramInputFile inputFile : inputFiles) {
      codeGenModels(inputFile);
    }
  }

  private void codeGenModels(Path srcDir, Path vajramInputRelativePath) throws IOException {
    codeGenModels(toVajramInputFile(new InputFilePath(srcDir, vajramInputRelativePath)));
  }

  private void codeGenModels(VajramInputFile inputFile) {
    try {
      VajramCodeGenerator vajramCodeGenerator =
          new VajramCodeGenerator(inputFile, Collections.emptyMap(), Collections.emptyMap());
      codeGenRequest(vajramCodeGenerator);
      codeGenUtil(vajramCodeGenerator);
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not generate code for file %s"
              .formatted(inputFile.inputFilePath().relativeFilePath()),
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

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void codeGenVajramImpl() throws Exception {
    ImmutableList<VajramInputFile> inputFiles = getInputDefinitions();
    ClassLoader systemClassLoader = VajramID.class.getClassLoader();
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
      Map<String, ParsedVajramData> vajramDefs = new HashMap<>();
      Map<String, ImmutableList<VajramInputDefinition>> vajramInputsDef = new HashMap<>();
      // The input defs are first fetched from config file. If
      // it is not found, then the VajramImpl class is loaded and "getInputDefinitions" method is
      // invoked
      //  to fetch the vajram input definitions
      inputFiles.forEach(
          vajramInputFile -> {
            Optional<ParsedVajramData> parsedVajramData =
                ParsedVajramData.fromVajram(urlcl, vajramInputFile);
            if (parsedVajramData.isEmpty()) {
              log.warn("VajramImpl codegen will be skipped for {}. ", vajramInputFile.vajramName());
            } else {
              vajramDefs.put(parsedVajramData.get().vajramName(), parsedVajramData.get());
              vajramInputsDef.put(
                  vajramInputFile.vajramName(),
                  vajramInputFile.vajramInputsDef().allInputsDefinitions());
            }
          });
      // add vajram input dependency vajram definitions from dependent modules/jars
      inputFiles.forEach(
          vajramInputFile -> {
            vajramInputFile
                .vajramInputsDef()
                .allInputsDefinitions()
                .forEach(
                    inputDef -> {
                      if (inputDef instanceof Dependency dependency
                          && dependency.dataAccessSpec() instanceof VajramID vajramID) {
                        String depVajramClass =
                            vajramID
                                .className()
                                .orElseThrow(
                                    () ->
                                        new VajramValidationException(
                                            "Vajram class missing in VajramInputDefinition for :"
                                                + vajramID));
                        String[] splits = Constants.DOT_PATTERN.split(depVajramClass);
                        String depPackageName =
                            stream(splits, 0, splits.length - 1).collect(Collectors.joining(DOT));
                        String vajramName = splits[splits.length - 1];
                        if (!vajramDefs.containsKey(vajramName)) {
                          try {
                            Class<?> vajramClass = urlcl.loadClass(depVajramClass);
                            Map<String, Field> fields = new HashMap<>();
                            stream(vajramClass.getDeclaredFields())
                                .forEach(field -> fields.put(field.getName(), field));
                            ParsedVajramData parsedVajramData1 =
                                new ParsedVajramData(
                                    vajramName, null, null, vajramClass, depPackageName, fields);
                            vajramDefs.put(vajramName, parsedVajramData1);
                            // load impl class and fetch input definitions
                            Class<? extends Vajram> parsedVajramImpl =
                                (Class<? extends Vajram>)
                                    urlcl.loadClass(
                                        depPackageName + DOT + getVajramImplClassName(vajramName));
                            Method getInputDefinitions =
                                parsedVajramImpl.getDeclaredMethod("getInputDefinitions");
                            Vajram vajram = parsedVajramImpl.getConstructor().newInstance();
                            vajramInputsDef.put(
                                vajramName,
                                (ImmutableList<VajramInputDefinition>)
                                    getInputDefinitions.invoke(vajram));
                          } catch (ClassNotFoundException
                              | InvocationTargetException
                              | NoSuchMethodException
                              | IllegalAccessException
                              | InstantiationException e) {
                            throw new RuntimeException(e);
                          }
                        }
                      }
                    });
          });
      inputFiles.forEach(
          inputFile -> {
            // check to call VajramImpl codegen if Vajram class exists
            if (vajramDefs.containsKey(inputFile.vajramName())) {
              try {
                VajramCodeGenerator vajramCodeGenerator =
                    new VajramCodeGenerator(inputFile, vajramDefs, vajramInputsDef);
                codeGenVajramImpl(vajramCodeGenerator, urlcl);
              } catch (Exception e) {
                throw new RuntimeException(
                    "Could not generate vajram impl for file %s"
                        .formatted(inputFile.inputFilePath().relativeFilePath()),
                    e);
              }
            }
          });
    } catch (IOException e) {
      throw new RuntimeException("Exception while generating vajram impl", e);
    }
  }

  private void codeGenVajramImpl(VajramCodeGenerator vajramCodeGenerator, ClassLoader classLoader)
      throws IOException {
    File vajramJavaDir =
        Paths.get(generatedSrcDir.toString(), vajramCodeGenerator.getPackageName().split("\\."))
            .toFile();
    if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
      String vajramImplCode = vajramCodeGenerator.codeGenVajramImpl(classLoader);
      File vajramImplSourceFile =
          new File(
              vajramJavaDir,
              CodegenUtils.getVajramImplClassName(vajramCodeGenerator.getVajramName())
                  + Constants.JAVA_EXT);
      Files.writeString(
          vajramImplSourceFile.toPath(), vajramImplCode, CREATE, TRUNCATE_EXISTING, WRITE);
    }
  }

  private void codeGenUtil(VajramCodeGenerator vajramCodeGenerator) throws IOException {
    File vajramJavaDir =
        Paths.get(generatedSrcDir.toString(), vajramCodeGenerator.getPackageName().split("\\."))
            .toFile();
    if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
      String vajramRequestJavaCode = vajramCodeGenerator.codeGenInputUtil();
      File vajramImplSourceFile =
          new File(
              vajramJavaDir,
              CodegenUtils.getInputUtilClassName(vajramCodeGenerator.getVajramName())
                  + Constants.JAVA_EXT);
      Files.writeString(
          vajramImplSourceFile.toPath(), vajramRequestJavaCode, CREATE, TRUNCATE_EXISTING, WRITE);
    }
  }

  ImmutableList<VajramInputFile> getInputDefinitions() throws Exception {
    Collection<VajramInputFile> vajramInputFiles = new ArrayList<>();
    Set<VajramInputsDef.InputFilePath> inputFiles = getInputsYamlFiles();
    for (VajramInputsDef.InputFilePath inputFile : inputFiles) {
      vajramInputFiles.add(toVajramInputFile(inputFile));
    }
    return ImmutableList.copyOf(vajramInputFiles);
  }

  private static VajramInputFile toVajramInputFile(InputFilePath inputFile) throws IOException {
    String fileName = inputFile.relativeFilePath().getFileName().toString();
    String vajramName = fileName.substring(0, fileName.length() - INPUTS_FILE_EXTENSION.length());
    VajramInputFile vajramInputFile =
        new VajramInputFile(
            vajramName, inputFile, VajramInputsDef.from(inputFile.absolutePath().toFile()));
    return vajramInputFile;
  }

  private Set<VajramInputsDef.InputFilePath> getInputsYamlFiles() throws IOException {
    Set<VajramInputsDef.InputFilePath> inputFilePaths = new LinkedHashSet<>();
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
                  // All vajram inputs files should have '.vajram_inputs.yaml' extension
                  return path.getFileName().toString().endsWith(INPUTS_FILE_EXTENSION);
                })) {
          vajramInputPathStream.forEach(
              p ->
                  inputFilePaths.add(
                      new VajramInputsDef.InputFilePath(srcDir, srcDir.relativize(p))));
        }
      }
    }
    return inputFilePaths;
  }
}
