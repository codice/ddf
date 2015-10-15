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
    'underscore',
    'jquery',
    'backbone',
    'text!templates/installer/configuration.handlebars',
    'modelbinder',
    'perfectscrollbar',
    'multiselect'
    ], function (Marionette, ich, _, $, Backbone, configurationTemplate) {

    ich.addTemplate('configurationTemplate', configurationTemplate);


    var ConfigurationView = Marionette.Layout.extend({
        template: 'configurationTemplate',
        className: 'full-height',

        initialize: function(options) {
            this.navigationModel = options.navigationModel;
            this.navigationModel.set("hidePrevious",true);
            this.listenTo(this.navigationModel,'next', this.next);
        },
        next: function() {
            this.navigationModel.nextStep('', 100);
        }
    });

    return ConfigurationView;
});