package br.com.siecola.productsservice.products.exceptions;

import br.com.siecola.productsservice.products.enums.ProductErrors;
import org.springframework.lang.Nullable;

public class ProductException extends Exception {
    private final ProductErrors productErrors;
    @Nullable
    private final String productId;

    public ProductException(ProductErrors productErrors, @Nullable String productId) {
        this.productErrors = productErrors;
        this.productId = productId;
    }

    public ProductErrors getProductErrors() {
        return productErrors;
    }

    @Nullable
    public String getProductId() {
        return productId;
    }
}
