package cz.ladicek.vertx.tiny.web;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class MainVerticleTest {
    private WebClient client;

    @BeforeEach
    void deploy(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MainVerticle()).onComplete(testContext.succeedingThenComplete());
        client = WebClient.create(vertx);
    }

    @AfterEach
    void close() {
        client.close();
    }

    @Test
    void simpleHello(VertxTestContext testContext) {
        client.get(8080, "localhost", "/hello")
                .as(BodyCodec.string())
                .send()
                .onComplete(testContext.succeeding(response -> {
                    assertEquals("Hello world", response.body());
                    testContext.completeNow();
                }));
    }

    @Test
    void helloWithParam(VertxTestContext testContext) {
        client.get(8080, "localhost", "/hello?name=Ladicek")
                .as(BodyCodec.string())
                .send()
                .onComplete(testContext.succeeding(response -> {
                    assertEquals("Hello Ladicek", response.body());
                    testContext.completeNow();
                }));
    }

    @Test
    void client(VertxTestContext testContext) {
        client.get(8080, "localhost", "/client")
                .as(BodyCodec.string())
                .send()
                .onComplete(testContext.succeeding(response -> {
                    assertEquals("Got: Hello world", response.body());
                    testContext.completeNow();
                }));
    }
}
