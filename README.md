This is a batteries excluded REST client library for the [PiKVM HTTP API](https://docs.pikvm.org/api/).

The client interfaces and models are generated from [`openapi.yaml`](openapi.yaml), an OpenAPI description of the `kvmd` API written from the official docs and the `kvmd` sources. Each API section (`auth`, `system`, `hid`, `atx`, `msd`, `gpio`, `streamer`, `switch`, `redfish`, `export`) becomes one `*Api` interface. The WebSocket event stream (`/api/ws`) is not covered.

You need both a MicroProfile Rest Client implementation and a Jakarta JSON Binding implementation to use it.

```java
//DEPS org.jboss.resteasy.microprofile:microprofile-rest-client:3.0.1.Final
//DEPS org.jboss.resteasy:resteasy-json-binding-provider:7.0.2.Final
//DEPS com.github.sixcorners:pikvm4j:<tag>
//REPOS central,https://jitpack.io

import com.github.sixcorners.pikvm4j.KvmdAuthFilter;
import com.github.sixcorners.pikvm4j.api.AtxApi;
import com.github.sixcorners.pikvm4j.api.SystemApi;
import java.net.URI;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

void main() {
  var builder =
      RestClientBuilder.newBuilder()
          .baseUri(URI.create("https://pikvm/"))
          // Only needed when no MicroProfile Config implementation is on the classpath.
          .property("microprofile.rest.client.disable.default.mapper", true)
          // Or KvmdAuthFilter.of("admin", "admin") without two-factor authentication.
          .register(KvmdAuthFilter.withTotp("admin", "admin", "<contents of /etc/kvmd/totp.secret>"));

  var info = builder.build(SystemApi.class).getInfo(SystemApi.GetInfoRequest.newInstance().fields("system"));
  System.out.println(info.getResult().getSystem().getKvmd().getVersion());

  builder.build(AtxApi.class).getAtxState().getResult().getLeds().getPower();
}
```

`KvmdAuthFilter` sends the `X-KVMD-User`/`X-KVMD-Passwd` headers with every request and, when given the TOTP secret, appends the current one-time code to the password. Any other MicroProfile Rest Client authentication mechanism works too; see the security schemes in `openapi.yaml`.

PiKVM ships with a self-signed certificate. Either install the certificate, or pass a trusting `sslContext(...)` and `hostnameVerifier(...)` to the builder.

Failed requests (HTTP status 400 and above) throw `com.github.sixcorners.pikvm4j.api.ApiException`; the body is `{"ok": false, "result": {"error": ..., "error_msg": ...}}`.

## Development

`gradle build` regenerates the sources from `openapi.yaml` into `build/generate-resources`, compiles and runs the tests. The generated sources are not committed.

`LiveDeviceTest` runs read-only requests against a real PiKVM. Copy `.env.example` to `.env` (git-ignored) and fill in `PIKVM_URL`, `PIKVM_USER`, `PIKVM_PASSWORD` and `PIKVM_TOTP_SECRET`; real environment variables with the same names take precedence. Without a `PIKVM_URL` the test is skipped.

The GitHub workflow builds every push to `main` (and weekly) and, when the commit is not tagged yet, creates the next patch release tag with [axion-release](https://github.com/allegro/axion-release-plugin). Releases are consumed through [JitPack](https://jitpack.io/#sixcorners/pikvm4j). To pick a different next version run `gradle markNextVersion -Prelease.version=X.Y.Z` before pushing.
