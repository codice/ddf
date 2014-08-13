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
    '/applications/js/controller/Feature.controller.js',
    'text!/applications/templates/plugins/features/pluginView.handlebars'
], function (Marionette, ich, FeatureController, featurePluginView) {

    ich.addTemplate('featurePluginView',featurePluginView);
    var PluginView = Marionette.Layout.extend({
        template: 'featurePluginView',
        regions: {
            featureRegion: '.feature-region'
        },
        initialize: function(){
            this.controller = new FeatureController({
                region : this.featureRegion
            });
        },
        onRender: function(){
            this.controller.show(this.model.getAppKey());
        }
    });

    return PluginView;

});