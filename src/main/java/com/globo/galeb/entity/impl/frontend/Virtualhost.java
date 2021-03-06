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
package com.globo.galeb.entity.impl.frontend;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.criteria.ICriterion.CriterionAction;
import com.globo.galeb.criteria.impl.RulesCriterion;
import com.globo.galeb.entity.EntitiesMap;

/**
 * Class Virtualhost.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 10, 2014.
 */
public class Virtualhost extends EntitiesMap<Rule> {

    /** The Constant ENABLE_ACCESSLOG_FIELDNAME. */
    public static final String ENABLE_ACCESSLOG_FIELDNAME = "enableAccessLog";

    /** The Constant ENABLE_CHUNKED_FIELDNAME. */
    public static final String ENABLE_CHUNKED_FIELDNAME   = "enableChunked";

    /** The Constant RULES_FIELDNAME. */
    public static final String RULES_FIELDNAME            = "rules";


    /**
     * Instantiates a new virtual server.
     */
    public Virtualhost() {
        this(UNDEF);
    }

    /**
     * Instantiates a new virtual server.
     *
     * @param id the id
     */
    public Virtualhost(String id) {
        this(new JsonObject().putString(ID_FIELDNAME, id));
    }

    /**
     * Instantiates a new virtual server.
     *
     * @param json the json
     */
    public Virtualhost(JsonObject json) {
        super(json);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.Entity#start()
     */
    @Override
    public void start() {
        setCriterion(new RulesCriterion().given(getEntities()).setLog(logger));
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.Entity#toJson()
     */
    @Override
    public JsonObject toJson() {
        prepareJson();

        JsonArray rulesArray = new JsonArray();
        for (Rule rule: getEntities().values()) {
            rulesArray.addObject(rule.toJson());
        }
        idObj.putArray(RULES_FIELDNAME, rulesArray);

        return super.toJson();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.EntitiesMap#addEntity(java.lang.Object)
     */
    @Override
    public boolean addEntity(Rule entity) {
        getCriterion().action(CriterionAction.RESET_REQUIRED);
        return super.addEntity(entity);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.EntitiesMap#removeEntity(java.lang.Object)
     */
    @Override
    public boolean removeEntity(Rule entity) {
        getCriterion().action(CriterionAction.RESET_REQUIRED);
        return super.removeEntity(entity);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.EntitiesMap#removeEntity(java.lang.String)
     */
    @Override
    public boolean removeEntity(String entityId) {
        getCriterion().action(CriterionAction.RESET_REQUIRED);
        return super.removeEntity(entityId);
    }

}
