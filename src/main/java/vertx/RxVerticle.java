package vertx;

import org.apache.commons.codec.digest.DigestUtils;

import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import mcs.Engine;
import wechat.Util;
import wechat.XmlProcess;

import java.util.Arrays;

public class RxVerticle extends AbstractVerticle {

    @Override
    public void start() {
        Engine.get();
        Router router = Router.router(vertx);
        HttpServer server = vertx.createHttpServer();

        router.route("/*").handler(StaticHandler.create("assets").setDefaultContentEncoding("utf-8"));
        router.route("/hello").handler(handler ->
                handler.response().putHeader("content-type", "text/html; charset=utf-8")
                        .end("<html><body><h1>Hello from vert.x!</h1></body></html>"));

        router.post("/chatbot").handler(handler -> {
            String body = handler.getBodyAsString("utf-8");
            String reply = "";
            String nonce = handler.request().getParam("nonce");
            if (nonce != null && nonce.length() > 0) {
                String msg_signature = handler.request().getParam("msg_signature");
                String timestamp = handler.request().getParam("timestamp");
                reply = XmlProcess.replyEncryptXml(body, nonce, msg_signature, timestamp);
            }
            else reply = XmlProcess.replyXml(body);

            handler.response().putHeader("content-type", "text/xml; charset=utf-8")
                    .end(reply);
        });

        router.get("/chatbot").handler(handler -> {
            String body = handler.getBodyAsString("utf-8");
            String reply = "";
            String echostr = handler.request().getParam("echostr");

            if (echostr != null && echostr.length() > 1) {
                String nonce = handler.request().getParam("nonce");
                String signature = handler.request().getParam("signature");
                String timestamp = handler.request().getParam("timestamp");
                String[] attrs = new String[] { Util.Token, timestamp, nonce };
                Arrays.sort(attrs);
                StringBuilder sb = new StringBuilder();
                for (String str : attrs) {
                    sb.append(str);
                }
                String sha1 = DigestUtils.sha1Hex(sb.toString());
                if (sha1.equals(signature)) reply = echostr;
            }
            handler.response().putHeader("content-type", "text/xml; charset=utf-8")
                    .end(reply);
        });

        router.get("/chattest").handler(handler -> {
            handler.response().putHeader("content-type", "text/xml; charset=utf-8")
                    .end(XmlProcess.replyXml(handler.request().getParam("msg")));
        });

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
