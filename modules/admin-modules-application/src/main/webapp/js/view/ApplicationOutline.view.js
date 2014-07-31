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
/*global define*/
/** Main view page for add. */
define([
        'marionette',
        'icanhaz',
        'text!applicationOutline',
        '/applications/js/view/ApplicationGrid.view.js',
        'fileupload'
    ],
    function(Marionette, ich, applicationOutline, AppGridView) {
        "use strict";

        if(!ich.applicationOutline) {
            ich.addTemplate('applicationOutline', applicationOutline);
        }

        var ApplicationView = Marionette.Layout.extend({
            template: 'applicationOutline',
            tagName: 'div',
            className: 'full-height well',
            regions: {
                applications: '#application-outline'
            },
            events: {
                'click .installConfirm': 'showStatus',
                'click .removeConfirm': 'showStatus'
            },

            initialize: function (options) {
                this.modelClass = options.modelClass;
            },
            onRender: function () {
                this.applications.show(new AppGridView({modelClass: this.modelClass}));
            }
        });

        return ApplicationView;

    });
