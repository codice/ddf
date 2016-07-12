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
    'icanhaz',
    'marionette',
    'backbone',
    'js/view/ConfigurationEdit.view.js',
    'wreqr',
    'underscore',
    'jquery',
    'js/view/Utils.js',
    'js/model/Service.js',
    'text!templates/registryModal.handlebars',
    'text!templates/optionListType.handlebars',
    'text!templates/textType.handlebars'
],
function (ich,Marionette,Backbone,ConfigurationEdit,wreqr,_,$,Utils,Service,modalRegistry,optionListType,textType) {

    ich.addTemplate('modalRegistry', modalRegistry);
    if (!ich.optionListType) {
        ich.addTemplate('optionListType', optionListType);
    }
    if (!ich.textType) {
        ich.addTemplate('textType', textType);
    }

    var ModalRegistry = {};

    ModalRegistry.View = Marionette.Layout.extend({
        template: 'modalRegistry',
        className: 'modal',
        events: {
            "click .submit-button": "submitData",
            "click .cancel-button": "closeAndUnbind",
            "change .registryName": "registryNameChanged"
        },
        regions: {
            details: '.modal-details',
            buttons: '.registry-buttons'
        },
        serializeData: function(){
            var data = {};

            if(this.model) {
                data = this.model.toJSON();
            }
            data.mode = this.mode;

            return data;
        },
        initialize: function(options){
            _.bindAll(this);
            this.registry = options.registry;
            this.modelBinder = new Backbone.ModelBinder();
            this.mode = options.mode;
            this.type = options.registryType;
        },
        onRender: function() {

            var config = this.getConfig();
            var properties = config.get('properties');

            this.$el.attr('role', "dialog");
            this.$el.attr('aria-hidden', "true");
            this.renderNameField();

            this.renderDetails(config);

            this.initRadioButtonsUI(properties);
            if(this.mode !== 'edit') {
                this.checkName('');
            }

            this.rebind(properties);
        },
        submitData: function() {
            wreqr.vent.trigger('beforesave');
            var view = this;
            var service = this.getConfig();

            service.get('properties').set('remoteName', view.model.get('remoteName'));
            if (service) {
                if (_.isUndefined(service.get('properties').id)) {
                    var name = this.$(".registryName").find('input').val().trim();
                    this.setConfigName(service, name);
                }

                service.save().then(function (response) {
                        var existingRegistry = view.registry.get('collection').find(function (item) {
                            var config;
                            item.get('registryConfiguration').forEach(function(regConfig){
                                if(regConfig.get('name') === view.type){
                                    config = regConfig;
                                }
                            });
                            return (config && config.get('properties').id === service.get('properties').id);
                        });

                        if (existingRegistry && view.mode === 'add' && existingRegistry.get('registryConfiguration') !== service) {

                            response = response.replace(/\[L[\w\.;@]*/g, '""');
                            var jsonResult = JSON.parse(response.toString().trim());

                            Service.Configuration.prototype.makeDisableCallByPid(jsonResult.request['arguments'][0]).done(function () {
                                wreqr.vent.trigger('refreshRegistries');
                                view.closeAndUnbind();
                            });
                        } else {
                            wreqr.vent.trigger('refreshRegistries');
                            view.closeAndUnbind();
                        }
                    },
                    function () {
                        wreqr.vent.trigger('refreshRegistries');
                    }).always(function () {
                        view.closeAndUnbind();
                    });
            }
        },
        onClose: function() {
            this.modelBinder.unbind();
            this.$el.off('hidden.bs.modal');
            this.$el.off('shown.bs.modal');
        },
        closeAndUnbind: function() {
            this.modelBinder.unbind();
            this.$el.modal("hide");
        },
        rebind: function (properties) {
            var $boundData = this.$el.find('.bound-controls');
            var bindings = Backbone.ModelBinder.createDefaultBindings($boundData, 'name');
            delete bindings.value;
            this.modelBinder.bind(properties, $boundData, bindings);
        },
        initRadioButtonsUI: function(boundModel) {
            var $radios = this.$el.find('input[type=radio]');
            var view = this;

            _.each($radios, function(radio) {
                var $radio = view.$(radio);
                var $label = $radio.closest('label.btn');

                if (boundModel.get($radio.attr('name')).toString() === $radio.attr('value')) {
                    $label.addClass('active');
                } else {
                    $label.removeClass('active');
                }
            });
        },
        renderNameField: function() {
            var model = this.model;
            var $registryName = this.$(".registryName");
            var initialName = model.get('name');
            var data = {
                id: model.id,
                name: 'Registry Name',
                defaultValue: [initialName],
                description: 'Unique identifier for a remote registry configuration.'
            };
            $registryName.append(ich.textType(data));
            $registryName.val(data.defaultValue);
            Utils.setupPopOvers($registryName, data.id, data.name, data.description);
        },
        registryNameChanged: function(evt) {
            var newName = this.$(evt.currentTarget).find('input').val().trim();
            this.checkName(newName);
        },
        checkName: function(newName) {
            var view = this;
            var model = view.model;
            var config = this.getConfig();

            if (newName === '') {
                view.showError('A registry must have a unique name.');
            } else if (newName !== model.get('name')) {
                if (view.nameIsValid(newName)) {
                    this.setConfigName(config, newName);
                    view.clearError();
                } else {
                    view.showError('A registry configuration with the name "' + newName + '" already exists. Please choose another name.');
                }
            } else {
                //model name was reverted back to original value
                view.clearError();
            }
        },
        nameExists: function(name) {
            var configs = this.parentModel.get('collection');
            var match = configs.find(function(registryConfig) {
                    return registryConfig.get('name') === name;
                });
            return !_.isUndefined(match);
        },
        nameIsValid: function(name) {
            var valid = false;
            var configs = this.registry.get('collection');
            var match = configs.find(function(registryConfig) {
                return registryConfig.get('id') === name;
            });
            if (_.isUndefined(match)) {
                valid = true;
            }
            return valid;
        },
        renderDetails: function (configuration) {
            var service = configuration.get('service');
            if (!_.isUndefined(service)) {
                var toDisplay = service.get('metatype').filter(function (mt) {
                    return !_.contains(['shortname', 'id'], mt.get('id'));
                });
                this.details.show(new ConfigurationEdit.ConfigurationCollection({
                    collection: new Service.MetatypeList(toDisplay),
                    service: service,
                    configuration: configuration}));
            } else {
                this.$(this.details.el).html('');
                this.$(this.buttons.el).html('');
            }
        },
        showError: function(msg) {
            var view = this;
            var $group = view.$el.find('.registryName>.control-group');

            $group.find('.error-text').text(msg).show();
            view.$el.find('.submit-button').attr('disabled','disabled');
            $group.addClass('has-error');
        },
        clearError: function() {
            var view = this;
            var $group = view.$el.find('.registryName>.control-group');
            var $error = $group.find('.error-text');

            view.$el.find('.submit-button').removeAttr('disabled');
            $group.removeClass('has-error');
            $error.hide();
        },
        setConfigName: function(config, name) {
            if (!_.isUndefined(config)) {
                var properties =  config.get('properties');
                properties.set({
                    'shortname': name,
                    'id': name
                });
            }
        },
        getConfig: function() {
            var config;
            var type = this.type;
            if(type && this.mode === 'add'){
                this.model.get('registryConfiguration').forEach(function(regConfig){
                    if (regConfig.get('name') === type){
                        config = regConfig;
                    }
                });
            } else {
                config = this.model.get('registryConfiguration').at(0);
            }

            return config;
        }
    });

    return ModalRegistry;
});
