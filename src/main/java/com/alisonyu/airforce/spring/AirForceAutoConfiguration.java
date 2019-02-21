package com.alisonyu.airforce.spring;

import com.alisonyu.airforce.core.AirForceBuilder;
import com.alisonyu.airforce.core.AirForceContext;
import com.alisonyu.airforce.core.AirForceContextBuilder;
import com.alisonyu.airforce.core.AirForceVerticle;
import com.alisonyu.airforce.web.exception.ExceptionHandler;
import com.alisonyu.airforce.web.router.mounter.RouterMounter;
import com.alisonyu.airforce.microservice.provider.ServiceProvider;
import com.alisonyu.airforce.web.template.TemplateRegistry;
import com.google.common.collect.Lists;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.sstore.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import javax.annotation.Resources;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class AirForceAutoConfiguration implements ApplicationContextAware {

    Logger logger = LoggerFactory.getLogger(AirForceAutoConfiguration.class);

    @Autowired
    ApplicationArguments applicationArguments;


    @Bean("airforceContext")
    @ConditionalOnMissingBean(value = {AirForceContext.class,Vertx.class})
   public AirForceContext airForceContext(
           @Autowired(required = false) Function<Vertx,SessionStore> sessionStoreFactory,
           @Autowired(required = false) List<ExceptionHandler> exceptionHandlers,
           @Autowired(required = false) HttpServerOptions httpServerOptions,
           @Autowired(required = false) List<RouterMounter> routerMounters,
           @Autowired(required = false) List<TemplateRegistry> templateRegistries,
           @Autowired(required = false) VertxOptions vertxOptions,
           @Autowired(required = false) Router router
           ){
       return AirForceContextBuilder.create()
               .args(applicationArguments.getSourceArgs())
               .session(sessionStoreFactory)
               .emberHttpServer(true)
               .exceptionHandler(exceptionHandlers)
               .httpServerOption(httpServerOptions)
               .routerMounters(routerMounters)
               .templateEngines(templateRegistries)
               .vertxOption(vertxOptions)
               .router(router)
               .init();
   }

    @Bean
    @ConditionalOnMissingBean(Vertx.class)
    @ConditionalOnBean(name = "airforceContext")
    public Vertx vertx(@Autowired @Qualifier("airforceContext") AirForceContext airForceContext){
        return airForceContext.getVertx();
    }


   @Bean("airforceContext2")
   @ConditionalOnMissingBean(AirForceContext.class)
   @ConditionalOnBean({Vertx.class})
   public AirForceContext airForceContext2(
           @Autowired(required = false) Function<Vertx,SessionStore> sessionStoreFactory,
           @Autowired(required = false) List<ExceptionHandler> exceptionHandlers,
           @Autowired(required = false) HttpServerOptions httpServerOptions,
           @Autowired(required = false) List<RouterMounter> routerMounters,
           @Autowired(required = false) List<TemplateRegistry> templateRegistries,
           @Autowired(required = false) VertxOptions vertxOptions,
           @Autowired Vertx vertx,
           @Autowired(required = false) Router router
   ){
       return AirForceContextBuilder.create()
               .args(applicationArguments.getSourceArgs())
               .session(sessionStoreFactory)
               .emberHttpServer(true)
               .exceptionHandler(exceptionHandlers)
               .httpServerOption(httpServerOptions)
               .routerMounters(routerMounters)
               .templateEngines(templateRegistries)
               .vertxOption(vertxOptions)
               .vertx(vertx)
               .router(router)
               .init();
   }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AirForceContext airForceContext = applicationContext.getBean(AirForceContext.class);
        applicationContext.getBeansOfType(AirForceVerticle.class)
                .values()
                .forEach(v -> {
                    Class<? extends AirForceVerticle> type = v.getClass();
                    DeploymentOptions deploymentOptions = v.getDeployOption();
                    airForceContext.deployVerticle(()-> applicationContext.getBean(type),deploymentOptions,as -> {
                        if (as.succeeded()){
                            logger.info("deploy {} successfully!",type.getName());
                        }else{
                            logger.error("deploy {} failed!",type.getName(),as.cause());
                        }
                    });
                });

        List<Object> services = applicationContext.getBeansWithAnnotation(ServiceProvider.class)
                .values()
                .stream()
                .filter(o -> o instanceof AbstractVerticle)
                .collect(Collectors.toList());

        airForceContext.publishServices(services);
    }
}
