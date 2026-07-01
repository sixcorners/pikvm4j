package com.github.sixcorners.openapi_generator_client.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sixcorners.openapi_generator_client.model.GeneratorInput;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;

public class ClientsApiTest {
  private static final ClientsApi client =
      RestClientBuilder.newBuilder()
          .baseUri("https://api.openapi-generator.tech/")
          .property("microprofile.rest.client.disable.default.mapper", true)
          .build(ClientsApi.class);

  @Test
  public void clientOptionsTest() {
    var response = client.clientOptions();
    assertNotNull(response);
  }

  @Test
  public void downloadFileTest() {
    var fileId = "asdf";
    assertThrows(ApiException.class, () -> client.downloadFile(fileId));
  }

  @Test
  public void generateClientTest() {
    var language = "java";
    var generatorInput = new GeneratorInput();
    assertThrows(ApiException.class, () -> client.generateClient(language, generatorInput));
  }

  @Test
  public void getClientOptionsTest() {
    var language = "java";
    var response = client.getClientOptions(language);
    assertNotNull(response);
  }
}
