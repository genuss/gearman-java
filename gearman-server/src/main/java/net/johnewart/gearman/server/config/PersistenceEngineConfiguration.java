package net.johnewart.gearman.server.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import net.johnewart.gearman.engine.exceptions.JobQueueFactoryException;
import net.johnewart.gearman.engine.healthchecks.RedisHealthCheck;
import net.johnewart.gearman.engine.queue.factories.DynamoDBPersistedJobQueueFactory;
import net.johnewart.gearman.engine.queue.factories.JobQueueFactory;
import net.johnewart.gearman.engine.queue.factories.MemoryJobQueueFactory;
import net.johnewart.gearman.engine.queue.factories.PostgreSQLPersistedJobQueueFactory;
import net.johnewart.gearman.engine.queue.factories.RedisPersistedJobQueueFactory;
import net.johnewart.gearman.server.config.persistence.DynamoDBConfiguration;
import net.johnewart.gearman.server.config.persistence.PostgreSQLConfiguration;
import net.johnewart.gearman.server.config.persistence.RedisConfiguration;
import redis.clients.jedis.Jedis;

public class PersistenceEngineConfiguration {

    private static final String ENGINE_MEMORY = "memory";
    private static final String ENGINE_REDIS = "redis";
    private static final String ENGINE_POSTGRES = "postgres";
    private static final String ENGINE_DYNAMODB = "dynamodb";

    private RedisConfiguration redis;
    private PostgreSQLConfiguration postgreSQL;
    private DynamoDBConfiguration dynamoDB;

    private String engine;
    private JobQueueFactory jobQueueFactory;

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public RedisConfiguration getRedis() {
        return redis;
    }

    public void setRedis(RedisConfiguration redis) {
        this.redis = redis;
    }

    public PostgreSQLConfiguration getPostgreSQL() {
        return postgreSQL;
    }

    public void setPostgreSQL(PostgreSQLConfiguration postgreSQL) {
        this.postgreSQL = postgreSQL;
    }

    public DynamoDBConfiguration getDynamodB() { return dynamoDB; }

    public void setDynamoDB(DynamoDBConfiguration dynamoDB) { this.dynamoDB = dynamoDB; }

    public JobQueueFactory getJobQueueFactory(MetricRegistry metricRegistry) {
        if(jobQueueFactory == null) {
            switch (getEngine()) {
                case ENGINE_MEMORY:
                    jobQueueFactory = new MemoryJobQueueFactory(metricRegistry);
                    break;
                case ENGINE_DYNAMODB:
                    try {
                        jobQueueFactory = new DynamoDBPersistedJobQueueFactory(
                                dynamoDB.getEndpoint(),
                                dynamoDB.getAccessKey(),
                                dynamoDB.getSecretKey(),
                                dynamoDB.getTable(),
                                dynamoDB.getReadUnits(),
                                dynamoDB.getWriteUnits(),
                                metricRegistry
                        );
                    } catch (JobQueueFactoryException e) {
                        jobQueueFactory = null;
                    }
                    break;
                case ENGINE_POSTGRES:
                    try {
                        jobQueueFactory = new PostgreSQLPersistedJobQueueFactory(
                                postgreSQL.getHost(),
                                postgreSQL.getPort(),
                                postgreSQL.getDbName(),
                                postgreSQL.getUser(),
                                postgreSQL.getPassword(),
                                postgreSQL.getTable(),
                                metricRegistry
                        );
                    } catch (JobQueueFactoryException e) {
                        jobQueueFactory = null;
                    }
                    break;
                case ENGINE_REDIS:
                    jobQueueFactory = new RedisPersistedJobQueueFactory(
                            redis.getHost(),
                            redis.getPort(),
                            metricRegistry
                    );
                    break;
                default:
                    jobQueueFactory = null;
            }
        }

        if(jobQueueFactory == null) {
            throw new RuntimeException("No job queue factory was constructed, giving up!");
        }

        return jobQueueFactory;
    }

    public HealthCheck getHealthCheck()
    {
        switch(getEngine()) {
            case ENGINE_REDIS:
                return new RedisHealthCheck(new Jedis(redis.getHost(), redis.getPort()));
            default:
                return null;
        }
    }
}
