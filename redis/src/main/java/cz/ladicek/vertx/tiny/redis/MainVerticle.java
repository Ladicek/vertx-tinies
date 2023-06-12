package cz.ladicek.vertx.tiny.redis;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxInfluxDbOptions;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions;

import java.util.List;

public class MainVerticle extends AbstractVerticle {
    // https://www.jaegertracing.io/docs/1.46/getting-started/
    // docker run -it --rm --name jaeger -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 -e COLLECTOR_OTLP_ENABLED=true -p 6831:6831/udp -p 6832:6832/udp -p 5778:5778 -p 16686:16686 -p 4317:4317 -p 4318:4318 -p 14250:14250 -p 14268:14268 -p 14269:14269 -p 9411:9411 jaegertracing/all-in-one:1.46
    // open http://localhost:16686/

    // https://docs.influxdata.com/influxdb/v2.7/install/?t=Docker
    // docker run -it --rm --name influxdb -e DOCKER_INFLUXDB_INIT_MODE=setup -e DOCKER_INFLUXDB_INIT_USERNAME=influxdb -e DOCKER_INFLUXDB_INIT_PASSWORD=influxdb -e DOCKER_INFLUXDB_INIT_ORG=influxdb -e DOCKER_INFLUXDB_INIT_BUCKET=default -e DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=my-super-secret-token -p 8086:8086 influxdb:2.7 --reporting-disabled
    // open http://localhost:8086/

    // https://redis.io/docs/stack/get-started/install/docker/
    // docker run -it --rm --name redis -p 6379:6379 -p 8001:8001 redis/redis-stack:latest
    // open http://localhost:8001/

    public static void main(String[] args) {
        Resource resource = Resource.getDefault().merge(Resource.builder().put("service.name", "my-service").build());
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.getDefault()).build())
                .setResource(resource)
                .build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

        VertxOptions options = new VertxOptions()
                .setMetricsOptions(new MicrometerMetricsOptions()
                        .setEnabled(true)
                        .setInfluxDbOptions(new VertxInfluxDbOptions()
                                .setEnabled(true)
                                .setToken("my-super-secret-token")
                                .setOrg("influxdb")
                                .setBucket("default")))
                .setTracingOptions(new OpenTelemetryOptions(openTelemetry));
        Vertx vertx = Vertx.vertx(options);
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        RedisAPI client = RedisAPI.api(Redis.createClient(vertx));

        Router router = Router.router(vertx);

        router.route(HttpMethod.GET, "/:key").handler(ctx -> {
            String key = ctx.pathParam("key");
            client.get(key)
                    .onSuccess(response -> ctx.end(response.toString()))
                    .onFailure(ctx::fail);
        });

        router.route(HttpMethod.POST, "/:key").handler(BodyHandler.create()).handler(ctx -> {
            String key = ctx.pathParam("key");
            String value = ctx.body().asString();
            client.set(List.of(key, value))
                    .onSuccess(response -> ctx.end())
                    .onFailure(ctx::fail);
        });

        router.route(HttpMethod.DELETE, "/:key").handler(ctx -> {
            String key = ctx.pathParam("key");
            client.del(List.of(key))
                    .onSuccess(response -> ctx.end())
                    .onFailure(ctx::fail);
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
