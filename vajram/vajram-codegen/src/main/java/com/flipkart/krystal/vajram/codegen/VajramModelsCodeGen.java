package com.flipkart.krystal.vajram.codegen;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import com.flipkart.krystal.vajram.codegen.models.VajramInputFile;
import com.flipkart.krystal.vajram.codegen.models.VajramInputsDef;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
public class VajramModelsCodeGen {

  public static final String INPUTS_FILE_EXTENSION = ".vajram.yaml";
  private final List<Path> srcDirs;
  private final Path javaDir;

  public static void main(String[] args) throws Exception {
    Options options = new Options();

    Option classesDirOpt =
        new Option("c", "classesDir", true, "Root Directory for all .class files");
    classesDirOpt.setRequired(true);
    options.addOption(classesDirOpt);

    Option javaSrcDir = new Option("j", "javaDir", true, "Root Directory for all .java files");
    javaSrcDir.setRequired(true);
    options.addOption(javaSrcDir);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd = null; // not a good practice, it serves it purpose

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      log.error("Command line options could not be parsed", e);
      formatter.printHelp("Vajram Code Generator", options);
      System.exit(1);
    }

    //    codeGenModels(cmd.getOptionValue("classesDir"), cmd.getOptionValue("javaDir"));
  }

  public static void codeGenModels(Set<File> srcDirs, String destinationDir) throws Exception {
    new VajramModelsCodeGen(srcDirs.stream().map(File::toPath).toList(), Path.of(destinationDir))
        .codeGenModels();
  }

  public VajramModelsCodeGen(List<Path> srcDirs, Path javaDir) {
    this.srcDirs = srcDirs;
    this.javaDir = javaDir;
  }

  private void codeGenModels() throws Exception {
    ImmutableList<VajramInputFile> inputFiles = getInputDefinitions();
    for (VajramInputFile inputFile : inputFiles) {
      try {
        codeGenRequest(inputFile);
        codeGenUtil(inputFile);
      } catch (Exception e) {
        throw new RuntimeException(
            "Could not generate code for file %s".formatted(inputFile.srcRelativeFilePath()), e);
      }
    }
  }

  private void codeGenRequest(VajramInputFile vajramInputFile) throws IOException {
    VajramCodeGenerator vajramCodeGenerator = new VajramCodeGenerator(vajramInputFile);
    File vajramJavaDir =
        Paths.get(javaDir.toString(), vajramCodeGenerator.getPackageName().split("\\.")).toFile();
    if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
      String vajramRequestJavaCode = vajramCodeGenerator.codeGenVajramRequest();
      File vajramImplSourceFile =
          new File(vajramJavaDir, vajramCodeGenerator.getRequestClassName() + ".java");
      Files.writeString(
          vajramImplSourceFile.toPath(), vajramRequestJavaCode, CREATE, TRUNCATE_EXISTING, WRITE);
    }
  }

  private void codeGenUtil(VajramInputFile vajramInputFile) throws IOException {
    VajramCodeGenerator vajramCodeGenerator = new VajramCodeGenerator(vajramInputFile);
    File vajramJavaDir =
        Paths.get(javaDir.toString(), vajramCodeGenerator.getPackageName().split("\\.")).toFile();
    if (vajramJavaDir.isDirectory() || vajramJavaDir.mkdirs()) {
      String vajramRequestJavaCode = vajramCodeGenerator.codeGenInputUtil();
      File vajramImplSourceFile =
          new File(vajramJavaDir, vajramCodeGenerator.getInputUtilClassName() + ".java");
      Files.writeString(
          vajramImplSourceFile.toPath(), vajramRequestJavaCode, CREATE, TRUNCATE_EXISTING, WRITE);
    }
  }

  ImmutableList<VajramInputFile> getInputDefinitions() throws Exception {
    List<VajramInputFile> vajramInputFiles = new ArrayList<>();
    Set<InputFilePath> inputFiles = getInputsYamlFiles();
    for (InputFilePath inputFile : inputFiles) {
      String fileName = inputFile.relativeFilePath().getFileName().toString();
      String vajramName = fileName.substring(0, fileName.length() - INPUTS_FILE_EXTENSION.length());
      vajramInputFiles.add(
          new VajramInputFile(
              vajramName,
              inputFile.relativeFilePath(),
              VajramInputsDef.from(inputFile.absolutePath().toFile())));
    }
    return ImmutableList.copyOf(vajramInputFiles);
  }

  private Set<InputFilePath> getInputsYamlFiles() throws IOException {
    Set<InputFilePath> inputFilePaths = new LinkedHashSet<>();
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
              p -> inputFilePaths.add(new InputFilePath(srcDir, srcDir.relativize(p))));
        }
      }
    }
    return inputFilePaths;
  }
}
