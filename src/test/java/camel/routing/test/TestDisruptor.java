package camel.routing.test;

import static java.util.concurrent.TimeUnit.*;

import javax.annotation.PostConstruct;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { TestDisruptor.NestedConfiguration.class })
public class TestDisruptor {

	private static final Logger log = LoggerFactory
			.getLogger(TestDisruptor.class);

	public static final String IN_QUEUE = "disruptor:in?multipleConsumers=true";

	public static final String OUT_QUEUE = "disruptor:out?multipleConsumers=true";

	static public class BarEventConsumer {

		private static final Logger log = LoggerFactory
				.getLogger(BarEventConsumer.class);

		@Autowired
		private CamelContext context;

		@Autowired
		private ApplicationContext applicationContext;

		public BarEventConsumer() {
		}

		@PostConstruct
		void postConstruct() {
			new Thread(this::receiveMessage).start();
			try {
				SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		void receiveMessage() {
			try {
				context.start();
				ConsumerTemplate consumer = context.createConsumerTemplate();
				consumer.start();
				log.error("Consuming messages from {}.",
						TestDisruptor.OUT_QUEUE);
				Exchange aisExchange = consumer
						.receive(TestDisruptor.OUT_QUEUE);
				// log.error("Received {}", aisExchange.getIn().getBody());
				ProducerTemplate destination = context.createProducerTemplate();
				destination.sendBody("mock:result",
						"bar" + aisExchange.getIn().getBody());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Autowired
	private CamelContext camelContext;

	@Configuration
	@ImportResource({ "classpath:/disruptor-test.xml" })
	static class NestedConfiguration {
		@Bean
		@DependsOn("camel")
		public BarEventConsumer barEventConsumer() {
			return new BarEventConsumer();
		}
	}

	@Test
	public void testDisruptor() throws Exception {
		MockEndpoint resultEndpoint = camelContext.getEndpoint("mock:result",
				MockEndpoint.class);
		resultEndpoint.expectedMessageCount(1);
		String dataToSend = "HELLO WORLD";
		ProducerTemplate producer = camelContext.createProducerTemplate();
		producer.sendBody(IN_QUEUE, dataToSend);
		log.error("Sent {} on {} status={}", dataToSend, IN_QUEUE,
				camelContext.getStatus());
		SECONDS.sleep(1);
		resultEndpoint.assertIsSatisfied();
	}

}
