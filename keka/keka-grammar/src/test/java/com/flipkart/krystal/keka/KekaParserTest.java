package com.flipkart.krystal.keka;

import static java.nio.file.Files.readString;
import static java.util.Collections.emptyList;
import static org.antlr.v4.runtime.CharStreams.fromString;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class KekaParserTest {

  @Test
  void computeAveragePrice() throws Exception {
    parseFile("computeAveragePrice.keka");
  }

  @Disabled("Comments are not yet supported")
  @Test
  void errabilityDemo() throws Exception {
    parseFile("errabilityDemo.keka");
  }

  @Test
  void getAvailableProductDetails() throws Exception {
    parseFile("getAvailableProductDetails.keka");
  }

  @Test
  void getFriendsOfUser() throws Exception {
    parseFile("getFriendsOfUser.keka");
  }

  @Test
  void getProductDetails() throws Exception {
    parseFile("getProductDetails.keka");
  }

  @Test
  void getUserInfo() throws Exception {
    parseFile("getUserInfo.keka");
  }

  @Test
  void helloWorld() throws Exception {
    parseFile("helloWorld.keka");
  }

  @Test
  void isProductAvailable() throws Exception {
    parseFile("isProductAvailable.keka");
  }

  @Test
  void sayHelloToFriends() throws Exception {
    parseFile("sayHelloToFriends.keka");
  }

  @Test
  void sayHelloToFriendsV2() throws Exception {
    parseFile("sayHelloToFriendsV2.keka");
  }

  private void parseFile(String name) throws URISyntaxException, IOException {
    Path path =
        Paths.get(
            Optional.ofNullable(this.getClass().getClassLoader())
                .map(classLoader -> classLoader.getResource(name))
                .orElseThrow()
                .toURI());
    assertThat(syntaxErrors(readString(path))).isEqualTo(emptyList());
  }

  @Test
  void outputTypeAcceptsAtMostOneSuffix() {
    for (String type : List.of("string", "string?", "string~", "string~~", "`now string?")) {
      assertThat(syntaxErrors("package t;\nvajram v() out " + type + " { { \"x\" } }"))
          .as(type)
          .isEmpty();
    }
    for (String type : List.of("string?~", "string~?", "string??", "string?~~")) {
      assertThat(syntaxErrors("package t;\nvajram v() out " + type + " { { \"x\" } }"))
          .as(type)
          .isNotEmpty();
    }
  }

  @Test
  void inputsAndInjectionsAcceptOnlyErrableMarker() {
    assertThat(syntaxErrors("package t;\nvajram v(string? a) out `now string { { a } }")).isEmpty();
    assertThat(syntaxErrors("package t;\nvajram v(string~ a) out `now string { { a } }"))
        .isNotEmpty();
    assertThat(syntaxErrors("package t;\nvajram v() out `now string inject (Db~ db) { { \"x\" } }"))
        .isNotEmpty();
  }

  @Test
  void batchedInputsAreMarkedWithAnAnnotationNotAGrouper() {
    assertThat(syntaxErrors("package t;\nvajram v(`batched string a) out `now string { { a } }"))
        .isEmpty();
    assertThat(syntaxErrors("package t;\nvajram v(#batch string a) out `now string { { a } }"))
        .isNotEmpty();
  }

  @Test
  void localVariablesMayCarryACompletionMarker() {
    assertThat(syntaxErrors("package t;\nvajram v() out `now string { string~ x = y; { x } }"))
        .isEmpty();
  }

  private static List<String> syntaxErrors(String code) {
    KekaLexer kekaLexer = new KekaLexer(fromString(code));
    KekaParser kekaParser = new KekaParser(new CommonTokenStream(kekaLexer));
    List<String> errors = new ArrayList<>();
    kekaLexer.removeErrorListeners();
    kekaParser.removeErrorListeners();
    kekaParser.addErrorListener(
        new BaseErrorListener() {
          @Override
          public void syntaxError(
              Recognizer<?, ?> recognizer,
              Object offendingSymbol,
              int line,
              int charPositionInLine,
              String msg,
              RecognitionException e) {
            errors.add("line " + line + ':' + charPositionInLine + ' ' + msg);
          }
        });
    kekaParser.program();
    return errors;
  }
}
