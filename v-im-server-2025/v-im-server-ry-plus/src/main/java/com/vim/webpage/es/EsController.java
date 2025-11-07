package com.vim.webpage.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/es")
public class EsController {

    private static final Logger log = LoggerFactory.getLogger(EsController.class);

    private final ElasticsearchClient esClient;

    public EsController(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @GetMapping("/health")
    public Map<String, Object> health() throws IOException {
        Map<String, Object> result = new HashMap<>();
        try {
            HealthResponse health = esClient.cluster().health();
            result.put("cluster_name", health.clusterName());
            result.put("status", health.status().jsonValue());
            result.put("number_of_nodes", health.numberOfNodes());
            result.put("active_primary_shards", health.activePrimaryShards());
            result.put("active_shards", health.activeShards());
        } catch (ElasticsearchException e) {
            log.error("Elasticsearch error", e);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
