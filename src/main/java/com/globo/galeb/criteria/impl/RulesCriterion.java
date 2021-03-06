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
package com.globo.galeb.criteria.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.entity.impl.frontend.NullRule;
import com.globo.galeb.entity.impl.frontend.Rule;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.http.HttpServerRequest;

/**
 * Class RulesCriterion.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 10, 2014.
 * @param <T> the generic type
 */
public class RulesCriterion implements ICriterion<Rule> {

    /** The log. */
    private SafeLogger        log = null;

    /** The map. */
    private Map<String, Rule> map = null;

    /** The rule list */
    private List<Rule> ruleList = new ArrayList<>();

    /** The request match. */
    private RequestMatch requestMatch;

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#setLog(org.vertx.java.core.logging.Logger)
     */
    @Override
    public ICriterion<Rule> setLog(final SafeLogger logger) {
        log = logger;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#given(java.util.Map)
     */
    @Override
    public ICriterion<Rule> given(final Map<String, Rule> map) {
        this.map = map;
        ruleList.addAll(map.values());
        Collections.sort(ruleList);
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#when(java.lang.Object)
     */
    @Override
    public ICriterion<Rule> when(final Object param) {
        if (param instanceof HttpServerRequest) {
            requestMatch = new RequestMatch((HttpServerRequest)param);
        } else {
            if (log==null) {
                log = new SafeLogger();
            }
            log.warn(String.format("Param is instance of %s.class. Expected %s.class",
                    param.getClass().getSimpleName(), HttpServerRequest.class.getSimpleName()));
        }
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#thenGetResult()
     */
    @Override
    public Rule thenGetResult() {
        Rule ruleDefault = null;
        if (ruleList.isEmpty()) {
            ruleList.addAll(map.values());
            Comparator<? super Rule> comparator = new Comparator<Rule>() {
                @Override
                public int compare(Rule r1, Rule r2) {
                    return r1.getPriorityOrder()-r2.getPriorityOrder();
                }
            };
            Collections.sort(ruleList, comparator);
        }

        for (Rule rule: ruleList) {
            if (rule==null) {
                continue;
            }
            if (ruleDefault==null && rule.isRuleDefault()) {
                ruleDefault = rule;
            }
            if (rule.isMatchWith(requestMatch)) {
                return rule;
            }
        }
        if (ruleDefault!=null) {
            if (log==null) {
                log = new SafeLogger();
            }
            log.debug(String.format("Calling default rule %s [%s]", ruleDefault.getId(), ruleDefault.getClass().getSimpleName()));
            return ruleDefault;
        }
        return new NullRule();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#action(com.globo.galeb.criteria.ICriterion.CriterionAction)
     */
    @Override
    public ICriterion<Rule> action(ICriterion.CriterionAction criterionAction) {
        switch (criterionAction) {
            case RESET_REQUIRED:
                ruleList.clear();
                break;

            default:
                break;
        }
        return this;
    }
}
