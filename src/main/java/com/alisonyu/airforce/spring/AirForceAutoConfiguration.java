package com.alisonyu.airforce.spring;

import com.alisonyu.airforce.common.AirForceBuilder;
import com.alisonyu.airforce.microservice.AirforceVerticle;
import com.alisonyu.airforce.microservice.core.exception.ExceptionHandler;
import com.alisonyu.airforce.microservice.router.RouterMounter;
import com.alisonyu.airforce.microservice.service.provider.ServiceProvider;
import com.google.common.collect.Lists;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class AirForceAutoConfiguration implements ApplicationContextAware {

    Logger logger = LoggerFactory.getLogger(AirForceAutoConfiguration.class);

    @Autowired
    ApplicationArguments applicationArguments;


    @Bean(initMethod = "init")
    @ConditionalOnMissingBean(Vertx.class)
    public AirForceBuilder airForceBuilder(@Autowired(required = false) VertxOptions vertxOptions,
                                           @Autowired(required = false) ClusterManager clusterManager){

        return AirForceBuilder.build()
                .vertxOptions(vertxOptions)
                .setClusterManager(clusterManager);
    }


    @Bean
    @ConditionalOnMissingBean(Vertx.class)
    public Vertx vertx(AirForceBuilder airForceBuilder){
        return airForceBuilder.getVertx();
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AirForceBuilder builder = applicationContext.getBean(AirForceBuilder.class);
        //if container has user define vertx, use the vertx
        Vertx vertx = applicationContext.getBean(Vertx.class);
        if (! Objects.equals(builder.getVertx(),vertx)){
            builder.vertx(vertx);
        }
        //register routerMounter
        Map<String, RouterMounter> mounterMap = applicationContext.getBeansOfType(RouterMounter.class);
        builder.routerMounters(Lists.newArrayList(mounterMap.values()));

        //register exceptional handler
        Map<String,ExceptionHandler> handlerMap = applicationContext.getBeansOfType(ExceptionHandler.class);
        builder.restExceptionHandler(Lists.newArrayList(handlerMap.values()));

        //get all prototype rest verticle from container
        Map<String, AirforceVerticle> verticles = applicationContext.getBeansOfType(AirforceVerticle.class);
        Set<Class<? extends AirforceVerticle>> classSet = verticles.values()
                .stream()
                .map(v -> v.getClass())
                .distinct()
                .collect(Collectors.toSet());

        //deploy verticle
        builder.airforceVerticles(classSet,clazz -> applicationContext.getBean(clazz));

        //deploy services
        Map<String,Object> serviceMap = applicationContext.getBeansWithAnnotation(ServiceProvider.class);
        List<Object> services = serviceMap.values()
                .stream()
                .filter(o -> !AirforceVerticle.class.isAssignableFrom(o.getClass()))
                .collect(Collectors.toList());
        builder.publish(services);

        //run start method
        builder.run(AirForceAutoConfiguration.class,applicationArguments.getSourceArgs());
    }





}
