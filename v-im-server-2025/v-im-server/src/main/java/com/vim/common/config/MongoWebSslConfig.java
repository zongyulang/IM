package com.vim.common.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Additional Mongo client for web module, connecting to "mydatabase" over TLS with replica set.
 * Properties prefix: mongodb.web.ssl
 */
@Configuration
@ConfigurationProperties(prefix = "mongodb.web.ssl")
public class MongoWebSslConfig {

    /**
     * Example:
     * uri: mongodb://mongodb:27017/?replicaSet=rs0
     * caPem: classpath:mongodb-cert/ca.pem
     * clientPem: classpath:mongodb-cert/mongodb.pem
     * username: adminMainUser
     * password: YpwsYYDS!ThisY957337!
     * authSource: admin
     */
    private String uri;
    private String caPem;
    private String clientPem;
    private String username;
    private String password;
    private String authSource;

    @Bean(name = "webMongoClient")
    public MongoClient webMongoClient() throws Exception {
        // Trust store from CA
        X509Certificate caCert = loadCertificateFromPem(caPem);
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", caCert);

        // Key store from client pem
        Object[] keyAndCertChain = loadPrivateKeyAndCertificateChain(clientPem);
        PrivateKey privateKey = (PrivateKey) keyAndCertChain[0];
        X509Certificate[] certChain = (X509Certificate[]) keyAndCertChain[1];

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("client", privateKey, "".toCharArray(), certChain);

        javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory
                .getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "".toCharArray());

        javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory
                .getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        // 构建带认证的连接字符串
        String connectionString = uri;
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            // 如果URI中没有认证信息,添加认证参数
            String authSourceParam = (authSource != null && !authSource.isEmpty()) ? authSource : "admin";
            if (!uri.contains("@")) {
                // 在 mongodb:// 后插入认证信息
                connectionString = uri.replace("mongodb://", 
                    "mongodb://" + username + ":" + password + "@");
                // 添加 authSource 参数
                if (!connectionString.contains("authSource=")) {
                    connectionString += (connectionString.contains("?") ? "&" : "?") + "authSource=" + authSourceParam;
                }
            }
        }

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .applyToSslSettings(builder -> {
                    builder.enabled(true);
                    builder.context(sslContext);
                    builder.invalidHostNameAllowed(true);
                })
                .build();

        return MongoClients.create(settings);
    }

    @Bean(name = "webMongoTemplate")
    public MongoTemplate webMongoTemplate() throws Exception {
        // Force database name to "mydatabase" regardless of what the URI includes
        return new MongoTemplate(webMongoClient(), "mydatabase");
    }

    // ===== Helpers (duplicated for isolation) =====
    private X509Certificate loadCertificateFromPem(String pemPath) throws Exception {
        Resource resource = createResource(pemPath);
        try (InputStream inputStream = resource.getInputStream()) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(inputStream);
        }
    }

    private Object[] loadPrivateKeyAndCertificateChain(String resourcePath) throws Exception {
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        PrivateKey privateKey = null;
        List<X509Certificate> certificates = new ArrayList<>();

        Resource resource = createResource(resourcePath);
        try (PEMParser parser = new PEMParser(new InputStreamReader(resource.getInputStream()))) {
            Object object;
            while ((object = parser.readObject()) != null) {
                if (object instanceof PrivateKeyInfo) {
                    privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
                } else if (object instanceof X509CertificateHolder) {
                    X509CertificateHolder holder = (X509CertificateHolder) object;
                    JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
                    X509Certificate cert = certConverter.getCertificate(holder);
                    certificates.add(cert);
                } else if (object instanceof X509Certificate) {
                    certificates.add((X509Certificate) object);
                }
            }
        }

        if (privateKey == null) {
            throw new IllegalArgumentException("No private key found in: " + resourcePath);
        }
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("No certificates found in: " + resourcePath);
        }

        return new Object[]{privateKey, certificates.toArray(new X509Certificate[0])};
    }

    private Resource createResource(String path) {
        if (path.startsWith("classpath:")) {
            return new ClassPathResource(path.substring("classpath:".length()));
        } else if (path.startsWith("file:")) {
            return new FileSystemResource(path.substring("file:".length()));
        } else if (path.startsWith("/")) {
            return new FileSystemResource(path);
        } else {
            return new ClassPathResource(path);
        }
    }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public String getCaPem() { return caPem; }
    public void setCaPem(String caPem) { this.caPem = caPem; }
    public String getClientPem() { return clientPem; }
    public void setClientPem(String clientPem) { this.clientPem = clientPem; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getAuthSource() { return authSource; }
    public void setAuthSource(String authSource) { this.authSource = authSource; }
}
