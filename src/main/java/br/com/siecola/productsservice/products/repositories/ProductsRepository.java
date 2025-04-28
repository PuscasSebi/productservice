package br.com.siecola.productsservice.products.repositories;

import br.com.siecola.productsservice.products.controllers.ProductsController;
import br.com.siecola.productsservice.products.enums.ProductErrors;
import br.com.siecola.productsservice.products.exceptions.ProductException;
import br.com.siecola.productsservice.products.models.Product;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Repository
@XRayEnabled
public class ProductsRepository {

    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    private static final Logger LOG = LogManager.getLogger(ProductsRepository.class);


    private DynamoDbAsyncTable<Product> productsTable;
    @Autowired
    public ProductsRepository(
            DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
        @Value("${aws.productsdb.name}") String productsDdbName) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.productsTable = dynamoDbEnhancedAsyncClient
                .table(productsDdbName, TableSchema.fromBean(Product.class));
    }

    public PagePublisher<Product> getAll() {
        //DO NOT DO THIS PRODUCTION
        return productsTable.scan();
    }

    public CompletableFuture<Product> getById(String productId) {
        LOG.info("productId: {}", productId);
        return productsTable.getItem(Key.builder()
                        .partitionValue(productId)
                .build());
    }

    private CompletableFuture<Product> checkIfCodeExists(String code) {
        List<Product> products = new ArrayList<>();
        productsTable.index("codeIdx").query(QueryEnhancedRequest.builder()
                        .limit(1)
                        .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                        .partitionValue(code)
                                .build()))
                .build()).subscribe(pg -> products.addAll(pg.items())).join();
        LOG.info("by code {} found products: {}", code, products);
        if (products.size() > 0) {
            return CompletableFuture.completedFuture(products.get(0));
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public CompletableFuture<Product> getByCode(String productCode)  {
        Product product = checkIfCodeExists(productCode).join();

        if (product != null){
            return getById(product.getId());
        } else {
            return null;
        }
    }


    public CompletableFuture<Void> create(Product product) throws ProductException {
        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null) {
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, productWithSameCode.getId());
        }
        return productsTable.putItem(product);
    }

    public CompletableFuture<Product> deleteById(String productId) {
        return productsTable.deleteItem(Key.builder()
                .partitionValue(productId)
                .build());
    }

    public CompletableFuture<Product> update(Product product, String productId) throws ProductException {
        product.setId(productId);
        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null && !productWithSameCode.getId().equals(product.getId())) {
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, productWithSameCode.getId());
        }
        return productsTable.updateItem(
                UpdateItemEnhancedRequest.builder(Product.class)
                        .item(product)
                        .conditionExpression(Expression.builder()
                                .expression("attribute_exists(id)")
                                .build())
                        .build()
        );
    }


}
