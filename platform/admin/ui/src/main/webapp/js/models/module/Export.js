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
    'underscore',
    'backbone',
    'jquery'
], function (_, Backbone, $) {

    function getConstructedUrl(model) {
        return url + getEscapedPath(model);
    }

    function getEscapedPath(model) {
        return model.get('path').split('/').join('!/');
    }

    function handleSuccess(model, response) {
        if (response.error) {
            addError(model, response.error);
        } else if (response.value.length !== 0) {
            _.forEach(response.value, function (warning) {
                addWarning(model, warning.message);
            });
        }
    }

    function handleFailure(model, response) {
        var message;
        switch (response.status) {
            case 403:
                message = 'Your session has expired. Please <a href="/login/index.html?prevurl=/admin/" target="_blank">log in</a> again.';
                break;
            default:
                message = response.status + ': ' + response.statusText;
                break;
        }
        addError(model, message);
    }

    function clearErrorsAndWarnings(model) {
        model.set('warnings', []);
        model.set('errors', []);
    }

    function addError(model, message) {
        var errors = model.get('errors');
        model.set('errors', errors.concat([message]));
    }

    function addWarning(model, message) {
        var warnings = model.get('warnings');
        model.set('warnings', warnings.concat([message]));
    }

    var url = '/admin/jolokia/exec/org.codice.ddf.configuration.migration.ConfigurationMigrationManager:service=configuration-migration/export/';

    var ExportModel = Backbone.Model.extend({
        defaults: {
            path: 'etc/exported',
            warnings: [],
            errors: [],
            inProgress: false
        },
        export: function () {
            var model = this;
            clearErrorsAndWarnings(model);
            model.set('inProgress', true);
            $.ajax({
                type: 'GET',
                url: getConstructedUrl(model),
                dataType: 'JSON'
            }).then(function (response) {
                handleSuccess(model, response);
            }, function (response) {
                handleFailure(model, response);
            }).always(function () {
                model.set('inProgress', false);
            });
        }
    });

    return ExportModel;
});