package br.com.siecola.productsservice.products.controllers;

import br.com.siecola.productsservice.events.dto.EventType;
import br.com.siecola.productsservice.events.services.EventsPublisher;
import br.com.siecola.productsservice.products.dto.ProductDto;
import br.com.siecola.productsservice.products.enums.ProductErrors;
import br.com.siecola.productsservice.products.exceptions.ProductException;
import br.com.siecola.productsservice.products.models.Product;
import br.com.siecola.productsservice.products.repositories.ProductsRepository;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/products")
@XRayEnabled
public class ProductsController {
    private static final Logger LOG = LogManager.getLogger(ProductsController.class);
    private final ProductsRepository productsRepository;

    private final EventsPublisher eventsPublisher;

    @Autowired
    public ProductsController(ProductsRepository productsRepository, EventsPublisher eventsPublisher) {
        this.productsRepository = productsRepository;
        this.eventsPublisher = eventsPublisher;
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(required = false, name = "code") String code)
            throws ProductException {

        if (code != null) {
            code = code.strip().trim();
            LOG.info("Get product by cod: {}", code);
            Product productByCode = productsRepository.getByCode(code).join();
            if (productByCode != null) {
                return new ResponseEntity<>(new ProductDto(productByCode), HttpStatus.OK);
            } else {
                throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, null);
            }
        } else {
            LOG.info("Get all products");
            List<ProductDto> productsDto = new ArrayList<>();

            productsRepository.getAll().items().subscribe(product -> {
                productsDto.add(new ProductDto(product));
            }).join();

            return new ResponseEntity<>(productsDto, HttpStatus.OK);
        }
    }
    @GetMapping("v2/{pathParam}")
    public ResponseEntity<?> getProduct(@PathVariable("pathParam") String code)
            throws ProductException {

        if (code != null) {
            code = code.strip().trim();
            LOG.info("Get product by cod: {}", code);
            Product productByCode = productsRepository.getByCode(code).join();
            if (productByCode != null) {
                return new ResponseEntity<>(new ProductDto(productByCode), HttpStatus.OK);
            } else {
                throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, null);
            }
        } else {
            LOG.info("Get all products");
            List<ProductDto> productsDto = new ArrayList<>();

            productsRepository.getAll().items().subscribe(product -> {
                productsDto.add(new ProductDto(product));
            }).join();

            return new ResponseEntity<>(productsDto, HttpStatus.OK);
        }
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable("id") String id) throws ProductException {
        Product product = productsRepository.getById(id).join();
        if (product != null) {
            LOG.info("Get product by its id: {}", id);
            return new ResponseEntity<>(new ProductDto(product), HttpStatus.OK);
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) throws ProductException,
            JsonProcessingException, ExecutionException, InterruptedException {
        Product productCreated = ProductDto.toProduct(productDto);
        productCreated.setId(UUID.randomUUID().toString());
        CompletableFuture<Void> voidCompletableFuture = productsRepository.create(productCreated);

        CompletableFuture<PublishResponse> publishResponseCompletableFuture = eventsPublisher.sendProductEvent(productCreated, EventType.PRODUCT_CREATED, "userinfo@gm.com");

        CompletableFuture.allOf(voidCompletableFuture, publishResponseCompletableFuture).join();

        ThreadContext.put("messageId", publishResponseCompletableFuture.get().messageId());

        LOG.info("Product created - ID: {}", productCreated.getId());
        return new ResponseEntity<>(new ProductDto(productCreated), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<ProductDto> deleteProductById(@PathVariable("id") String id) throws ProductException, JsonProcessingException {
        Product productDeleted = productsRepository.deleteById(id).join();
        if (productDeleted != null) {
            PublishResponse join = eventsPublisher.sendProductEvent(productDeleted, EventType.PRODUCT_DELETED, "userinfo@gm.com").join();
            ThreadContext.put("messageId", join.messageId());
            LOG.info("Product deleted - ID: {}", productDeleted.getId());
            return new ResponseEntity<>(new ProductDto(productDeleted), HttpStatus.OK);
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductDto> updateProduct(@RequestBody ProductDto productDto,
                                                    @PathVariable("id") String id) throws ProductException, JsonProcessingException {
        try {
            Product productUpdated = productsRepository
                    .update(ProductDto.toProduct(productDto), id).join();
            PublishResponse join = eventsPublisher.sendProductEvent(productUpdated, EventType.PRODUCT_UPDATED, "userinfo@gm.com").join();
            ThreadContext.put("messageId", join.messageId());

            LOG.info("Product updated - ID: {}", productUpdated.getId());
            return new ResponseEntity<>(new ProductDto(productUpdated), HttpStatus.OK);
        } catch (CompletionException e) {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }
}
