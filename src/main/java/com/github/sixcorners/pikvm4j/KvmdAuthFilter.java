package com.github.sixcorners.pikvm4j;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.Locale;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Adds the {@code X-KVMD-User} and {@code X-KVMD-Passwd} headers to every request.
 *
 * <p>Register an instance with {@code RestClientBuilder.register(...)}. When two-factor
 * authentication is enabled on the PiKVM use {@link #withTotp(String, String, String)} so the
 * current one-time code is appended to the password of each request.
 */
public final class KvmdAuthFilter implements ClientRequestFilter {
  public static final String USER_HEADER = "X-KVMD-User";
  public static final String PASSWD_HEADER = "X-KVMD-Passwd";

  private final String user;
  private final Supplier<String> passwd;

  private KvmdAuthFilter(String user, Supplier<String> passwd) {
    this.user = user;
    this.passwd = passwd;
  }

  /** Plain user name and password authentication. */
  public static KvmdAuthFilter of(String user, String passwd) {
    return new KvmdAuthFilter(user, () -> passwd);
  }

  /**
   * Password authentication with a TOTP code appended, for a PiKVM with 2FA enabled.
   *
   * @param totpSecret the base32 secret from {@code /etc/kvmd/totp.secret}
   */
  public static KvmdAuthFilter withTotp(String user, String passwd, String totpSecret) {
    byte[] key = base32Decode(totpSecret);
    return new KvmdAuthFilter(user, () -> passwd + totp(key, Clock.systemUTC()));
  }

  @Override
  public void filter(ClientRequestContext requestContext) {
    requestContext.getHeaders().putSingle(USER_HEADER, user);
    requestContext.getHeaders().putSingle(PASSWD_HEADER, passwd.get());
  }

  /** The current six digit TOTP code (RFC 6238, SHA-1, 30 second period) for a base32 secret. */
  public static String totp(String base32Secret) {
    return totp(base32Decode(base32Secret), Clock.systemUTC());
  }

  static String totp(byte[] key, Clock clock) {
    long counter = clock.millis() / 1000 / 30;
    byte[] msg = new byte[8];
    for (int i = 7; i >= 0; i--) {
      msg[i] = (byte) counter;
      counter >>>= 8;
    }
    byte[] hash;
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      hash = mac.doFinal(msg);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
    int offset = hash[hash.length - 1] & 0xf;
    int code =
        ((hash[offset] & 0x7f) << 24)
            | ((hash[offset + 1] & 0xff) << 16)
            | ((hash[offset + 2] & 0xff) << 8)
            | (hash[offset + 3] & 0xff);
    return String.format(Locale.ROOT, "%06d", code % 1_000_000);
  }

  static byte[] base32Decode(String secret) {
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    String s = secret.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
    byte[] out = new byte[s.length() * 5 / 8];
    int buffer = 0;
    int bits = 0;
    int index = 0;
    for (char c : s.toCharArray()) {
      int value = alphabet.indexOf(c);
      if (value < 0) {
        throw new IllegalArgumentException("Invalid base32 character: " + c);
      }
      buffer = (buffer << 5) | value;
      bits += 5;
      if (bits >= 8) {
        out[index++] = (byte) (buffer >> (bits - 8));
        bits -= 8;
      }
    }
    return out;
  }
}
