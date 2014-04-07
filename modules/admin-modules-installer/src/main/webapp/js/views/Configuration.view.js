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
    'backbone',
    '/installer/lib/configuration-module/index.js',
    '!text!/installer/templates/configurationItem.handlebars',
    'text!/installer/templates/configuration.handlebars',
    'modelbinder',
    'perfectscrollbar',
    'multiselect'
    ], function (Marionette, ich, _, Backbone, Service, configurationItem, configurationTemplate) {

    ich.addTemplate('configurationItem', configurationItem);
    ich.addTemplate('configurationTemplate', configurationTemplate);

    var serviceModelResponse = new Service.Response();

    serviceModelResponse.fetch({
        url: '/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/getService/(service.pid%3Dddf.platform.config)'
    });

    var displayedProperties = [
        {
            id: 'port',
            note: ''
        },
        {
            id: 'host',
            note: ''
        },
        {
            id: 'id',
            description: 'The name of this site. This name will be provided via web services that ask for the name of the site.',
            note: ''
        },
        {
            id: 'protocol',
            description: 'The protocol used to advertise the system. When selecting the protocol, be sure to enter the port number corresponding to that protocol.',
            note: ''
        },
        {
            id: 'organization',
            description: 'The name of the organization that runs this site.',
            note: ''
        }
    ];

    var ConfigurationItem = Marionette.ItemView.extend({
        className: 'well well-sm white',
        template: 'configurationItem',
        onRender: function() {
            if(this.model.get('optionLabels') && this.model.get('optionLabels').length !== 0) {
                this.$('[name=' + this.model.get('id') + ']').multiselect();
            }
        }
    });

    var ConfigurationCollection = Marionette.CollectionView.extend({
        itemView: ConfigurationItem,
        buildItemView: function(item, ItemViewType, itemViewOptions){
            var view;
            _.each(displayedProperties, function(property) {
                if(item.get('id') === property.id) {
                    //if we hardcoded specific messages above, clobber what is in the metatype
                    if(property.description) {
                        item.set({ 'description': property.description });
                    }
                    if(property.note) {
                        item.set({ 'note': property.note});
                    }
                    var options = _.extend({model: item}, itemViewOptions);

                    view = new ItemViewType(options);
                }
            });
            if(view) {
                return view;
            }
            return new Marionette.ItemView();
        },
    });

    var validateFunction = function(attrs) {
        var validation = [];
        if(!attrs.properties.get('port') || _.isNaN(parseInt(attrs.properties.get('port'), 10))) {
            validation.push({
                message: 'Port is required and must only be digits.',
                id: 'port'
            });
        }

        if(!attrs.properties.get('host')) {
            validation.push({
                message: 'Host is required.',
                id: 'host'
            });
        }

        if(validation.length > 0) {
            return validation;
        }
    };

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
            Backbone.ModelBinder.SetOptions({modelSetOptions: {validate: true}});
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
            this.$('select').multiselect('refresh');
        },
        onClose: function() {
            this.stopListening(this.navigationModel);
            this.$('#config-form').perfectScrollbar('destroy');
        },
        next: function() {
            var view = this;
            var configuration = this.model.get('value').at(0).get('configurations').at(0);

            this.listenTo(configuration, 'invalid', function(model, error) {
                this.model.get('value').at(0).get('metatype').forEach(function(metatype) {
                    view.$('[name='+metatype.get('id')+'Error]').hide();
                    _.each(error, function(errorItem) {
                        if(metatype.get('id') === errorItem.id) {
                            view.$('[name='+metatype.get('id')+'Error]').show().html(errorItem.message);
                        }
                    });
                });
            });

            this.model.get('value').at(0).get('configurations').at(0).validate = validateFunction;

            //save the config
            var saved = configuration.save();
            if(saved) {
                saved.success(function() {
                    view.navigationModel.nextStep('Saved Configuration', 100);
                }).fail(function() {
                    view.navigationModel.nextStep('Unable to Save Configuration: check logs', 0);
                  });
            }
        },
        previous: function() {
            //this is your hook to perform any teardown that must be done before going to the previous step
            this.navigationModel.previousStep();
        }
    });

    return ConfigurationView;
});