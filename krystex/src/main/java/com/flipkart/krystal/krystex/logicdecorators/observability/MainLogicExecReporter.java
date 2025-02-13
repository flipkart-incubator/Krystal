package com.flipkart.krystal.krystex.logicdecorators.observability;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.data.Errable.nil;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.io.File.separator;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.concurrent.CompletableFuture.allOf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.google.common.collect.ImmutableMap;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class MainLogicExecReporter implements OutputLogicDecorator {

  private final KryonExecutionReport kryonExecutionReport;
  private static final String FILE_PATH = separator + "tmp" + separator + "krystal_exec_graph_";
  private final ObjectMapper objectMapper;

  public MainLogicExecReporter(KryonExecutionReport kryonExecutionReport) {
    this.kryonExecutionReport = kryonExecutionReport;
    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .setSerializationInclusion(NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(SerializationFeature.FAIL_ON_SELF_REFERENCES);
  }

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate, OutputLogicDefinition<Object> originalLogicDefinition) {
    return facets -> {
      KryonId kryonId = originalLogicDefinition.kryonLogicId().kryonId();
      KryonLogicId kryonLogicId = originalLogicDefinition.kryonLogicId();
      /*
       Report logic start
      */
      kryonExecutionReport.reportMainLogicStart(kryonId, kryonLogicId, facets);

      /*
       Execute logic
      */
      ImmutableMap<FacetValues, CompletableFuture<@Nullable Object>> results =
          logicToDecorate.execute(facets);
      /*
       Report logic end
      */
      allOf(results.values().toArray(CompletableFuture[]::new))
          .whenComplete(
              (unused, throwable) -> {
                kryonExecutionReport.reportMainLogicEnd(
                    kryonId,
                    kryonLogicId,
                    new LogicExecResults(
                        results.entrySet().stream()
                            .map(
                                e ->
                                    new LogicExecResponse(
                                        e.getKey(),
                                        e.getValue().handle(Errable::errableFrom).getNow(nil())))
                            .collect(toImmutableList())));
              });
      return results;
    };
  }

  @Override
  public String getId() {
    return MainLogicExecReporter.class.getName();
  }

  public KryonExecutionReport getKryonExecutionReport() {
    return this.kryonExecutionReport;
  }

  @Override
  public void onComplete() {
    String htmlString = generateGraph();
    String fileName =
        LocalDateTime.now(ZoneId.of(checkNotNull(ZoneId.SHORT_IDS.get("IST"))))
                .format(ISO_OFFSET_DATE_TIME)
            + ".html";
    writeToFile(htmlString, FILE_PATH + fileName);
  }

  public static void writeToFile(String content, String filePath) {
    try {
      Path path = Paths.get(filePath);
      if (path.getParent() == null) {
        log.error(
            "Parent path is null so not storing the html output of DefaultKryonExecutionReport");
        return;
      }
      if (!Files.exists(path.getParent())) {
        Files.createDirectories(path.getParent());
      }

      FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8);
      writer.write(content);
      writer.close();
    } catch (IOException e) {
      log.error("Error writing file: {} with path: {}" + e.getMessage(), filePath);
    }
  }

  private String generateGraph() {
    try {
      String jsonString = objectMapper.writeValueAsString(kryonExecutionReport);
      return GenerateHtml.generateHtml(jsonString);
    } catch (JsonProcessingException e) {
      log.error("Error came while serializing kryonExecutionReport");
    }
    return "";
  }
}
