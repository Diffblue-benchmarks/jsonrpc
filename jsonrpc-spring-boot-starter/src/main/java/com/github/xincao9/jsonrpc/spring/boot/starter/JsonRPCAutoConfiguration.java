/*
 * Copyright 2019 xincao9@gmail.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.xincao9.jsonrpc.spring.boot.starter;

import com.github.xincao9.jsonrpc.core.DiscoveryService;
import com.github.xincao9.jsonrpc.core.config.ClientConfig;
import com.github.xincao9.jsonrpc.core.JsonRPCClient;
import com.github.xincao9.jsonrpc.core.impl.JsonRPCClientImpl;
import com.github.xincao9.jsonrpc.core.constant.ClientConsts;
import com.github.xincao9.jsonrpc.core.constant.ServerConsts;
import com.github.xincao9.jsonrpc.core.JsonRPCServer;
import com.github.xincao9.jsonrpc.core.impl.JsonRPCServerImpl;
import com.github.xincao9.jsonrpc.core.config.ServerConfig;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * 自动配置类
 *
 * @author xincao9@gmail.com
 */
public class JsonRPCAutoConfiguration implements EnvironmentAware, DisposableBean, BeanFactoryPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRPCAutoConfiguration.class);

    private Environment environment;
    private Boolean server;
    private Boolean client;
    private JsonRPCClient jsonRPCClient;
    private JsonRPCServer jsonRPCServer;

    /**
     * 修改器
     * 
     * @param environment 环境
     */
    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 自动扫描，注册服务组件
     *
     * @return 注册服务组件
     */
    public JsonRPCBeanPostProcessor jsonRPCBeanPostProcessor() {
        return new JsonRPCBeanPostProcessor(jsonRPCClient, jsonRPCServer);
    }

    /**
     * 释放资源
     *
     * @throws Exception 异常
     */
    @Override
    public void destroy() throws Exception {
        if (jsonRPCClient != null) {
            try {
                jsonRPCClient.shutdown();
            } catch (Throwable e) {
                LOGGER.error(e.getMessage());
            }
        }
        if (jsonRPCServer != null) {
            try {
                jsonRPCServer.shutdown();
            } catch (Throwable e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    /**
     * 服务端角色
     *
     * @param server 状态
     */
    public void setServer(Boolean server) {
        this.server = server;
    }

    /**
     * 客户端角色
     *
     * @param client 状态
     */
    public void setClient(Boolean client) {
        this.client = client;
    }

    /**
     * 后置处理组件工厂
     * 
     * @param beanFactory 容器上下文
     * @throws BeansException 组件异常
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        DiscoveryService discoveryService = null;
        if ((client || server) && environment.containsProperty(ConfigConsts.DISCOVERY_ZOOKEEPER)) {
            discoveryService = new ZKDiscoveryServiceImpl(environment.getProperty(ConfigConsts.DISCOVERY_ZOOKEEPER));
        }
        if (server) {
            try {
                Properties pros = new Properties();
                if (environment.containsProperty(ServerConsts.PORT)) {
                    pros.setProperty(ServerConsts.PORT, environment.getProperty(ServerConsts.PORT));
                }
                ServerConfig.init(pros);
                jsonRPCServer = beanFactory.createBean(JsonRPCServerImpl.class);
                jsonRPCServer.setDiscoveryService(discoveryService);
                jsonRPCServer.start();
            } catch (Throwable ex) {
                throw new BeansException(ex.getMessage()) {
                };
            }
        }
        if (client) {
            try {
                Properties pros = new Properties();
                if (environment.containsProperty(ClientConsts.SERVER_LIST)) {
                    pros.setProperty(ClientConsts.SERVER_LIST, environment.getProperty(ClientConsts.SERVER_LIST));
                }
                if (environment.containsProperty(ClientConsts.CONNECTION_TIMEOUT_MS)) {
                    pros.setProperty(ClientConsts.CONNECTION_TIMEOUT_MS, environment.getProperty(ClientConsts.CONNECTION_TIMEOUT_MS));
                }
                if (environment.containsProperty(ClientConsts.INVOKE_TIMEOUT_MS)) {
                    pros.setProperty(ClientConsts.INVOKE_TIMEOUT_MS, environment.getProperty(ClientConsts.INVOKE_TIMEOUT_MS));
                }
                ClientConfig.init(pros);
                jsonRPCClient = beanFactory.createBean(JsonRPCClientImpl.class);
                jsonRPCClient.setDiscoveryService(discoveryService);
                jsonRPCClient.start();
            } catch (Throwable ex) {
                throw new BeansException(ex.getMessage()) {
                };
            }
        }
        if (server || client) {
             beanFactory.addBeanPostProcessor(jsonRPCBeanPostProcessor());
        }
    }
}
