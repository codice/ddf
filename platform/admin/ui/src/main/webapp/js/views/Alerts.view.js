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
define([
        'marionette',
        'text!templates/alerts.handlebars',
        'icanhaz',
        'jquery'
    ],
    function (Marionette, alertsTemplate, ich, $) {

        ich.addTemplate('alertsTemplate', alertsTemplate);

        var AlertsView = {};

        var dismissUrl = '/admin/jolokia/exec/org.codice.ddf.admin.core.alert.service.internal.AlertServiceBean:service=alert-service/dismissAlert/';

        AlertsView.View = Marionette.ItemView.extend({
            template: 'alertsTemplate',
            events: {
                'shown.bs.collapse': 'toggleDetailsMsg',
                'hidden.bs.collapse': 'toggleDetailsMsg',
                'click .dismiss': 'dismissAlert'
            },
            modelEvents: {
                'change': 'render'
            },
            /*jshint -W030 */
            toggleDetailsMsg: function () {
                var model = this.model;
                model.get('details-button-action') === 'Show' ? model.set('details-button-action', 'Hide') : model.set('details-button-action', 'Show');
                model.get('collapse') === 'out' ? model.set('collapse', 'in') : model.set('collapse', 'out');
            },
            serializeData: function () {
                var json = this.model.toJSON();
                json.collapseId = 'alertCollapse_' + parseInt((Math.random() * Math.pow(2, 32)), 10);
                return json;
            },
            // Performs the actual AJAX call to dismiss the alert
            dismissAlert: function() {
                var model = this.model;

                $.ajax({
                    type: 'GET',
                    url: dismissUrl + model.get('key') + '/',
                    dataType: 'JSON'
                }).success(function(response) {
                    if (response.value === true) {
                        model.destroy();
                    }
                });
            }
        });

        return AlertsView;
    }
);