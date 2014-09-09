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
package com.globo.galeb.core;

import static com.globo.galeb.core.Constants.QUEUE_ROUTE_ADD;
import static com.globo.galeb.core.Constants.QUEUE_ROUTE_DEL;
import static com.globo.galeb.core.Constants.QUEUE_ROUTE_VERSION;

import java.util.Iterator;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class QueueMap {

    private final Verticle verticle;
    private final Vertx vertx;
    private final EventBus eb;
    private final Logger log;
    private final Map<String, Virtualhost> virtualhosts;

    public static String buildMessage(String virtualhostStr,
                                      String hostStr,
                                      String portStr,
                                      String statusStr,
                                      String uriStr,
                                      String properties)
    {
        JsonObject messageJson = new JsonObject();
        JsonObject virtualhostObj = new JsonObject().putString("name", virtualhostStr);

        try {
            virtualhostObj.putObject("properties", new JsonObject(properties));
        } catch (DecodeException ignoreBadJson) {
            virtualhostObj.putObject("properties", new JsonObject());
        }
        messageJson.putString("virtualhost", virtualhostObj.encode());
        messageJson.putString("host", hostStr);
        messageJson.putString("port", portStr);
        messageJson.putString("status", statusStr);
        messageJson.putString("uri", uriStr);

        return messageJson.toString();
    }

    public QueueMap(final Verticle verticle, final Map<String, Virtualhost> virtualhosts) {
        this.verticle = verticle;
        this.vertx = (verticle != null) ? verticle.getVertx() : null;
        this.eb=(verticle != null) ? verticle.getVertx().eventBus() : null;
        this.log=(verticle != null) ? verticle.getContainer().logger() : null;
        this.virtualhosts=virtualhosts;
    }

    public boolean processAddMessage(String message) {
        if (virtualhosts==null) {
            return false;
        }

        boolean isOk = true;
        JsonObject messageJson = new JsonObject(message);
        JsonObject virtualhostJson = new JsonObject(messageJson.getString("virtualhost", "{}"));
        String virtualhost = virtualhostJson.getString("name", "");
        String uri = messageJson.getString("uri", "");
        String uriBase = uri.split("/")[1];

        switch (uriBase) {
            case "route":
            case "virtualhost":
                if (!virtualhosts.containsKey(virtualhost)) {
                    Virtualhost newVirtualhostObj = new Virtualhost(virtualhostJson, vertx);
                    virtualhosts.put(virtualhost, newVirtualhostObj);
                    log.info(String.format("[%s] Virtualhost %s added", verticle.toString(), virtualhost));
                    isOk = true;
                } else {
                    isOk = false;
                }
                break;
            case "backend":
                if (!virtualhosts.containsKey(virtualhost)) {
                    log.warn(String.format("[%s] Backend didnt create, because Virtualhost %s not exist", verticle.toString(), virtualhost));
                    isOk = false;
                } else {

                    String host = messageJson.getString("host", "");
                    String port = messageJson.getString("port", "");
                    boolean status = !"0".equals(messageJson.getString("status", ""));
                    String backend = (!"".equals(host) && !"".equals(port)) ?
                            String.format("%s:%s", host, port) : "";

                    final Virtualhost vhost = virtualhosts.get(virtualhost);
                    if (vhost.addBackend(backend, status)) {
                        log.info(String.format("[%s] Backend %s (%s) added", verticle.toString(), backend, virtualhost));
                    } else {
                        log.warn(String.format("[%s] Backend %s (%s) already exist", verticle.toString(), backend, virtualhost));
                        isOk = false;
                    }
                }
                break;
            default:
                log.warn(String.format("[%s] uriBase %s not supported", verticle.toString(), uriBase));
                isOk = false;
                break;
        }
        return isOk;
    }

    public boolean processDelMessage(String message) {
        if (virtualhosts==null) {
            return false;
        }

        boolean isOk = true;
        JsonObject messageJson = new JsonObject(message);
        JsonObject virtualhostJson = new JsonObject(messageJson.getString("virtualhost", "{}"));
        String virtualhost = virtualhostJson.getString("name", "");
        String host = messageJson.getString("host", "");
        String port = messageJson.getString("port");
        boolean status = !"0".equals(messageJson.getString("status", ""));
        String uri = messageJson.getString("uri", "");
//        String properties = messageJson.getString("properties", "{}");

        String backend = (!"".equals(host) && !"".equals(port)) ?
                String.format("%s:%s", host, port) : "";
        String uriBase = uri.split("/")[1];

        switch (uriBase) {
            case "route":
                Iterator<Virtualhost> iterVirtualhost = virtualhosts.values().iterator();
                while (iterVirtualhost.hasNext()) {
                    Virtualhost aVirtualhost = iterVirtualhost.next();
                    if (aVirtualhost!=null) {
                        aVirtualhost.clear(true);
                        aVirtualhost.clear(false);
                    }
                }
                virtualhosts.clear();
                log.info(String.format("[%s] All routes were cleaned", verticle.toString()));
                break;
            case "virtualhost":
                if (virtualhosts.containsKey(virtualhost)) {
                    virtualhosts.get(virtualhost).clearAll();
                    virtualhosts.remove(virtualhost);
                    log.info(String.format("[%s] Virtualhost %s removed", verticle.toString(), virtualhost));
                } else {
                    log.warn(String.format("[%s] Virtualhost not removed. Virtualhost %s not exist", verticle.toString(), virtualhost));
                    isOk = false;
                }
                break;
            case "backend":
                if ("".equals(backend)) {
                    log.warn(String.format("[%s] Backend UNDEF", verticle.toString()));
                    isOk = false;
                } else if (!virtualhosts.containsKey(virtualhost)) {
                    log.warn(String.format("[%s] Backend not removed. Virtualhost %s not exist", verticle.toString(), virtualhost));
                    isOk = false;
                } else {
                    final Virtualhost virtualhostObj = virtualhosts.get(virtualhost);
                    if (virtualhostObj!=null && virtualhostObj.removeBackend(backend, status)) {
                        log.info(String.format("[%s] Backend %s (%s) removed", verticle.toString(), backend, virtualhost));
                    } else {
                        log.warn(String.format("[%s] Backend not removed. Backend %s (%s) not exist", verticle.toString(), backend, virtualhost));
                        isOk = false;
                    }
                }
                break;
            default:
                log.warn(String.format("[%s] uriBase %s not supported", verticle.toString(), uriBase));
                isOk = false;
                break;
        }
        return isOk;
    }

    public void processVersionMessage(String message) {
        try {
            setVersion(Long.parseLong(message));
        } catch (java.lang.NumberFormatException e) {}
    }

    public void registerQueueAdd() {
        Handler<Message<String>> queueAddHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                processAddMessage(message.body());
                postAddEvent(message.body());
            }
        };
        if (eb != null) {
            eb.registerHandler(QUEUE_ROUTE_ADD, queueAddHandler);
        }
    }

    public void registerQueueDel() {
        Handler<Message<String>> queueDelHandler =  new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                processDelMessage(message.body());
                postDelEvent(message.body());
            }
        };
        if (eb!=null) {
            eb.registerHandler(QUEUE_ROUTE_DEL,queueDelHandler);
        }
    }

    public void registerQueueVersion() {
        Handler<Message<String>> queueVersionHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                processVersionMessage(message.body());
            }
        };
        if (eb!=null) {
            eb.registerHandler(QUEUE_ROUTE_VERSION, queueVersionHandler);
        }
    }

    private void setVersion(Long version) {
        if (verticle instanceof IEventObserver) {
            ((IEventObserver)verticle).setVersion(version);
            log.info(String.format("[%s] POST /version: %d", verticle.toString(), version));
        }
    }

    private void postDelEvent(String message) {
        if (verticle instanceof IEventObserver) {
            ((IEventObserver)verticle).postDelEvent(message);
        }
    }

    private void postAddEvent(String message) {
        if (verticle instanceof IEventObserver) {
            ((IEventObserver)verticle).postAddEvent(message);
        }
    }
}
