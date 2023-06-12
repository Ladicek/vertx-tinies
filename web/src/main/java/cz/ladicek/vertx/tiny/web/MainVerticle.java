package cz.ladicek.vertx.tiny.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class MainVerticle extends AbstractVerticle {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        WebClient client = WebClient.create(vertx);

        router.get("/client").handler(ctx -> {
            client.get(8080, "localhost", "/hello")
                    .as(BodyCodec.string())
                    .send()
                    .onSuccess(response -> ctx.end("Got: " + response.body()))
                    .onFailure(ctx::fail);
        });

        router.get("/hello").handler(ctx -> {
            String name = ctx.queryParams().get("name");
            ctx.end("Hello " + (name != null ? name : "world"));
        });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080)
                .onSuccess(server -> {
                    System.out.println("HTTP server started on port " + server.actualPort());
                    startPromise.complete();
                });
    }
}
