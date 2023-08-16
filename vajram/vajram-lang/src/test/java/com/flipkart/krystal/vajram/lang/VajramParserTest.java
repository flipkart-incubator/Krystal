package com.flipkart.krystal.vajram.lang;

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
import org.junit.jupiter.api.Test;

public class VajramParserTest {
  @Test
  void getUserInfo() throws Exception {
    parseFile("getUserInfo.vajram");
  }

  @Test
  void sayHelloToFriends() throws Exception {
    parseFile("sayHelloToFriends.vajram");
  }

  @Test
  void getFriendsOfUser() throws Exception {
    parseFile("getFriendsOfUser.vajram");
  }

  @Test
  void sayHelloToFriendsV2() throws Exception {
    parseFile("sayHelloToFriendsV2.vajram");
  }

  private void parseFile(String name) throws URISyntaxException, IOException {
    Path path =
        Paths.get(
            Optional.ofNullable(this.getClass().getClassLoader())
                .map(classLoader -> classLoader.getResource(name))
                .orElseThrow()
                .toURI());
    String code = readString(path);
    VajramLexer vajramLexer = new VajramLexer(fromString(code));
    VajramParser vajramParser = new VajramParser(new CommonTokenStream(vajramLexer));
    List<String> errors = new ArrayList<>();
    vajramParser.addErrorListener(
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
    vajramParser.program();
    assertThat(errors).isEqualTo(emptyList());
  }
}
