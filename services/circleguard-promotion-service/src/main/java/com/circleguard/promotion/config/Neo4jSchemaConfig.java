package com.circleguard.promotion.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.Neo4jClient;

@Configuration
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Profile("!test")
public class Neo4jSchemaConfig {

    private final Neo4jClient neo4jClient;

    @PostConstruct
    public void initializeSchema() {
        log.info("Initializing Neo4j Schema (Indices)...");

        // 1. User anonymousId Index
        neo4jClient.query("CREATE INDEX user_anon_id_idx IF NOT EXISTS FOR (n:User) ON (n.anonymousId)").run();

        // 2. User status Index
        neo4jClient.query("CREATE INDEX user_status_idx IF NOT EXISTS FOR (n:User) ON (n.status)").run();

        // 3. Relationship startTime Index (Supported in Neo4j 5+)
        neo4jClient.query("CREATE INDEX encounter_time_idx IF NOT EXISTS FOR ()-[r:ENCOUNTERED]-() ON (r.startTime)").run();
        
        log.info("Neo4j Indices initialized successfully.");
    }
}
