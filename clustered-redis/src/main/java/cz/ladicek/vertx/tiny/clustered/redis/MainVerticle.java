package cz.ladicek.vertx.tiny.clustered.redis;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;

import java.util.List;

public class MainVerticle extends AbstractVerticle {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Redis redis = Redis.createClient(vertx, new RedisOptions()
                .setType(RedisClientType.CLUSTER)
                .setConnectionString("redis://192.168.1.117"));
        RedisAPI client = RedisAPI.api(redis);

        call(client, "foo", 0);
        call(client, "bar", 0);
        call(client, "baz", 0);
        call(client, "qux", 0);
        call(client, "quux", 0);
        call(client, "corge", 0);
        call(client, "grault", 0);
        call(client, "garply", 0);
        call(client, "waldo", 0);
        call(client, "fred", 0);
        call(client, "plugh", 0);
        call(client, "xyzzy", 0);
        call(client, "thud", 0);
    }

    private void call(RedisAPI client, String prefix, int last) {
        if (last == 1_000_000) {
            return;
        }
        int x = last + 1;
        client.set(List.of(prefix + x, "" + x))
                .flatMap(response -> {
                    return client.get(prefix + x);
                }).flatMap(response -> {
                    System.out.println(prefix + " -> " + response);
                    return client.set(List.of("__last__" + prefix, "" + x));
                }).onSuccess(response -> {
                    vertx.runOnContext(ignored -> call(client, prefix, x));
                }).onFailure(error -> {
                    System.out.println(error);
                });
    }
}
