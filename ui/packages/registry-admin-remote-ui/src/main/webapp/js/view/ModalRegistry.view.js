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
        'text!templates/optionListType.handlebars'
    ],
    function (ich, Marionette, Backbone, ConfigurationEdit, wreqr, _, $, Utils, Service, modalRegistry, optionListType) {

        ich.addTemplate('modalRegistry', modalRegistry);
        if (!ich.optionListType) {
            ich.addTemplate('optionListType', optionListType);
        }

        var ModalRegistry = {};

        if (!String.prototype.endsWith) {
            String.prototype.endsWith = function (searchString, position) {
                var subjectString = this.toString();
                if (typeof position !== 'number' || !isFinite(position) || Math.floor(position) !== position || position > subjectString.length) {
                    position = subjectString.length;
                }
                position -= searchString.length;
                var lastIndex = subjectString.lastIndexOf(searchString, position);
                return lastIndex !== -1 && lastIndex === position;
            };
        }


        ModalRegistry.View = Marionette.Layout.extend({
            template: 'modalRegistry',
            className: 'modal',
            events: {
                "click .submit-button": "submitData",
                "click .cancel-button": "closeAndUnbind",
                "change .registryName": "registryNameChanged",
                "change #registryUrl": "urlChanged"
            },
            regions: {
                details: '.modal-details',
                buttons: '.registry-buttons'
            },

            serializeData: function () {
                var data = {};

                if (this.model) {
                    data = this.model.toJSON();
                }
                data.mode = this.mode;

                return data;
            },
            initialize: function (options) {
                _.bindAll.apply(_, [this].concat(_.functions(this)));
                this.registry = options.registry;
                this.modelBinder = new Backbone.ModelBinder();
                this.mode = options.mode;
                this.type = options.registryType;
            },
            onRender: function () {

                var config = this.getConfig();
                var properties = config.get('properties');

                this.$el.attr('role', "dialog");
                this.$el.attr('aria-hidden', "true");
                this.$el.attr('data-backdrop', "static");
                this.renderDetails(config);
                this.initRadioButtonsUI(properties);
                if (this.mode !== 'edit') {
                    this.showError('Registry must have a valid url (non-blank/non-duplicate)');
                }
                this.rebind(properties);
            },
            submitData: function () {
                wreqr.vent.trigger('beforesave');
                var view = this;
                var service = this.getConfig();
                if (service) {
                    service.save().then(function () {
                            wreqr.vent.trigger('refreshRegistries');
                            view.closeAndUnbind();
                        },
                        function () {
                            wreqr.vent.trigger('refreshRegistries');
                        }).always(function () {
                        view.closeAndUnbind();
                    });
                }
            },
            urlChanged: function (evt) {
                var url = evt.target.value;
                var hostnamePort;
                if (url) {
                    hostnamePort = this.extractHostnameFromUrl(url);
                }
                if (hostnamePort && this.urlIsValid(url)) {
                    var oldName;
                    var newNameToSet = hostnamePort + " (" + this.type + ")";
                    //When a registry remotely connects, it's hostname and port are in parentheses. In that case, newNameInParens is used to check for duplication
                    var newNameInParens = "(" + hostnamePort + ")" + " (" + this.type + ")";
                    //find any registries that contain the name we are trying to set
                    var registryWithDupId;
                    this.registry.get('collection').models.forEach(function (registry) {
                        if (registry.id) {
                            if (registry.id.endsWith(newNameToSet) || registry.id.endsWith(newNameInParens)) {
                                registryWithDupId = registry;
                                return;
                            }
                        }
                    });
                    oldName = this.model.get('id');
                    //If there are no registries with a dup id, we can safely set the name
                    //when registryWithDupId is defined and we are in edit mode, we must check that oldName endsWith() either newNameInParens or newNameToSet
                    //if oldName does not end with either one, we have changed a registries hostname and port so that if we set its new name, we will create a duplicate registry entry
                    if (_.isUndefined(registryWithDupId) || (this.mode === 'edit' && ( oldName.endsWith(newNameToSet) || oldName.endsWith(newNameInParens)))) {
                        this.setConfigName(this.getConfig(), newNameToSet);
                        this.clearError();
                    } else {
                        this.showError("Registry must have a valid url (non-blank/non-duplicate)");
                    }

                } else {
                    this.showError("Registry must have a valid url (non-blank/non-duplicate)");
                }
            },
            extractHostnameFromUrl: function (url) {
                var myRe = new RegExp("^(?:([^:/?#.]+):)?(?://)?(([^:/?#]*)(?::(\\d*))?)((/(?:[^?#](?![^?#/]*\\.[^?#/.]+(?:[\\?#]|$)))*/?)?([^?#/]*))?(?:\\?([^#]*))?(?:#(.*))?");
                var regExUrl = myRe.exec(url);
                return regExUrl[2];
            },
            onClose: function () {
                this.modelBinder.unbind();
                this.$el.off('hidden.bs.modal');
                this.$el.off('shown.bs.modal');
            },
            closeAndUnbind: function () {
                this.modelBinder.unbind();
                this.$el.modal("hide");
            },
            rebind: function (properties) {
                var $boundData = this.$el.find('.bound-controls');
                var bindings = Backbone.ModelBinder.createDefaultBindings($boundData, 'name');
                delete bindings.value;
                this.modelBinder.bind(properties, $boundData, bindings);
            },
            initRadioButtonsUI: function (boundModel) {
                var $radios = this.$el.find('input[type=radio]');
                var view = this;

                _.each($radios, function (radio) {
                    var $radio = view.$(radio);
                    var $label = $radio.closest('label.btn');

                    if (boundModel.get($radio.attr('name')).toString() === $radio.attr('value')) {
                        $label.addClass('active');
                    } else {
                        $label.removeClass('active');
                    }
                });
            },
            registryNameChanged: function (evt) {
                var newName = this.$(evt.currentTarget).find('input').val().trim();
                this.checkName(newName);
            },
            checkName: function (newName) {
                var view = this;
                var model = view.model;
                var config = this.getConfig();

                if (newName === '') {
                    view.showError('A registry must have a unique URL.');
                    return false;
                } else if (newName !== model.get('name')) {
                    if (view.nameIsValid(newName, config.get("properties").get("registry-id"))) {
                        this.setConfigName(config, newName);
                        view.clearError();
                        return true;
                    } else {
                        view.showError('A registry configuration with the name "' + newName + '" already exists. Please choose another name.');
                        return false;
                    }
                } else {
                    //model name was reverted back to original value
                    view.clearError();
                    return true;
                }
            },
            urlIsValid: function (url) {
                var valid = false;
                var configs = this.registry.get('collection');
                var match;
                var mode = this.mode;
                var threshold;
                if (mode === 'edit') {
                    threshold = 1;
                } else {
                    threshold = 0;
                }
                var count = 0;
                configs.forEach(function (registryConfig) {
                    if (registryConfig.get('attributes').registryUrl === url) {
                        count++;
                    }
                    if (count > threshold) {
                        match = registryConfig;
                        return;
                    }
                });
                if (_.isUndefined(match)) {
                    valid = true;
                }
                return valid;
            },
            nameIsValid: function (name, registryId) {
                var valid = false;
                var configs = this.registry.get('collection');
                var match;
                configs.forEach(function (registryConfig) {
                    if ((registryConfig.get('id') === name) && (registryConfig.get("attributes")["registry-id"] !== registryId)) {
                        match = registryConfig;
                        return;
                    }
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
                        configuration: configuration
                    }));
                } else {
                    this.$(this.details.el).html('');
                    this.$(this.buttons.el).html('');
                }
            },
            showError: function (msg) {
                var view = this;
                var $group = view.$el.find('.registryUrl');
                $group.find('.error-text').text(msg).show();
                view.$el.find('.submit-button').attr('disabled', 'disabled');
                $group.addClass('has-error');
            },

            clearError: function () {
                var view = this;
                var $group = view.$el.find('.registryUrl');
                var $error = $group.find('.error-text');

                view.$el.find('.submit-button').removeAttr('disabled');
                $group.removeClass('has-error');
                $error.hide();
            },
            setConfigName: function (config, name) {
                if (!_.isUndefined(config)) {
                    var properties = config.get('properties');
                    properties.set({
                        'shortname': name,
                        'id': name,
                        'name': name
                    });
                }
            },
            getConfig: function () {
                var config;
                var type = this.type;
                if (type && this.mode === 'add') {
                    this.model.get('registryConfiguration').forEach(function (regConfig) {
                        if (regConfig.get('name') === type) {
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
