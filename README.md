This is a batteries excluded REST client library for the OpenAPI Generator API.

You need both a MicroProfile Rest Client implementation and a Jakarta JSON Binding implementation to use it.

```java
//DEPS org.jboss.resteasy.microprofile:microprofile-rest-client:3.0.1.Final
//DEPS org.jboss.resteasy:resteasy-json-binding-provider:7.0.2.Final
//DEPS com.github.sixcorners:openapi-generator-client:1.0.0
//REPOS central,https://jitpack.io

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import info.sixcorners.openapi_generator_client.api.ClientsApi;

void main() {
  RestClientBuilder.newBuilder()
      .baseUri("https://api.openapi-generator.tech/")
      .property("microprofile.rest.client.disable.default.mapper", true) // ??
      .build(ClientsApi.class)
      .clientOptions()
      .forEach(System.out::println);
}
```
