/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.common.test.restito;

import java.util.Map;

import com.xebialabs.restito.semantics.Call;
import com.xebialabs.restito.semantics.Predicate;

/**
 * Class used to capture record the headers from the incoming request to the StubServer.
 * Restito does not pass the incoming request from inside of the response function, so this
 * class is needed to extract any headers needed for the response.
 */
public class HeaderCapture implements Predicate<Call> {

    private Map<String, String> headers;

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public boolean apply(Call call) {
        headers = call.getHeaders();
        return true;
    }
}