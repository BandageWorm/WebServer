package vertx;

import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class RxVerticle extends AbstractVerticle {

    @Override
    public void start() {
        Router router = Router.router(vertx);
        HttpServer server = vertx.createHttpServer();

        router.route("/*").handler(StaticHandler.create("assets").setDefaultContentEncoding("utf-8"));
        router.route("/hello").handler(handler ->
                handler.response().putHeader("content-type", "text/html; charset=utf-8")
                        .end("<html><body><h1>Hello from vert.x!</h1></body></html>")
        );

        server.requestStream().toFlowable()
                .map(HttpServerRequest::pause)
                .onBackpressureDrop(req -> req.response().setStatusCode(503).end())
                .observeOn(RxHelper.scheduler(vertx.getDelegate()))
                .subscribe(req -> {
                    req.resume();
                    router.accept(req);
                });
        server.listen(80);
    }

    @Override
    public void stop() throws Exception {
    }
}
