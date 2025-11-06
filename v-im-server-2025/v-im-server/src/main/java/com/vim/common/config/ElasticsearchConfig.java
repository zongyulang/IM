package com.vim.common.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch 客户端配置（适配 ES 8.x Java API Client）
 */
@Configuration
public class ElasticsearchConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${elasticsearch.uris:http://localhost:9200}")
    private List<String> uris;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Value("${elasticsearch.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${elasticsearch.ssl.ca-path:}")
    private String caPath;

    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient() {
        List<HttpHost> hosts = new ArrayList<>();
        for (String uriStr : uris) {
            if (uriStr == null || uriStr.isBlank()) continue;
            URI uri = URI.create(uriStr);
            hosts.add(new HttpHost(uri.getHost(), uri.getPort() == -1 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 9200) : uri.getPort(), uri.getScheme()));
        }
        RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]));

        // Basic Auth（如配置了用户名密码）
        if (username != null && !username.isBlank()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        // SSL 支持（可选，自签证书场景）
        if (sslEnabled) {
            try {
                SSLContext sslContext = buildSSLContextFromCA(caPath);
                builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                );
            } catch (Exception e) {
                log.error("Failed to initialize Elasticsearch SSL context, falling back to default.", e);
            }
        }

        // 默认 header（可选）
        Header[] defaultHeaders = new Header[]{new BasicHeader("Accept", "application/json"), new BasicHeader("Content-type", "application/json")};
        builder.setDefaultHeaders(defaultHeaders);

        return builder.build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient, ObjectMapper objectMapper) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    private SSLContext buildSSLContextFromCA(String caPath) throws Exception {
        if (caPath == null || caPath.isBlank()) {
            throw new IllegalArgumentException("CA path is empty while ssl.enabled = true");
        }
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        try (InputStream is = resolveResource(caPath)) {
            if (is == null) {
                throw new IllegalArgumentException("Cannot load CA certificate from " + caPath);
            }
            // 将单一 CA 证书作为信任项放入 TrustStore
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.security.cert.Certificate cert = cf.generateCertificate(is);
            trustStore.setCertificateEntry("ca", cert);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private InputStream resolveResource(String location) throws Exception {
        if (location.startsWith("classpath:")) {
            String path = location.substring("classpath:".length());
            return this.getClass().getResourceAsStream(path.startsWith("/") ? path : "/" + path);
        }
        if (location.startsWith("file:")) {
            java.nio.file.Path p = java.nio.file.Paths.get(URI.create(location));
            return java.nio.file.Files.newInputStream(p);
        }
        // 作为绝对/相对文件路径读取
        java.nio.file.Path p = java.nio.file.Paths.get(location);
        if (java.nio.file.Files.exists(p)) {
            return java.nio.file.Files.newInputStream(p);
        }
        return null;
    }
}
