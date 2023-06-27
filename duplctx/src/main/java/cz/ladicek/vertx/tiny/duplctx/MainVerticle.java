package cz.ladicek.vertx.tiny.duplctx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.ext.web.Router;

public class MainVerticle extends AbstractVerticle {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);

        router.get("/").handler(ctx -> {
            System.out.println("!!! 1 " + Thread.currentThread().getName() + " -> " + describeCurrentContext());
            vertx.executeBlocking(() -> {
                //delay(100);
                System.out.println("!!! 2 " + Thread.currentThread().getName() + " -> " + describeCurrentContext());
                return null;
            }).onComplete(result -> {
                System.out.println("!!! 3 " + Thread.currentThread().getName() + " -> " + describeCurrentContext());
                ctx.end("Hello");
            });
            //delay(100);
            System.out.println("!!! 4 " + Thread.currentThread().getName() + " -> " + describeCurrentContext());
        });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080)
                .onSuccess(server -> {
                    System.out.println("HTTP server started on port " + server.actualPort());
                    startPromise.complete();
                });
    }

    private static void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String describeCurrentContext() {
        ContextInternal ctx = ContextInternal.current();
        return str(ctx) + " | " + str(ctx.localContextData());
    }

    private static String str(Object obj) {
        if (obj == null) {
            return "<null>";
        }
        return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
    }
}
