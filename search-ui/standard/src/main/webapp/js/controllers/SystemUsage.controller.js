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
/*global define */
define(['jquery',
        'underscore',
        'marionette',
        'wreqr',
        'properties',
        'js/view/SystemUsageModal.view',
        'jqueryCookie'
    ], function ($, _, Marionette, wreqr, properties, SystemUsageModal) {
        'use strict';
        var SystemUsageController;

        SystemUsageController = Marionette.Controller.extend({
            initialize: function () {
                if(properties.ui.systemUsageTitle && (_.isUndefined($.cookie("systemUsage")) ||
                        !properties.ui.systemUsageOncePerSession)) {
                    $.cookie("systemUsage", true);
                    var modal = new SystemUsageModal();
                    wreqr.vent.trigger('showModal', modal);
                }
            }

        });

        return SystemUsageController;
    }
);
