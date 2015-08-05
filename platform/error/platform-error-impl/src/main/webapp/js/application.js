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
/*global define,window,atob,JSON,encodeURI*/

// #Main Application
define(['backbone',
        'marionette',
        'icanhaz',
        'text!templates/error.handlebars'
    ], function (Backbone, Marionette, ich, errorTemplate) {
    'use strict';

    var Application = {};

    ich.addTemplate('errorTemplate', errorTemplate);

    Application.App = new Marionette.Application();

    //add regions
    Application.App.addRegions({
        mainRegion: 'main'
    });

    Application.App.addInitializer(function() {
        var data = atob(window.data);
        var dataObj = JSON.parse(data);
        var model = new Backbone.Model({
            code: dataObj.code,
            message: dataObj.message,
            type: dataObj.type,
            throwable: dataObj.throwable,
            uri: encodeURI(dataObj.uri)
        });
        var ErrorForm = Marionette.ItemView.extend({
            template: 'errorTemplate',
            events: {
                'click .error-ellipsis': 'showTrace'
            },
            showTrace: function() {
                this.$('.error-throwable').css('height', 'auto').css('overflow', 'visible');
                this.$('.error-ellipsis').hide();
            }
        });
        var errorForm = new ErrorForm({model: model});
        Application.App.mainRegion.show(errorForm);
    });

    return Application;
});
