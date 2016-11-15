/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
        'icanhaz'
    ],
    function (ich) {
        "use strict";

        // The module to be exported
        var helper, helpers = {
            is: function (value, test, options) {
                if (value === test) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            },
            containsCsw: function(value, options) {
                if (value.includes("CSW")) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            },
            isnt: function (value, test, options) {
                if (value !== test) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            },
            debug : function() {
                console.log("Current Context");
                console.log("====================");
                console.log(this);

            }
        };

        // Export helpers
        for (helper in helpers) {
            if (helpers.hasOwnProperty(helper)) {
                ich.addHelper(helper, helpers[helper]);
            }
        }
    });
