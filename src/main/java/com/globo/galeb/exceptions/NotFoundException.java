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
package com.globo.galeb.exceptions;

import com.globo.galeb.rulereturn.HttpCode;

/**
 * Class NotFoundException.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class NotFoundException extends AbstractHttpException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -1148048070375338827L;

    /**
     * Instantiates a new not found exception.
     */
    public NotFoundException() {
        super(HttpCode.NOT_FOUND);
    }
}
