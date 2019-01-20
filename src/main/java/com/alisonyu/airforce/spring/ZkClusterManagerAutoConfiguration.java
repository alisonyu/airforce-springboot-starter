package com.alisonyu.airforce.spring;


import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

public class ZkClusterManagerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ClusterManager.class)
    public ClusterManager clusterManager(){
        return new ZookeeperClusterManager();
    }

}
