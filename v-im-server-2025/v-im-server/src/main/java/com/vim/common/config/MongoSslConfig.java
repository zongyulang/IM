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
@ConfigurationProperties(prefix = "mongodb.ssl")
@Configuration
public class MongoSslConfig {
    private String uri;
    private String caPem;
    private String clientPem;
    @Bean
    public MongoClient mongoClient() throws Exception {
        // 1. 加载CA证书 (构建信任库)
        X509Certificate caCert = loadCertificateFromPem(caPem);//loadCertificateFromPem(new ClassPathResource(caPem));
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", caCert);

        // 2. 从client.pem加载客户端证书和私钥 (构建密钥库)
        // 假设CLIENT_PEM_PATH指向的文件同时包含私钥和证书链
        Object[] keyAndCertChain = loadPrivateKeyAndCertificateChain(clientPem);
        PrivateKey privateKey = (PrivateKey) keyAndCertChain[0];
        X509Certificate[] certChain = (X509Certificate[]) keyAndCertChain[1];

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        // 将私钥和证书链存入密钥库。别名可自定义，密码可设为空字符串（若私钥未加密）
        keyStore.setKeyEntry("client", privateKey, "".toCharArray(), certChain);

        // 3. 初始化SSLContext
        javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "".toCharArray()); // 使用与setKeyEntry一致的密码

        javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        // 4. 配置MongoClientSettings
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToSslSettings(builder -> {
                    builder.enabled(true);
                    builder.context(sslContext);
                    builder.invalidHostNameAllowed(true); // 允许无效主机名
                })
                .build();

        return MongoClients.create(settings);
    }


    private X509Certificate loadCertificateFromPem(Resource resource) throws Exception {
        try (PEMParser parser = new PEMParser(new InputStreamReader(resource.getInputStream()))) {
            Object object = parser.readObject();

            if (object instanceof X509Certificate) {
                // 如果解析器直接返回X509Certificate，则直接返回
                return (X509Certificate) object;
            } else if (object instanceof X509CertificateHolder) {
                // *** 关键修改：使用JcaX509CertificateConverter进行转换 ***
                X509CertificateHolder holder = (X509CertificateHolder) object;
                JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
                return converter.getCertificate(holder); // 这个方法确实存在
            } else {
                throw new IllegalArgumentException("Expected X509Certificate or X509CertificateHolder, got: " +
                        (object != null ? object.getClass() : "null"));
            }
        }
    }

    private X509Certificate loadCertificateFromPem(String pemPath) throws Exception {
        Resource resource;

        // 判断是 classpath 路径还是文件系统路径
        if (pemPath.startsWith("classpath:")) {
            resource = new ClassPathResource(pemPath.substring("classpath:".length()));
        } else if (pemPath.startsWith("file:")) {
            resource = new FileSystemResource(pemPath.substring("file:".length()));
        } else {
            // 默认为文件系统绝对路径
            resource = new FileSystemResource(pemPath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(inputStream);
        }
    }


    /**
     * 从PEM文件加载私钥和证书链。
     * 返回一个Object数组，其中[0]是PrivateKey，[1]是X509Certificate[]。
     */
    private Object[] loadPrivateKeyAndCertificateChain(Resource resource) throws Exception {
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        PrivateKey privateKey = null;
        List<X509Certificate> certificates = new ArrayList<>();

        try (PEMParser parser = new PEMParser(new InputStreamReader(resource.getInputStream()))) {
            Object object;
            while ((object = parser.readObject()) != null) {
                if (object instanceof PrivateKeyInfo) {
                    // 处理私钥
                    privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
                } else if (object instanceof org.bouncycastle.cert.X509CertificateHolder) {

                    X509CertificateHolder holder = (X509CertificateHolder) object;
                    JcaX509CertificateConverter converter2 = new JcaX509CertificateConverter();
                    X509Certificate cert = converter2.getCertificate(holder); // This method exists
                    certificates.add(cert);

                } else if (object instanceof X509Certificate) {
                    // 如果解析器直接返回了X509Certificate，则直接使用
                    certificates.add((X509Certificate) object);
                }
                // 可以根据需要在此处添加对其他类型（如EncryptedPrivateKeyInfo）的处理逻辑
            }
        }

        if (privateKey == null) {
            throw new IllegalArgumentException("No private key found in: " + resource.getFilename());
        }
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("No certificates found in: " + resource.getFilename());
        }

        return new Object[]{privateKey, certificates.toArray(new X509Certificate[0])};
    }

    private Object[] loadPrivateKeyAndCertificateChain(String resourcePath) throws Exception {
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        PrivateKey privateKey = null;
        List<X509Certificate> certificates = new ArrayList<>();

        // 创建适当的 Resource 对象
        Resource resource = createResource(resourcePath);

        try (PEMParser parser = new PEMParser(new InputStreamReader(resource.getInputStream()))) {
            Object object;
            while ((object = parser.readObject()) != null) {
                if (object instanceof PrivateKeyInfo) {
                    // 处理私钥
                    privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
                } else if (object instanceof org.bouncycastle.cert.X509CertificateHolder) {
                    X509CertificateHolder holder = (org.bouncycastle.cert.X509CertificateHolder) object;
                    JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
                    X509Certificate cert = certConverter.getCertificate(holder);
                    certificates.add(cert);
                } else if (object instanceof X509Certificate) {
                    // 如果解析器直接返回了X509Certificate，则直接使用
                    certificates.add((X509Certificate) object);
                }
                // 可以根据需要在此处添加对其他类型（如EncryptedPrivateKeyInfo）的处理逻辑
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

    /**
     * 创建适当的 Resource 对象，支持 classpath 和文件系统路径
     */
    private Resource createResource(String path) {
        if (path.startsWith("classpath:")) {
            return new ClassPathResource(path.substring("classpath:".length()));
        } else if (path.startsWith("file:")) {
            return new FileSystemResource(path.substring("file:".length()));
        } else if (path.startsWith("/")) {
            // 绝对路径
            return new FileSystemResource(path);
        } else {
            // 默认为 classpath 相对路径
            return new ClassPathResource(path);
        }
    }

    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongoClient(), "imdatabase");
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getCaPem() {
        return caPem;
    }

    public void setCaPem(String caPem) {
        this.caPem = caPem;
    }

    public String getClientPem() {
        return clientPem;
    }

    public void setClientPem(String clientPem) {
        this.clientPem = clientPem;
    }
}