package org.iotp.server.config;

import com.builtamont.cassandra.migration.CassandraMigration;
import com.builtamont.cassandra.migration.api.configuration.KeyspaceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;

/**
 * Created by SonCD on 21/08/2021
 */
@Configuration
public class CassandraMigrationConfiguration {

    private static final Logger logger = LoggerFactory
            .getLogger(CassandraMigrationConfiguration.class);
    @Value("${cassandra.ip}")
    private String ip;
    @Value("${cassandra.port}")
    private Integer port;
    @Value("${cassandra.keyspace_name}")
    private String keyspaceName;
    @Value("${cassandra.migration}")
    private String migrationsPath;

    @Bean
    InitializingBean migrationCassandra() {
        return () -> {
            String[] locations = {migrationsPath};
            logger.info("contactPoints: {}, port: {}, keyspaceName: {}, " +
                            "scriptLocation: {}",
                    ip, port, keyspaceName, locations);

            KeyspaceConfiguration keyspaceConfig = getKeyspaceConfiguration();
            CassandraMigration cm = getCassandraMigration(locations,
                    keyspaceConfig);
        };
    }

    @NotNull
    private KeyspaceConfiguration getKeyspaceConfiguration() {
        KeyspaceConfiguration keyspaceConfig = new KeyspaceConfiguration();
        keyspaceConfig.setName(keyspaceName);
        keyspaceConfig.getClusterConfig().setContactpoints(ip.split(","));
        keyspaceConfig.getClusterConfig().setPort(port);
        return keyspaceConfig;
    }

    @NotNull
    private CassandraMigration getCassandraMigration(String[] locations,
                                                     KeyspaceConfiguration keyspaceConfig) {
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(locations);
        cm.setKeyspaceConfig(keyspaceConfig);
        cm.migrate();
        return cm;
    }

}
