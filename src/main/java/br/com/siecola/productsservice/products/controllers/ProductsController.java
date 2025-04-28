package br.com.siecola.productsservice.products.controllers;

import br.com.siecola.productsservice.products.dto.ProductDto;
import br.com.siecola.productsservice.products.enums.ProductErrors;
import br.com.siecola.productsservice.products.exceptions.ProductException;
import br.com.siecola.productsservice.products.models.Product;
import br.com.siecola.productsservice.products.repositories.ProductsRepository;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@RestController
@RequestMapping("/api/products")
@XRayEnabled
public class ProductsController {
    private static final Logger LOG = LogManager.getLogger(ProductsController.class);
    private final ProductsRepository productsRepository;

    @Autowired
    public ProductsController(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(required = false) String code)
            throws ProductException {
        if (code != null) {
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
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {
        Product productCreated = ProductDto.toProduct(productDto);
        productCreated.setId(UUID.randomUUID().toString());
        productsRepository.create(productCreated).join();

        LOG.info("Product created - ID: {}", productCreated.getId());
        return new ResponseEntity<>(new ProductDto(productCreated), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<ProductDto> deleteProductById(@PathVariable("id") String id) throws ProductException {
        Product productDeleted = productsRepository.deleteById(id).join();
        if (productDeleted != null) {
            LOG.info("Product deleted - ID: {}", productDeleted.getId());
            return new ResponseEntity<>(new ProductDto(productDeleted), HttpStatus.OK);
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductDto> updateProduct(@RequestBody ProductDto productDto,
                                                    @PathVariable("id") String id) throws ProductException {
        try {
            Product productUpdated = productsRepository
                    .update(ProductDto.toProduct(productDto), id).join();
            LOG.info("Product updated - ID: {}", productUpdated.getId());
            return new ResponseEntity<>(new ProductDto(productUpdated), HttpStatus.OK);
        } catch (CompletionException e) {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }
}
