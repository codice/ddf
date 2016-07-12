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
/*global define,window*/
define(['marionette',
        'wreqr',
        'properties',
        'js/view/SystemUsageModal.view'
    ], function (Marionette, wreqr, properties, SystemUsageModal) {
        'use strict';

        return Marionette.Controller.extend({
            shouldDisplayModal: function () {
                return properties.ui.systemUsageTitle && (
                        window.localStorage.getItem('systemUsage') === null ||
                        !properties.ui.systemUsageOncePerSession);
            },
            initialize: function () {
                if (this.shouldDisplayModal()) {
                    window.localStorage.setItem('systemUsage', 'true');
                    var modal = new SystemUsageModal();
                    wreqr.vent.trigger('showModal', modal);
                }
            }

        });
    }
);
