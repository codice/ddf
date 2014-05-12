/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package ddf.catalog.pubsub.criteria.temporal;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemporalEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemporalEvaluator.class);

    public static boolean evaluate(TemporalEvaluationCriteria tec) {
        String methodName = "evaluate";
        LOGGER.debug("ENTERING: {}", methodName);

        boolean evaluation = false;

        if (tec == null) {
            return evaluation;
        }

        Date start = tec.getStart();
        Date end = tec.getEnd();
        Date input = tec.getInput();

        LOGGER.debug("start = {},   end = {},   input = {}", start, end, input);

        if (start == null) {
            LOGGER.debug("Doing start = null evaluation");
            evaluation = input.before(end) || input.equals(end);
        } else if (end == null) {
            LOGGER.debug("Doing end = null evaluation");
            evaluation = input.after(start) || input.equals(start);
        } else if (input == null) {
            LOGGER.debug("Doing input = null evaluation - return false");
            return false;
        } else {
            LOGGER.debug("Doing start/end evaluation");
            evaluation = (input.after(start) || input.equals(start))
                    && (input.before(end) || input.equals(end));
        }

        LOGGER.debug("evaluation = {}", evaluation);

        LOGGER.debug("EXITING: {}", methodName);

        return evaluation;
    }

}
