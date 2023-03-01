package com.github.sasergeev.restclient;

import com.squareup.okhttp.OkHttpClient;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SSLHttpClient extends OkHttpClient {
    private static SSLHttpClient instance;

    private SSLHttpClient(InputStream x509Cert) {
        super();
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            Certificate ca = cf.generateCertificate(x509Cert);
            x509Cert.close();

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            this.setConnectTimeout(120, TimeUnit.SECONDS);
            this.setWriteTimeout(120, TimeUnit.SECONDS);
            this.setReadTimeout(120, TimeUnit.SECONDS);

            this.setSslSocketFactory(sslContext.getSocketFactory());

        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public static SSLHttpClient getInstance(InputStream x509Cert) {
        if (instance == null)
            instance = new SSLHttpClient(x509Cert);
        return instance;
    }
}
