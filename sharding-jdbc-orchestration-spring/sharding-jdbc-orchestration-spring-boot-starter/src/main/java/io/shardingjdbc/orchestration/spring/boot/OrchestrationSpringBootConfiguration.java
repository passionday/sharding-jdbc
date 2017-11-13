/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
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
 * </p>
 */

package io.shardingjdbc.orchestration.spring.boot;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.shardingjdbc.core.constant.ShardingPropertiesConstant;
import io.shardingjdbc.core.exception.ShardingJdbcException;
import io.shardingjdbc.core.util.DataSourceUtil;
import io.shardingjdbc.orchestration.api.OrchestrationMasterSlaveDataSourceFactory;
import io.shardingjdbc.orchestration.api.OrchestrationShardingDataSourceFactory;
import io.shardingjdbc.orchestration.spring.boot.masterslave.SpringBootMasterSlaveRuleConfigurationProperties;
import io.shardingjdbc.orchestration.spring.boot.orchestration.OrchestrationSpringBootConfigurationProperties;
import io.shardingjdbc.orchestration.spring.boot.sharding.SpringBootShardingRuleConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Orchestration spring boot sharding and master-slave configuration.
 *
 * @author caohao
 */
@Configuration
@EnableConfigurationProperties({OrchestrationSpringBootConfigurationProperties.class, SpringBootShardingRuleConfigurationProperties.class, SpringBootMasterSlaveRuleConfigurationProperties.class})
public class OrchestrationSpringBootConfiguration implements EnvironmentAware {
    
    @Autowired
    private SpringBootShardingRuleConfigurationProperties shardingProperties;
    
    @Autowired
    private SpringBootMasterSlaveRuleConfigurationProperties masterSlaveProperties;
    
    @Autowired
    private OrchestrationSpringBootConfigurationProperties orchestrationProperties;
    
    private final Map<String, DataSource> dataSourceMap = new HashMap<>();
    
    private final Properties props = new Properties();
    
    @Bean
    public DataSource dataSource() throws SQLException {
        return null == masterSlaveProperties.getMasterDataSourceName() 
                ? OrchestrationShardingDataSourceFactory.createDataSource(dataSourceMap, 
                        shardingProperties.getShardingRuleConfiguration(), orchestrationProperties.getOrchestrationConfiguration(), shardingProperties.getData(), shardingProperties.getProps())
                : OrchestrationMasterSlaveDataSourceFactory.createDataSource(dataSourceMap, 
                        masterSlaveProperties.getMasterSlaveRuleConfiguration(), orchestrationProperties.getOrchestrationConfiguration(), masterSlaveProperties.getData());
    }
    
    @Override
    public void setEnvironment(final Environment environment) {
        setDataSourceMap(environment);
        setShardingProperties(environment);
    }
    
    private void setDataSourceMap(final Environment environment) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, "sharding.jdbc.datasource.");
        String dataSources = propertyResolver.getProperty("names");
        for (String each : dataSources.split(",")) {
            try {
                Map<String, Object> dataSourceProps = propertyResolver.getSubProperties(each + ".");
                Preconditions.checkState(!dataSourceProps.isEmpty(), "Wrong datasource properties!");
                DataSource dataSource = DataSourceUtil.getDataSource(dataSourceProps.get("type").toString(), dataSourceProps);
                dataSourceMap.put(each, dataSource);
            } catch (final ReflectiveOperationException ex) {
                throw new ShardingJdbcException("Can't find datasource type!", ex);
            }
        }
    }
    
    private void setShardingProperties(final Environment environment) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, "sharding.jdbc.config.sharding.props.");
        String showSQL = propertyResolver.getProperty(ShardingPropertiesConstant.SQL_SHOW.getKey());
        if (!Strings.isNullOrEmpty(showSQL)) {
            props.setProperty(ShardingPropertiesConstant.SQL_SHOW.getKey(), showSQL);
        }
        String executorSize = propertyResolver.getProperty(ShardingPropertiesConstant.EXECUTOR_SIZE.getKey());
        if (!Strings.isNullOrEmpty(executorSize)) {
            props.setProperty(ShardingPropertiesConstant.EXECUTOR_SIZE.getKey(), executorSize);
        }
    }
}