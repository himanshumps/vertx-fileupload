package com.aikido.ibmcloud.day2.fileserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class SimpleFormUploadServer extends AbstractVerticle {

  @Override
  public void start() {


    Router router = Router.router(vertx);
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("accept");
    allowedHeaders.add("X-PINGARUNER");

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    /*
     * these methods aren't necessary for this sample,
     * but you may need them for your projects
     */
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);
    router.route().handler(LoggerHandler.create(LoggerFormat.SHORT));
    router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
    router.route().handler(BodyHandler.create());
    router.post("/upload").handler(this::uploadFile);
    router.route().failureHandler(routingContext -> {
      for (FileUpload fileUpload : routingContext.fileUploads()) {
        routingContext.response().end(MessageFormat.format("{0} failed to upload", fileUpload.fileName()));
      }
    });
    final HttpServerOptions httpServerOptions = new HttpServerOptions();

    httpServerOptions
      .setIdleTimeout(60)
      .setIdleTimeoutUnit(TimeUnit.MINUTES)
      .setHandle100ContinueAutomatically(true)
      .setLogActivity(false);
    final HttpServer httpServer = vertx.createHttpServer(httpServerOptions);

    httpServer.requestHandler(router).listen(8080, handler -> {
      if (handler.succeeded()) {
        System.out.println("Server started on port 8080");
      } else {
        handler.cause().printStackTrace();
      }
    });
  }

  private void uploadFile(RoutingContext routingContext) {
    for (FileUpload fileUpload : routingContext.fileUploads()) {
      vertx.fileSystem()
        .writeFile(MessageFormat.format("/mnt/volume/{0}", fileUpload.fileName()), routingContext.getBody(),
          handler -> {
            if (handler.succeeded()) {
              routingContext.response().end(MessageFormat.format("{0} uploaded successfully", fileUpload.fileName()));
            } else {
              routingContext.response().end(MessageFormat.format("{0} failed to upload", fileUpload.fileName()));
            }
          });
    }
  }
}