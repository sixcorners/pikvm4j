package com.github.sixcorners.pikvm4j;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class KvmdAuthFilterTest {
  @Test
  void base32() {
    assertArrayEquals(
        "12345678901234567890".getBytes(StandardCharsets.US_ASCII),
        KvmdAuthFilter.base32Decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"));
  }

  @Test
  void rfc6238Vectors() {
    byte[] key = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    assertEquals("287082", KvmdAuthFilter.totp(key, at(59)));
    assertEquals("081804", KvmdAuthFilter.totp(key, at(1111111109)));
    assertEquals("050471", KvmdAuthFilter.totp(key, at(1111111111)));
    assertEquals("005924", KvmdAuthFilter.totp(key, at(1234567890)));
    assertEquals("279037", KvmdAuthFilter.totp(key, at(2000000000)));
  }

  private static Clock at(long epochSeconds) {
    return Clock.fixed(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
  }
}
