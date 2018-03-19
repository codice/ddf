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
/* global define */
define([
    'marionette',
    'icanhaz',
    'js/controllers/Configuration.controller',
    'text!templates/application/plugins/config/pluginView.handlebars'
    ], function (Marionette, ich, ConfigurationController, configPluginViewTemplate) {

    ich.addTemplate('configPluginViewTemplate',configPluginViewTemplate);
    var PluginView = Marionette.Layout.extend({
        template: 'configPluginViewTemplate',

        regions: {
            configurationRegion: '.region'
        },
        initialize: function(){
            this.controller = new ConfigurationController({
                region : this.configurationRegion,
                url: "/admin/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/getServices/" + this.model.get('appId')
            });
        },
        onRender: function(){
            this.controller.show();
        },
        onClose: function() {
            this.controller.close();
        }
    });

    return PluginView;

});