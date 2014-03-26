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
define(function (require) {

    var Marionette = require('marionette'),
        ich = require('icanhaz'),
        Service = require('/installer/lib/configuration-module/index.js');

    ich.addTemplate('configurationTemplate', require('text!/installer/templates/configuration.handlebars'));

    var serviceModel = new Service.Response();

    serviceModel.fetch({
        url: '/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/getService/(service.pid%3Dddf.platform.config)'
    });

    var ConfigurationView = Marionette.ItemView.extend({
        template: 'configurationTemplate',
        tagName: 'div',
        className: 'full-height',
        model: serviceModel,
        initialize: function(options) {
            this.navigationModel = options.navigationModel;
            this.listenTo(this.navigationModel,'next', this.next);
            this.listenTo(this.navigationModel,'previous', this.previous);
        },
        onClose: function() {
            this.stopListening(this.navigationModel);
        },
        next: function() {
            //this is your hook to perform any validation you need to do before going to the next step
            this.navigationModel.nextStep();
        },
        previous: function() {
            //this is your hook to perform any teardown that must be done before going to the previous step
            this.navigationModel.previousStep();
        }
    });

    return ConfigurationView;
});