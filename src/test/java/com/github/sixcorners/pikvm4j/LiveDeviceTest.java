package com.github.sixcorners.pikvm4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.sixcorners.pikvm4j.api.AtxApi;
import com.github.sixcorners.pikvm4j.api.AuthApi;
import com.github.sixcorners.pikvm4j.api.ExportApi;
import com.github.sixcorners.pikvm4j.api.GpioApi;
import com.github.sixcorners.pikvm4j.api.HidApi;
import com.github.sixcorners.pikvm4j.api.MsdApi;
import com.github.sixcorners.pikvm4j.api.RedfishApi;
import com.github.sixcorners.pikvm4j.api.StreamerApi;
import com.github.sixcorners.pikvm4j.api.SwitchApi;
import com.github.sixcorners.pikvm4j.api.SystemApi;
import com.github.sixcorners.pikvm4j.model.InfoResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Read-only checks against a real PiKVM. Configured through the {@code PIKVM_URL}, {@code
 * PIKVM_USER}, {@code PIKVM_PASSWORD} and {@code PIKVM_TOTP_SECRET} variables, taken from the
 * environment or from a git-ignored {@code .env} file in the project directory (see {@code
 * .env.example}). Skipped when no URL is configured.
 */
class LiveDeviceTest {
  private static RestClientBuilder builder;
  private static String user;
  private static String password;
  private static String totpSecret;

  @BeforeAll
  static void setUp() throws IOException, GeneralSecurityException {
    Map<String, String> env = loadEnv();
    String url = env.get("PIKVM_URL");
    assumeTrue(url != null && !url.isBlank(), "PIKVM_URL not configured");
    user = env.getOrDefault("PIKVM_USER", "admin");
    password = env.getOrDefault("PIKVM_PASSWORD", "");
    totpSecret = env.getOrDefault("PIKVM_TOTP_SECRET", "");
    SSLContext insecure = SSLContext.getInstance("TLS");
    insecure.init(null, new TrustManager[] {new TrustAll()}, null);
    builder =
        RestClientBuilder.newBuilder()
            .baseUri(URI.create(url))
            // The default mapper needs a MicroProfile Config implementation on the classpath.
            .property("microprofile.rest.client.disable.default.mapper", true)
            .sslContext(insecure)
            .hostnameVerifier((host, session) -> true)
            .register(
                totpSecret.isBlank()
                    ? KvmdAuthFilter.of(user, password)
                    : KvmdAuthFilter.withTotp(user, password, totpSecret));
  }

  /** {@code KEY=VALUE} lines from {@code .env}, overridden by real environment variables. */
  private static Map<String, String> loadEnv() throws IOException {
    Map<String, String> env = new HashMap<>();
    Path dotEnv = Path.of(".env");
    if (Files.exists(dotEnv)) {
      for (String line : Files.readAllLines(dotEnv)) {
        String trimmed = line.strip();
        int eq = trimmed.indexOf('=');
        if (trimmed.isEmpty() || trimmed.startsWith("#") || eq < 0) {
          continue;
        }
        env.put(trimmed.substring(0, eq).strip(), trimmed.substring(eq + 1).strip());
      }
    }
    System.getenv().forEach((k, v) -> env.merge(k, v, (fromFile, fromEnv) -> fromEnv));
    return env;
  }

  private static <T> T api(Class<T> type) {
    return builder.build(type);
  }

  @Test
  void auth() {
    AuthApi auth = api(AuthApi.class);
    assertTrue(auth.checkAuth().getOk());
    // Form-encoded login with a short-lived token; the auth filter headers are ignored here.
    assertTrue(
        auth.login(
                AuthApi.LoginRequest.newInstance()
                    .user(user)
                    .passwd(
                        password + (totpSecret.isBlank() ? "" : KvmdAuthFilter.totp(totpSecret)))
                    .expire(60))
            .getOk());
  }

  @Test
  void info() {
    InfoResponse info = api(SystemApi.class).getInfo(SystemApi.GetInfoRequest.newInstance());
    assertTrue(info.getOk());
    assertNotNull(info.getResult().getSystem().getKvmd().getVersion());
    assertNotNull(info.getResult().getHw().getPlatform().getBase());

    InfoResponse hwOnly =
        api(SystemApi.class).getInfo(SystemApi.GetInfoRequest.newInstance().fields("hw"));
    assertNotNull(hwOnly.getResult().getHw());
    assertNull(hwOnly.getResult().getSystem());
  }

  @Test
  void log() throws IOException {
    try (InputStream log =
        api(SystemApi.class).getLog(SystemApi.GetLogRequest.newInstance().seek(60))) {
      String text = new String(log.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(text.contains("kvmd"), text);
    }
  }

  @Test
  void logFollow() throws IOException {
    // The stream never ends, so read a single record and close it.
    try (InputStream log =
            api(SystemApi.class)
                .getLog(SystemApi.GetLogRequest.newInstance().seek(60).follow(true));
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(log, StandardCharsets.UTF_8))) {
      String first = reader.readLine();
      assertNotNull(first);
      assertTrue(first.startsWith("["), first);
    }
  }

  @Test
  void prometheus() {
    assertTrue(api(ExportApi.class).getPrometheusMetrics().contains("pikvm_atx_enabled"));
  }

  @Test
  void hid() {
    HidApi hid = api(HidApi.class);
    assertNotNull(hid.getHidState().getResult().getMouse().getOutputs().getActive());
    assertFalse(hid.getHidKeymaps().getResult().getKeymaps().getAvailable().isEmpty());
    assertNotNull(hid.getHidInactivity().getResult().getInactivity());
  }

  @Test
  void atx() {
    assertNotNull(api(AtxApi.class).getAtxState().getResult().getLeds().getPower());
  }

  @Test
  void msd() {
    assertNotNull(api(MsdApi.class).getMsdState().getResult().getDrive().getCdrom());
  }

  @Test
  void gpio() {
    assertNotNull(api(GpioApi.class).getGpioState().getResult().getState().getOutputs());
  }

  @Test
  void streamer() {
    StreamerApi streamer = api(StreamerApi.class);
    assertNotNull(streamer.getStreamerState().getResult().getParams().getQuality());
    assertNotNull(streamer.getOcrState().getResult().getOcr().getEnabled());
    byte[] jpeg =
        streamer.takeSnapshot(StreamerApi.TakeSnapshotRequest.newInstance().allowOffline(true));
    assertEquals((byte) 0xFF, jpeg[0]);
    assertEquals((byte) 0xD8, jpeg[1]);
  }

  @Test
  void switchState() {
    assertNotNull(api(SwitchApi.class).getSwitchState().getResult().getSummary().getSynced());
  }

  @Test
  void redfish() {
    RedfishApi redfish = api(RedfishApi.class);
    assertEquals("RootService", redfish.getRedfishRoot().getId());
    assertFalse(redfish.getRedfishSystems().getMembers().isEmpty());
    assertNotNull(
        redfish
            .getRedfishSystem(RedfishApi.GetRedfishSystemRequest.newInstance().systemId("0"))
            .getPowerState());
  }

  private static final class TrustAll implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {}

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {}

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }
}
