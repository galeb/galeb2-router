/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.globo.galeb.verticles;

import com.globo.galeb.bus.IEventObserver;
import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.bus.VertxQueueService;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.handlers.DeleteMatcherHandler;
import com.globo.galeb.handlers.GetMatcherHandler;
import com.globo.galeb.handlers.PostMatcherHandler;
import com.globo.galeb.handlers.PutMatcherHandler;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.metrics.CounterWithEventBus;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.rulereturn.HttpCode;
import com.globo.galeb.server.Server;
import com.globo.galeb.server.ServerResponse;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Class RouteManagerVerticle.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RouteManagerVerticle extends Verticle implements IEventObserver {

    /** The route manager id. */
    private static String routeManagerId = "route_manager";

    /** The logger. */
    private SafeLogger log;

    /** The server instance. */
    private Server server;

    /** The http server name. */
    private String httpServerName = null;

    /** The farm instance. */
    private Farm farm;

    /** The queue service. */
    private IQueueService queueService;

    /** The Constant URI_PATTERN_REGEX. */
    private static final String URI_PATTERN_REGEX = "\\/([^\\/]+)[\\/]?([^\\/]+)?";

    /* (non-Javadoc)
     * @see org.vertx.java.platform.Verticle#start()
     */
    @Override
    public void start() {
        log = new SafeLogger().setLogger(container.logger());
        final JsonObject conf = container.config();
        final JsonObject starterConf = conf.getObject(ConfVerticleDictionary.CONF_STARTER_CONF, new JsonObject());
        final ICounter counter = new CounterWithEventBus(vertx.eventBus());
        server = new Server(vertx, container, counter);
        queueService = new VertxQueueService(vertx.eventBus(), log);
        farm = new Farm(this);
        farm.setLogger(log)
            .setPlataform(vertx)
            .setQueueService(queueService)
            .setCounter(counter)
            .setStaticConf(starterConf.getObject(ConfVerticleDictionary.CONF_ROOT_ROUTER, new JsonObject()))
            .start();

        startHttpServer(conf);

        log.info(String.format("Instance %s started", this.toString()));
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IEventObserver#setVersion(java.lang.Long)
     */
    @Override
    public void setVersion(Long version) {
        farm.setVersion(version);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IEventObserver#postAddEvent(java.lang.String)
     */
    @Override
    public void postAddEvent(String message) {
        return;
    };

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IEventObserver#postDelEvent(java.lang.String)
     */
    @Override
    public void postDelEvent(String message) {
        return;
    };

    /**
     * Start http server.
     *
     * @param serverConf the server conf
     */
    private void startHttpServer(final JsonObject serverConf) {

        RouteMatcher routeMatcher = new RouteMatcher();

        routeMatcher.getWithRegEx(URI_PATTERN_REGEX, new GetMatcherHandler(routeManagerId, log, farm));

        routeMatcher.post("/:uriBase", new PostMatcherHandler(routeManagerId, log, queueService));

        routeMatcher.deleteWithRegEx(URI_PATTERN_REGEX, new DeleteMatcherHandler(routeManagerId, log, queueService));

        routeMatcher.putWithRegEx(URI_PATTERN_REGEX, new PutMatcherHandler(routeManagerId, log, queueService));

        routeMatcher.noMatch(new Handler<HttpServerRequest>() {

            @Override
            public void handle(HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req).setLog(log);

                if (httpServerName==null) {
                    httpServerName = req.headers().contains(HttpHeaders.HOST) ? req.headers().get(HttpHeaders.HOST) : "SERVER";
                    Server.setHttpServerName(httpServerName);
                }

                int statusCode = HttpCode.BAD_REQUEST;
                serverResponse.setStatusCode(statusCode)
                    .setMessage(HttpCode.getMessage(statusCode, true))
                    .setId(routeManagerId)
                    .endResponse();
                log.warn(String.format("%s %s not supported", req.method(), req.uri()));
            }
        });

        server.setDefaultPort(9000).setHttpServerRequestHandler(routeMatcher).start(this);
    }

}
