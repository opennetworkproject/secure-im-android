package com.opennetwork.secureim.push;


import android.content.Context;
import android.support.annotation.Nullable;

import com.opennetwork.secureim.BuildConfig;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.imservice.api.push.TrustStore;
import com.opennetwork.imservice.internal.configuration.OpenNetworkCdnUrl;
import com.opennetwork.imservice.internal.configuration.OpenNetworkServiceConfiguration;
import com.opennetwork.imservice.internal.configuration.OpenNetworkServiceUrl;

import java.util.HashMap;
import java.util.Map;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.TlsVersion;

public class OpenNetworkServiceNetworkAccess {

  private static final String TAG = OpenNetworkServiceNetworkAccess.class.getName();

  private static final String APPSPOT_SERVICE_REFLECTOR_HOST = "opennetwork-reflector-meek.appspot.com";
  private static final String APPSPOT_CDN_REFLECTOR_HOST     = "opennetwork-cdn-reflector.appspot.com";

  private static final ConnectionSpec GMAPS_CONNECTION_SPEC = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_2)
      .cipherSuites(CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA)
      .supportsTlsExtensions(true)
      .build();

  private static final ConnectionSpec GMAIL_CONNECTION_SPEC = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_2)
      .cipherSuites(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA,
                    CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_RC4_128_SHA,
                    CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV)
      .supportsTlsExtensions(true)
      .build();

  private static final ConnectionSpec PLAY_CONNECTION_SPEC = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_2)
      .cipherSuites(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_RC4_128_SHA,
                    CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV)
      .supportsTlsExtensions(true)
      .build();


  private final Map<String, OpenNetworkServiceConfiguration> censorshipConfiguration;
  private final String[]                                censoredCountries;
  private final OpenNetworkServiceConfiguration              uncensoredConfiguration;

  public OpenNetworkServiceNetworkAccess(Context context) {
    final TrustStore       googleTrustStore      = new GoogleFrontingTrustStore(context);
    final OpenNetworkServiceUrl baseGoogleService     = new OpenNetworkServiceUrl("https://www.google.com", APPSPOT_SERVICE_REFLECTOR_HOST, googleTrustStore, GMAIL_CONNECTION_SPEC);
    final OpenNetworkServiceUrl baseAndroidService    = new OpenNetworkServiceUrl("https://android.clients.google.com", APPSPOT_SERVICE_REFLECTOR_HOST, googleTrustStore, PLAY_CONNECTION_SPEC);
    final OpenNetworkServiceUrl mapsOneAndroidService = new OpenNetworkServiceUrl("https://clients3.google.com", APPSPOT_SERVICE_REFLECTOR_HOST, googleTrustStore, GMAPS_CONNECTION_SPEC);
    final OpenNetworkServiceUrl mapsTwoAndroidService = new OpenNetworkServiceUrl("https://clients4.google.com", APPSPOT_SERVICE_REFLECTOR_HOST, googleTrustStore, GMAPS_CONNECTION_SPEC);
    final OpenNetworkServiceUrl mailAndroidService    = new OpenNetworkServiceUrl("https://mail.google.com", APPSPOT_SERVICE_REFLECTOR_HOST, googleTrustStore, GMAIL_CONNECTION_SPEC);

    final OpenNetworkCdnUrl     baseGoogleCdn         = new OpenNetworkCdnUrl("https://www.google.com", APPSPOT_SERVICE_REFLECTOR_HOST, googleTrustStore, GMAIL_CONNECTION_SPEC);
    final OpenNetworkCdnUrl     baseAndroidCdn        = new OpenNetworkCdnUrl("https://android.clients.google.com", APPSPOT_SERVICE_REFLECTOR_HOST, googleTrustStore, PLAY_CONNECTION_SPEC);
    final OpenNetworkCdnUrl     mapsOneAndroidCdn     = new OpenNetworkCdnUrl("https://clients3.google.com", APPSPOT_SERVICE_REFLECTOR_HOST, googleTrustStore, GMAPS_CONNECTION_SPEC);
    final OpenNetworkCdnUrl     mapsTwoAndroidCdn     = new OpenNetworkCdnUrl("https://clients4.google.com", APPSPOT_SERVICE_REFLECTOR_HOST, googleTrustStore, GMAPS_CONNECTION_SPEC);
    final OpenNetworkCdnUrl     mailAndroidCdn        = new OpenNetworkCdnUrl("https://mail.google.com", APPSPOT_SERVICE_REFLECTOR_HOST, googleTrustStore, GMAIL_CONNECTION_SPEC);

    this.censorshipConfiguration = new HashMap<String, OpenNetworkServiceConfiguration>() {{
      put("+20", new OpenNetworkServiceConfiguration(new OpenNetworkServiceUrl[] {new OpenNetworkServiceUrl("https://www.google.com.eg",
                                                                                             APPSPOT_SERVICE_REFLECTOR_HOST,
                                                                                             googleTrustStore, GMAIL_CONNECTION_SPEC),
                                                                        baseAndroidService, mapsOneAndroidService, mapsTwoAndroidService, mailAndroidService},
                                                new OpenNetworkCdnUrl[] {new OpenNetworkCdnUrl("https://www.google.com.eg",
                                                                                     APPSPOT_CDN_REFLECTOR_HOST,
                                                                                     googleTrustStore, GMAIL_CONNECTION_SPEC),
                                                                    baseAndroidCdn, mapsOneAndroidCdn, mapsTwoAndroidCdn, mailAndroidCdn, mailAndroidCdn}));

      put("+971", new OpenNetworkServiceConfiguration(new OpenNetworkServiceUrl[] {new OpenNetworkServiceUrl("https://www.google.ae",
                                                                                              APPSPOT_SERVICE_REFLECTOR_HOST,
                                                                                              googleTrustStore, GMAIL_CONNECTION_SPEC),
                                                                         baseAndroidService, baseGoogleService, mapsOneAndroidService, mapsTwoAndroidService, mailAndroidService},
                                                 new OpenNetworkCdnUrl[] {new OpenNetworkCdnUrl("https://www.google.ae",
                                                                                      APPSPOT_CDN_REFLECTOR_HOST,
                                                                                      googleTrustStore, GMAIL_CONNECTION_SPEC),
                                                                     baseAndroidCdn, baseGoogleCdn, mapsOneAndroidCdn, mapsTwoAndroidCdn, mailAndroidCdn}));

      put("+968", new OpenNetworkServiceConfiguration(new OpenNetworkServiceUrl[] {new OpenNetworkServiceUrl("https://www.google.com.om",
                                                                                              APPSPOT_SERVICE_REFLECTOR_HOST,
                                                                                              googleTrustStore, GMAIL_CONNECTION_SPEC),
                                                                         baseAndroidService, baseGoogleService, mapsOneAndroidService, mapsTwoAndroidService, mailAndroidService},
                                                 new OpenNetworkCdnUrl[] {new OpenNetworkCdnUrl("https://www.google.com.om",
                                                                                      APPSPOT_CDN_REFLECTOR_HOST,
                                                                                      googleTrustStore, GMAIL_CONNECTION_SPEC),
                                                                     baseAndroidCdn, baseGoogleCdn, mapsOneAndroidCdn, mapsTwoAndroidCdn, mailAndroidCdn}));

      put("+974", new OpenNetworkServiceConfiguration(new OpenNetworkServiceUrl[] {new OpenNetworkServiceUrl("https://www.google.com.qa",
                                                                                              APPSPOT_SERVICE_REFLECTOR_HOST,
                                                                                              googleTrustStore, GMAIL_CONNECTION_SPEC),
                                                                         baseAndroidService, baseGoogleService, mapsOneAndroidService, mapsTwoAndroidService, mailAndroidService},
                                                 new OpenNetworkCdnUrl[] {new OpenNetworkCdnUrl("https://www.google.com.qa",
                                                                                      APPSPOT_CDN_REFLECTOR_HOST,
                                                                                      googleTrustStore, GMAIL_CONNECTION_SPEC),
                                                                     baseAndroidCdn, baseGoogleCdn, mapsOneAndroidCdn, mapsTwoAndroidCdn, mailAndroidCdn}));
    }};

    this.uncensoredConfiguration = new OpenNetworkServiceConfiguration(new OpenNetworkServiceUrl[] {new OpenNetworkServiceUrl(BuildConfig.opennetwork_URL, new OpenNetworkServiceTrustStore(context))},
                                                                  new OpenNetworkCdnUrl[] {new OpenNetworkCdnUrl(BuildConfig.opennetwork_CDN_URL, new OpenNetworkServiceTrustStore(context))});

    this.censoredCountries = this.censorshipConfiguration.keySet().toArray(new String[0]);
  }

  public OpenNetworkServiceConfiguration getConfiguration(Context context) {
    String localNumber = TextSecurePreferences.getLocalNumber(context);
    return getConfiguration(localNumber);
  }

  public OpenNetworkServiceConfiguration getConfiguration(@Nullable String localNumber) {
    if (localNumber == null) return this.uncensoredConfiguration;

    for (String censoredRegion : this.censoredCountries) {
      if (localNumber.startsWith(censoredRegion)) {
        return this.censorshipConfiguration.get(censoredRegion);
      }
    }

    return this.uncensoredConfiguration;
  }

  public boolean isCensored(Context context) {
    return getConfiguration(context) != this.uncensoredConfiguration;
  }

  public boolean isCensored(String number) {
    return getConfiguration(number) != this.uncensoredConfiguration;
  }

}
