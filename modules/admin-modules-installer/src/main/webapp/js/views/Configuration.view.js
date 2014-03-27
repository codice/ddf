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
        _ = require('underscore'),
        Backbone = require('backbone'),
        Service = require('/installer/lib/configuration-module/index.js');

    require('modelbinder');
    require('perfectscrollbar');
    require('multiselect');

    ich.addTemplate('configurationItem', require('!text!/installer/templates/configurationItem.handlebars'));
    ich.addTemplate('configurationTemplate', require('text!/installer/templates/configuration.handlebars'));

    var serviceModelResponse = new Service.Response();

    serviceModelResponse.fetch({
        url: '/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/getService/(service.pid%3Dddf.platform.config)'
    });



    var ConfigurationItem = Marionette.ItemView.extend({
        className: 'well well-sm white',
        template: 'configurationItem',
        onRender: function() {
            if(this.model.get('optionLabels') && this.model.get('optionLabels').length !== 0) {
                this.$('#' + this.model.get('id')).multiselect();
            }
        }
    });

    var ConfigurationCollection = Marionette.CollectionView.extend({
        itemView: ConfigurationItem
    });

    var ConfigurationView = Marionette.Layout.extend({
        template: 'configurationTemplate',
        className: 'full-height',
        model: serviceModelResponse,
        regions: {
            configurationItems: '#config-form'
        },
        initialize: function(options) {
            this.navigationModel = options.navigationModel;
            this.listenTo(this.navigationModel,'next', this.next);
            this.listenTo(this.navigationModel,'previous', this.previous);
            this.modelBinder = new Backbone.ModelBinder();
        },
        onRender: function() {
            var view = this;
            this.configurationItems.show(new ConfigurationCollection({collection: this.model.get('value').at(0).get('metatype')}));
            this.bind();
            _.defer(function () {
                view.$('#config-form').perfectScrollbar();
            });
        },
        bind: function() {
            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            if(this.model.get('value').at(0).get('configurations').length === 0) {
                var configuration = new Service.Configuration();
                configuration.initializeFromService(this.model.get('value').at(0));
                this.model.get('value').at(0).get('configurations').add(configuration);
            }
            this.modelBinder.bind(this.model.get('value').at(0).get('configurations').at(0).get('properties'), this.$el, bindings);
        },
        onClose: function() {
            this.stopListening(this.navigationModel);
            this.$('#config-form').perfectScrollbar('destroy');
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