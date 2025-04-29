package br.com.siecola.productsservice.products.exceptions;

import br.com.siecola.productsservice.events.dto.ProductFailureEventDto;
import br.com.siecola.productsservice.events.services.EventsPublisher;
import br.com.siecola.productsservice.products.dto.ProductErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@RestControllerAdvice
public class ProductsExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LogManager.getLogger(ProductsExceptionHandler.class);
    private final EventsPublisher eventsPublisher;

    @Autowired
    public ProductsExceptionHandler(EventsPublisher eventsPublisher) {
        this.eventsPublisher = eventsPublisher;
    }

    @ExceptionHandler(value = { ProductException.class })
    protected ResponseEntity<Object> handelProductError(ProductException productException, WebRequest webRequest)
            throws JsonProcessingException {
        ProductErrorResponse productErrorResponse = new ProductErrorResponse(
                productException.getProductErrors().getMessage(),
                productException.getProductErrors().getHttpStatus().value(),
                ThreadContext.get("requestId"),
                productException.getProductId()
        );

        ProductFailureEventDto productFailureEventDto = new ProductFailureEventDto(
                "matilde@siecola.com.br",
                productException.getProductErrors().getHttpStatus().value(),
                productException.getProductErrors().getMessage(),
                productException.getProductId()
        );

        PublishResponse publishResponse = eventsPublisher.sendProductFailureEvent(productFailureEventDto).join();
        ThreadContext.put("messageId", publishResponse.messageId());

        LOG.error(productException.getProductErrors().getMessage());

        return handleExceptionInternal(
                productException,
                productErrorResponse,
                new HttpHeaders(),
                productException.getProductErrors().getHttpStatus(),
                webRequest
        );
    }
}
