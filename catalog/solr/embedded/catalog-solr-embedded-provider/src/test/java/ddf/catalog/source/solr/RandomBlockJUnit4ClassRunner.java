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
package ddf.catalog.source.solr;

import java.util.Collections;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class RandomBlockJUnit4ClassRunner extends BlockJUnit4ClassRunner {

    public RandomBlockJUnit4ClassRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    protected java.util.List<org.junit.runners.model.FrameworkMethod> computeTestMethods() {
        java.util.List<org.junit.runners.model.FrameworkMethod> methods = super
                .computeTestMethods();
        Collections.shuffle(methods);
        return methods;
    }

}