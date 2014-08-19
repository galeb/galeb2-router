/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package lbaas;

import static lbaas.Constants.CONF_PORT;

import lbaas.exceptions.BadRequestException;
import io.netty.handler.codec.http.HttpResponseStatus;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

public class Server {

    private final Vertx vertx;
    private final JsonObject conf;
    private final Logger log;
    private final ICounter counter;

    public Server(final Vertx vertx, final Container container, final ICounter counter) {
        this.vertx = vertx;
        this.conf = container.config();
        this.log = container.logger();
        this.counter = counter;
    }

    public void start(
            final Object caller,
            final Handler<HttpServerRequest> handlerHttpServerRequest,
            final Integer defaultPort) {

        final Integer port = conf.getInteger(CONF_PORT,defaultPort);

        vertx.createHttpServer().requestHandler(handlerHttpServerRequest)
            .setTCPKeepAlive(conf.getBoolean("serverTCPKeepAlive",true))
            .listen(port, new Handler<AsyncResult<HttpServer>>() {
                public void handle(AsyncResult<HttpServer> asyncResult) {
                    if (asyncResult.succeeded()) {
                        log.info(String.format("[%s] Server listen: %d/tcp", caller.toString(), port));
                        EventBus eb = vertx.eventBus();
                        eb.publish("init.server", String.format("{ \"id\": \"%s\", \"status\": \"started\" }", caller.toString()));
                    } else {
                        log.fatal(String.format("[%s] Could not start server port: %d/tcp", caller.toString(), port));
                    }
                }
         });
    }

    public void showErrorAndClose(final HttpServerRequest req, final Throwable event, String key) {

        if (event instanceof java.util.concurrent.TimeoutException) {
            returnStatus(req, 504, null, key);
        } else if (event instanceof BadRequestException) {
            returnStatus(req, 400, null, key);
        } else {
            returnStatus(req, 502, null, key);
            log.error(String.format("ERROR 502: Virtualhost %s",
                    req.headers().contains("Host")? req.headers().get("Host"): "UNDEF"), event);
        }

        close(req);
    }

    public void close(final HttpServerRequest req) {
        try {
            req.response().close();
        } catch (RuntimeException e) {
            // Ignore already closed
            return;
        }
    }

    public void returnStatus(final HttpServerRequest req, Integer code) {
        returnStatus(req, code, "");
    }

    public void returnStatus(final HttpServerRequest req, Integer code, String message) {
        returnStatus(req, code, message, null);
    }

    public void returnStatus(final HttpServerRequest req, Integer code, String message, String id) {
        req.response().setStatusCode(code);
        req.response().setStatusMessage(HttpResponseStatus.valueOf(code).reasonPhrase());
        String messageReturn = message;
        if (counter!=null) {
            counter.httpCode(id, code);
        }

        if (message != null) {
            if ("".equals(message)) {
                req.response().headers().set("Content-Type", "application/json");
                JsonObject json = new JsonObject(
                        String.format("{ \"status_message\":\"%s\"}", req.response().getStatusMessage()));
                messageReturn = json.encodePrettily();
            }
            try {
                req.response().end(messageReturn);
            } catch (java.lang.IllegalStateException e) {
                // Response has already been written ?
                log.error(e.getMessage());
                return;
            } catch (RuntimeException e) {
                log.error(e.getMessage());
                return;
            }
        } else {
            try {
                req.response().end();
            } catch (java.lang.IllegalStateException e) {
                // Response has already been written ?
                log.error(e.getMessage());
                return;
            } catch (RuntimeException e) {
                log.error(e.getMessage());
                return;
            }
        }
    }

}
