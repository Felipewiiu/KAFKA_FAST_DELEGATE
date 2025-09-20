package br.com.alura.ecommerce;

import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ToString
@Getter
public class Order {

    private final String userId;
    private final String orderId;
    private final String email;
    private final BigDecimal amount;

    public Order(String userId, String orderId, String email, BigDecimal amount) {
        this.userId = userId;
        this.orderId = orderId;
        this.email = email;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }


}
