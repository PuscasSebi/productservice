package br.com.siecola.productsservice.events.services;

import br.com.siecola.productsservice.events.dto.EventType;
import br.com.siecola.productsservice.events.dto.ProductEventDto;
import br.com.siecola.productsservice.events.dto.ProductFailureEventDto;
import br.com.siecola.productsservice.products.models.Product;
import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class EventsPublisher {

    private final SnsAsyncClient snsAsyncClient;
    private final Topic productEventsTopic;
    private final ObjectMapper objectMapper;

    @Autowired
    public EventsPublisher(SnsAsyncClient snsAsyncClient,
            @Qualifier("productEventsTopic") Topic productEventsTopic,
            ObjectMapper objectMapper
    ) {

        this.snsAsyncClient = snsAsyncClient;
        this.productEventsTopic = productEventsTopic;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<PublishResponse> sendProductFailureEvent(ProductFailureEventDto productFailureEventDto)
            throws JsonProcessingException {
        return this.sendEvent(objectMapper.writeValueAsString(productFailureEventDto), EventType.PRODUCT_FAILURE);
    }

    public CompletableFuture<PublishResponse> sendProductEvent(Product product, EventType eventType, String email)
            throws JsonProcessingException {
        ProductEventDto productEventDto = new ProductEventDto(
                product.getId(),
                product.getCode(),
                email,
                product.getPrice()
        );
        return this.sendEvent(objectMapper.writeValueAsString(productEventDto), eventType);
    }

    private CompletableFuture<PublishResponse> sendEvent(String data, EventType eventType) {
        String requestId = ThreadContext.get("requestId");
        if (requestId == null){
            requestId = UUID.randomUUID().toString();
        }
        return this.snsAsyncClient.publish(PublishRequest.builder()
                        .message(data)
                        .messageAttributes(Map.of(
                                "eventType", MessageAttributeValue.builder()
                                                .dataType("String")
                                                .stringValue(eventType.name())
                                        .build(),
                                "requestId", MessageAttributeValue.builder()
                                                .dataType("String")
                                                .stringValue(requestId)
                                        .build(),
                                "traceId", MessageAttributeValue.builder()
                                                .dataType("String")
                                                .stringValue(Objects.requireNonNull(
                                                        AWSXRay.getCurrentSegment()).getTraceId().toString()
                                                )
                                        .build()
                        ))
                        .topicArn(this.productEventsTopic.topicArn())
                .build());
    }
}
