package vertx;

import io.vertx.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() {
        vertx.createHttpServer().requestHandler(req -> req.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end("{\"book\":\"茶馆\"}")).listen(80);
    }
}
