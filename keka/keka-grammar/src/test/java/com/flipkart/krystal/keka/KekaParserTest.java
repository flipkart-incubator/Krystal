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
    String code = readString(path);
    KekaLexer kekaLexer = new KekaLexer(fromString(code));
    KekaParser kekaParser = new KekaParser(new CommonTokenStream(kekaLexer));
    List<String> errors = new ArrayList<>();
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
    assertThat(errors).isEqualTo(emptyList());
  }
}
