package br.com.alura.ecommerce;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class NewOrderServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (var orderDispatcher = new KafkaDispatcher<Order>()) {
            try (var emailDispatcher = new KafkaDispatcher<String>()) {

                try {
                    var userId = UUID.randomUUID().toString();
                    var orderId = UUID.randomUUID().toString();
                    var amount = new BigDecimal(Math.random() * 5000 + 1).setScale(2, RoundingMode.HALF_UP);
                    var email = Math.random() + "@email.com";

                    var order = new Order(userId, orderId, amount, email);
                    orderDispatcher.send("ECOMMERCE_NEW_ORDER", userId, order);

                    var emailCode = "Thank you for your order! We are processing your order for WEB!";
                    emailDispatcher.send("ECOMMERCE_SEND_EMAIL", userId, emailCode);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().println("Order processed successfully");

                } catch (ExecutionException e) {
                    throw new ServletException(e);
                } catch (InterruptedException e) {
                    throw new ServletException(e);
                }

            }
        }
    }
}
