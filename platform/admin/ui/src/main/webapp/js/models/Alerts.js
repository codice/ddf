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
define([
    'underscore',
    'backbone'
], function (_, Backbone) {

    var AlertsModel = {};

    AlertsModel.AlertsDefaults = Backbone.Model.extend({
        defaults: {
            'banner': 'You should look at this.',
            'button': 'Show',
            'collapse': 'out',
            'type': 'danger'
        }
    });

    //setup insecure defaults alerts
    AlertsModel.InsecureAlerts = AlertsModel.AlertsDefaults.extend({
        defaults: _.extend({}, AlertsModel.AlertsDefaults.prototype.defaults, {
            'banner': 'The system is insecure because default configuration values are in use.'
        }),
        url: "/admin/jolokia/exec/org.codice.ddf.admin.insecure.defaults.service.InsecureDefaultsServiceBean:service=insecure-defaults-service/validate",
        parse: function (resp) {
            return {'items': resp.value};
        }
    });

    AlertsModel.JolokiaAlerts = AlertsModel.AlertsDefaults.extend({
        defaults: _.extend({}, AlertsModel.AlertsDefaults.prototype.defaults, {
            'banner': 'Unable to save your changes.'
        }),
        parse: function (response) {
            if (response.stacktrace) {
                var json = {'items': response.stacktrace.split(/\n/)};
                if (response.stacktrace === 'Forbidden') {
                    json.banner = 'Your session has expired. Please <a href="/login/index.html?prevurl=/admin/">log in</a> again.';
                }
                return json;
            } else {
                return response;
            }
        }
    });

    AlertsModel.Jolokia = function (jolokiaResponse) {
        return new AlertsModel.JolokiaAlerts(jolokiaResponse, {'parse': true});
    };

    return AlertsModel;
});