package org.bottiger.podcast.cloud.soundwavesapp;

import android.support.annotation.NonNull;

import com.squareup.okhttp.CertificatePinner;
import com.squareup.okhttp.OkHttpClient;

import org.bottiger.podcast.ApplicationConfiguration;

/**
 * Created by apl on 22-04-2015.
 */
public class PinnedHttpCLient {

    private static final String hostname = ApplicationConfiguration.CERTIFICATE_HOSTNAME;
    private static final String sBaseUrl = "https://" + hostname + "/";
    private static final String pinString = "sha1/" + ApplicationConfiguration.CERTIFICATE_PIN_SHA1;

    private static final OkHttpClient sClient = new OkHttpClient();
    private static final CertificatePinner sCertificatePinner = new CertificatePinner.Builder()
            .add(hostname, pinString)
            .build();

    @NonNull
    public static OkHttpClient getHttpClient() {
        sClient.setCertificatePinner(sCertificatePinner);
        return sClient;
    }

    @NonNull
    public static String getBaseUrl() {
        return sBaseUrl;
    }

}
