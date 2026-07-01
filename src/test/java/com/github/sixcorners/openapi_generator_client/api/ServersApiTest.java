package com.github.sixcorners.openapi_generator_client.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sixcorners.openapi_generator_client.model.GeneratorInput;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;

public class ServersApiTest {
  private static final ServersApi client =
      RestClientBuilder.newBuilder()
          .baseUri("https://api.openapi-generator.tech/")
          .property("microprofile.rest.client.disable.default.mapper", true)
          .build(ServersApi.class);

  @Test
  public void downloadFileTest() {
    var fileId = "asdf";
    assertThrows(ApiException.class, () -> client.downloadFile(fileId));
  }

  @Test
  public void generateServerForLanguageTest() {
    var framework = "java";
    var generatorInput = new GeneratorInput();
    assertThrows(
        ApiException.class, () -> client.generateServerForLanguage(framework, generatorInput));
  }

  @Test
  public void getServerOptionsTest() {
    var framework = "java";
    var response = client.getServerOptions(framework);
    assertNotNull(response);
  }

  @Test
  public void serverOptionsTest() {
    var response = client.serverOptions();
    assertNotNull(response);
  }
}
