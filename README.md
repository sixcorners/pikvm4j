This is a batteries excluded REST client library for the Rclone Remote Control API.

You need both a MicroProfile Rest Client implementation and a Jakarta JSON Binding implementation to use it.

```java
//DEPS org.jboss.resteasy.microprofile:microprofile-rest-client:3.0.1.Final
//DEPS org.jboss.resteasy:resteasy-json-binding-provider:7.0.2.Final
//DEPS com.github.sixcorners:rclone4j-rc:1.0.0
//REPOS central,https://jitpack.io

import com.github.sixcorners.rclone4j_rc.ForceJsonFilter;
import com.github.sixcorners.rclone4j_rc.api.DefaultApi;
import com.github.sixcorners.rclone4j_rc.model.RcListRequestBody;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

void main() {
  RestClientBuilder.newBuilder()
      .baseUri("http://localhost:5572/")
      .property("microprofile.rest.client.disable.default.mapper", true) // ??
      .register(ForceJsonFilter.class)
      .build(DefaultApi.class)
      .rcList(DefaultApi.RcListRequest.newInstance(), new RcListRequestBody())
      .getCommands()
      .forEach(System.out::println);
}
```
