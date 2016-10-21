package br.com.zup.kafka.consumer.typetests;

import br.com.zup.kafka.config.props.ConsumerProperties;
import br.com.zup.kafka.config.props.OffsetReset;
import br.com.zup.kafka.config.props.ProducerProperties;
import br.com.zup.kafka.config.props.PropertyBuilder;
import br.com.zup.kafka.consumer.ConsumerRunner;
import br.com.zup.kafka.consumer.GenericConsumerHandler;
import br.com.zup.kafka.consumer.TestConfigs;
import br.com.zup.kafka.consumer.utils.SampleTO;
import br.com.zup.kafka.producer.KafkaProducer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ZupKafkaInvalidObjectTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZupKafkaInvalidObjectTest.class);
    private static KafkaProducer<String, Map> producerInvalidObject;
    private static KafkaProducer<String, String> producerInvalidString;
    private static KafkaProducer<String, SampleTO> producer;

    private static final String TOPIC = "zup_kafka_object_topic";

    private static ExecutorService consumerExecutorService;

    private static GenericConsumerHandler<SampleTO> objectConsumerHandler = new GenericConsumerHandler<>();

    @BeforeClass
    public static void beforeClass() {
        ProducerProperties props = PropertyBuilder.producer(TestConfigs.KAFKA_BOOTSTRAP_SERVERS);
        producer = new KafkaProducer<>(props);
        producerInvalidObject = new KafkaProducer<>(props);
        producerInvalidString = new KafkaProducer<>(props);

        ConsumerProperties<String, SampleTO> consumerProperties = PropertyBuilder
                .consumer(objectConsumerHandler)
                .withTopics(Collections.singletonList(TOPIC))
                .withServers(TestConfigs.KAFKA_BOOTSTRAP_SERVERS)
                .withGroupId(TestConfigs.KAFKA_DEFAULT_GROUP_ID)
                .withDeserializerClass(SampleTO.class)
                .withAutoOffsetReset(OffsetReset.EARLIEST);

        consumerExecutorService = ConsumerRunner.execute(TestConfigs.KAFKA_DEFAULT_CONSUMER_POOL_SIZE, consumerProperties);
    }

    @AfterClass
    public static void afterClass() throws InterruptedException {
        producer.close();
        consumerExecutorService.shutdown();
        consumerExecutorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void should_ignore_invalid_object() throws ExecutionException, InterruptedException {
        objectConsumerHandler.setCountDown(1);
        Map map = new HashMap();
        map.put("oi", "maluco");
        producerInvalidObject.send(TOPIC, map);
        producer.send(TOPIC, new SampleTO("objectTestMsg", LocalDate.now())).get();
        Assert.assertEquals(objectConsumerHandler.await(), true);
    }

    @Test
    public void should_ignore_invalid_text() throws ExecutionException, InterruptedException {
        objectConsumerHandler.setCountDown(1);
        producerInvalidString.send(TOPIC, "hello");
        producer.send(TOPIC, new SampleTO("objectTestMsg", LocalDate.now())).get();
        Assert.assertEquals(objectConsumerHandler.await(), true);
    }

}

