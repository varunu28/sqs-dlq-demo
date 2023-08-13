import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SampleDlqDelayTest {

    private static final String QUEUE_URL = "http://localhost:44566/000000000000/sample-queue";

    private static final String DLQ_URL = "http://localhost:44566/000000000000/sample-queue-dlq";

    private static final String SAMPLE_MESSAGE = "sample_message";

    public static LocalStackContainer localstack = new LocalStackContainer()
            .withServices(LocalStackContainer.Service.SQS);

    private SqsAsyncClient sqsAsyncClient;

    @BeforeEach
    public void setUp() {
        localstack.start();
        sqsAsyncClient = SqsAsyncClient.builder()
                .endpointOverride(URI.create("http://localhost:44566"))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return "FAKE";
                    }

                    @Override
                    public String secretAccessKey() {
                        return "FAKE";
                    }
                }))
                .build();
    }

    @Test
    public void testSqsMessageDelay() {
        // Publish 1 message to SQS
        Mono<SendMessageResponse> sendMessageResponseMono = publishMessageToSQS();
        StepVerifier.create(sendMessageResponseMono)
                .expectNextCount(1)
                .verifyComplete();
        // Consume the message from SQS queue three times without acknowledgment
        for (int i = 0; i < 3; i++) {
            StepVerifier.create(consumeAndProcessMessage())
                    .thenAwait(Duration.ofSeconds(4))
                    .consumeNextWith(e -> assertEquals("sample_message", e))
                    .verifyComplete();
        }
        // NOTE: Need to query queue one more time to push the message to DLQ
        StepVerifier.create(consumeMessage(QUEUE_URL))
                    .thenAwait(Duration.ofSeconds(4))
                    .consumeNextWith(e -> assertEquals(0, e.messages().size()))
                    .verifyComplete();
        // Verify that the message is pushed to SQS DLQ
        StepVerifier.create(consumeMessage(DLQ_URL))
                .thenAwait(Duration.ofSeconds(4))
                .consumeNextWith(e -> assertEquals(1, e.messages().size()))
                .verifyComplete();
    }

    private Flux<String> consumeAndProcessMessage() {
        return consumeMessage(SampleDlqDelayTest.QUEUE_URL)
                .map(ReceiveMessageResponse::messages)
                .flatMapMany(Flux::fromIterable)
                .delayElements(Duration.ofSeconds(2))
                .flatMap(message -> Mono.just(message.body()))
                .doOnEach(i -> System.out.println("Received messages"))
                .delayElements(Duration.ofSeconds(1));
    }

    private Mono<ReceiveMessageResponse> consumeMessage(String queueUrl) {
        return Mono.fromFuture(() -> sqsAsyncClient.receiveMessage(
                ReceiveMessageRequest.builder().queueUrl(queueUrl).build()))
                .publishOn(Schedulers.boundedElastic());
    }

    private Mono<SendMessageResponse> publishMessageToSQS() {
        return Mono.fromFuture(() -> sqsAsyncClient.sendMessage(
                SendMessageRequest.builder()
                        .messageBody(SAMPLE_MESSAGE)
                        .queueUrl(QUEUE_URL)
                        .build()))
                .publishOn(Schedulers.boundedElastic());
    }
}
