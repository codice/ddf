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
    'js/models/Service',
    'js/wreqr.js',
    'jquery',
    '../Modal',
    'text!templates/installer/anonClaims.handlebars',
    'text!templates/installer/anonClaimProfiles.handlebars',
    'text!templates/installer/anonClaimsListHeader.handlebars',
    'text!templates/installer/anonClaimsList.handlebars',
    'text!templates/installer/anonWarningModal.handlebars'
], function (Marionette, ich, _, Backbone, Service, wreqr, $, Modal, anonClaimsTemplate, anonClaimProfiles, anonClaimsListHeader, anonClaimsList, anonWarningModal) {

    ich.addTemplate('anonClaimsTemplate', anonClaimsTemplate);
    ich.addTemplate('anonClaimProfiles', anonClaimProfiles);
    ich.addTemplate('anonClaimsListHeader', anonClaimsListHeader);
    ich.addTemplate('anonClaimsList', anonClaimsList);
    ich.addTemplate('anonWarningModal', anonWarningModal);

    var serviceModelResponse = new Service.Response();

    serviceModelResponse.fetch({
        url: '/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/getClaimsConfiguration/(service.pid%3Dddf.security.sts.anonymousclaims)'
    });

    var AnonClaimsView = Marionette.Layout.extend({
        template: 'anonClaimsTemplate',
        className: 'full-height',
        model: serviceModelResponse,
        regions: {
            anonClaimProfiles: '#claims-profiles',
            anonClaimsItems: '#config-div',
            anonClaimsModal: '#warning-container'
        },
        events: {
            "change .sourceTypesSelect": "render",
            "click .claimsContinue": "proceed",
            "click .claimsCancel": "cancel"
        },
        initialize: function (options) {
            this.navigationModel = options.navigationModel;
            this.listenTo(this.navigationModel, 'next', this.next);
            this.listenTo(this.navigationModel, 'previous', this.previous);
            this.listenTo(wreqr.vent, 'showWarnings', this.verifyContinue);
            this.listenTo(wreqr.vent, 'saveClaimData', this.saveData);
            this.valObj = this.model.get('value').at(0);
            this.configObj = this.valObj.get('configurations').at(0);
            this.configObj.set("ignoreWarnings", false);
            //setup default profile if it doesn't exist
            if (!this.valObj.get("profiles").availableProfiles.Default) {
                this.valObj.get("profiles").profileNames = this.valObj.get("profiles").profileNames.sort();
                this.valObj.get("profiles").profileNames.unshift("Default");
                this.valObj.get("profiles").availableProfiles.Default = this.valObj.get('metatype').at(0).get('defaultValue');
            }

            //close all dropdowns
            $('html').click(function () {
                $('.editable-list').hide();
            });

            Backbone.ModelBinder.SetOptions({modelSetOptions: {validate: true}});
        },
        onRender: function () {
            var view = this;
            var profileModel = new Backbone.Model(this.valObj.get('profiles'));
            this.anonClaimProfiles.show(new AnonClaimProfiles({
                model: profileModel,
                configuration: this.configObj
            }));
            this.anonClaimsItems.show(new AnonClaimsMultiValuedLayout({
                model: new Backbone.Model(this.valObj.get('claims')),
                configuration: this.configObj,
                profileModel: profileModel
            }));
            _.defer(function () {
                view.$('.scroll-area').perfectScrollbar({useKeyboard: false});
            });
        },
        onClose: function () {
            this.stopListening(this.navigationModel);
            this.$('.scroll-area').perfectScrollbar('destroy');
        },
        submitData: function () {
            wreqr.vent.trigger('beforesave');
            this.model.save();
        },
        next: function () {
            var view = this;
            this.configObj.set("ignoreWarnings", false);
            this.listenTo(this.configObj, 'invalid', function (model, errors) {
                this.configObj.get('validatedFields').forEach(function (fieldId) {
                    view.$('[name=' + fieldId + 'Error]').hide();
                });
                errors.forEach(function (errorItem) {
                    if (errorItem.name) {
                        view.$('[name=' + errorItem.name + 'Error]').show().html(errorItem.message);
                    }
                });
            });
            this.configObj.validate = this.validate;
            this.submitData();

            //save the config
            this.saveData();
        },
        saveData: function () {
            //save the config
            var view = this;
            var saved = this.configObj.save();
            if (saved) {
                saved.success(function () {
                    view.navigationModel.nextStep('', 100);
                }).fail(function () {
                    view.navigationModel.nextStep('Unable to Save Configuration: check logs', 0);
                });
            }
        },
        validate: function () {
            var errors = this.get('validationErrors');
            var warnings = this.get('validationWarnings');
            var ignoreWarnings = this.get('ignoreWarnings');
            var results = errors;
            if (!errors && warnings && !ignoreWarnings) {
                wreqr.vent.trigger('showWarnings');
                results = warnings;
            }
            return results;
        },
        verifyContinue: function () {
            var modal = new AnonWarningModal({model: new Backbone.Model(this.configObj.get('validationWarnings'))});
            this.anonClaimsModal.show(modal);
        },
        proceed: function () {
            this.configObj.set("ignoreWarnings", true);
            this.$('#warning-container').on('hidden.bs.modal', function () {
                wreqr.vent.trigger('saveClaimData');
            });

        },
        previous: function () {
            //this is your hook to perform any teardown that must be done before going to the previous step
            this.navigationModel.previousStep();
        }
    });

    var AnonClaimProfiles = Marionette.ItemView.extend({
        template: 'anonClaimProfiles',
        events: {
            "change .profile": "updateValues"
        },
        initialize: function (options) {
            this.configuration = options.configuration;
            this.model.set('curProfile', this.configuration.get('properties').get('profile'));
        },
        updateValues: function (e) {
            var profileName = e.currentTarget[e.currentTarget.selectedIndex].label;
            var profile = this.model.get('availableProfiles')[profileName];
            this.configuration.get('properties').set('attributes', profile);
            this.configuration.get('properties').set('profile', profileName);
            this.model.trigger('profileChanged');
        },
        onRender: function () {
            this.$('select').multiselect('refresh');
        }
    });

    var AnonClaimsMultiValuedEntry = Marionette.ItemView.extend({
        template: 'anonClaimsList',
        tagName: 'div',
        className: 'row-container-div',
        initialize: function () {
            this.modelBinder = new Backbone.ModelBinder();
            this.model.validate = this.validate;
        },
        events: {
            "click .minus-button": "minusButton",
            "click .editable-list-button": "showList",
            "click .editable-list-item": "selectItem",
            "mouseover .editable-list-item": "highlightItem"
        },
        modelEvents: {
            "change": "render"
        },
        minusButton: function () {
            this.model.collection.remove(this.model);
        },
        onRender: function () {
            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.model, this.$el, bindings);
            this.model.set('showWarning', _.contains(this.model.get('immutableClaims'), this.model.get('claimName')));

        },
        onClose: function () {
            this.remove();
            this.unbind();
        },
        showList: function (e) {
            e.stopPropagation();
            var list = this.$('.editable-list');
            var input = this.$('.editable-list-input');
            list.css({'top': input.outerHeight(), 'left': '0px', 'min-width': input.outerWidth()});
            list.toggle();
        },
        selectItem: function (e) {

            var newValue = this.$(e.target).text();
            this.$('.editable-list-input').val(newValue);
            this.$('.editable-list-item').removeClass('selected-list-item');
            this.$(e.target).addClass('selected-list-item');
            this.model.set('claimName', newValue);
            this.$('.editable-list').hide();
        },
        highlightItem: function (e) {
            this.$('.editable-list-item').removeClass('highlighted-list-item');
            this.$(e.target).addClass('highlighted-list-item');
        }
    });

    var AnonClaimsMultiValueCollection = Marionette.CollectionView.extend({
        itemView: AnonClaimsMultiValuedEntry,
        tagName: 'div',
        modelEvents: {
            "change": "render"
        }
    });

    var AnonClaimsMultiValuedLayout = Marionette.Layout.extend({
        template: 'anonClaimsListHeader',
        itemView: AnonClaimsMultiValueCollection,
        tagName: 'div',
        regions: {
            listItems: '#listItems'
        },
        events: {
            "click .plus-button": "plusButton"
        },
        modelEvents: {
            "change": "updateValues"
        },
        initialize: function (options) {
            _.bindAll(this);
            this.claimIdCounter = 0;
            this.configuration = options.configuration;
            this.profileModel = options.profileModel;
            this.collectionArray = new Backbone.Collection();
            this.listenTo(wreqr.vent, 'refresh', this.updateValues);
            this.listenTo(wreqr.vent, 'beforesave', this.saveValues);
            this.listenTo(this.profileModel, 'profileChanged', this.updateValues);
        },
        updateValues: function () {
            var csvVal, view = this;
            csvVal = this.configuration.get('properties').get('attributes');
            this.collectionArray.reset();
            if (csvVal && csvVal !== '') {
                if (_.isArray(csvVal)) {
                    _.each(csvVal, function (item) {
                        view.addItem(item);
                    });
                } else {
                    _.each(csvVal.split(/[,]+/), function (item) {
                        view.addItem(item);
                    });
                }
            }
            this.setupPopOvers();
        },
        saveValues: function () {
            var values = [];
            _.each(this.collectionArray.models, function (model) {
                values.push(model.get('claimName') + "=" + model.get('claimValue'));
            });
            var errors = this.validate();
            if (!errors) {
                this.configuration.get('properties').set('attributes', values);
            }
        },
        validate: function () {
            var validation = [], warnings = [], idArray = [], nameMap = {};
            var claimName, claimValue;
            var immutableClaims = this.model.get("immutableClaims");
            if (this.collectionArray.length === 0) {
                warnings.push({
                    message: 'By removing all claims, anonymous access will effectively be disabled'
                });
            }
            _.each(this.collectionArray.models, function (model) {
                claimName = model.get('claimName');
                claimValue = model.get('claimValue');
                idArray.push(model.get('claimNameId'));
                idArray.push(model.get('claimValueId'));
                immutableClaims = _.without(immutableClaims, claimName);
                if (nameMap[claimName]) {
                    warnings.push({
                        message: 'Duplicate claim name ' + claimName
                    });
                } else if (claimName) {
                    nameMap[claimName] = model.get('claimNameId');
                }
                if (!claimName) {
                    validation.push({
                        message: 'Claim name is required',
                        name: model.get('claimNameId')
                    });
                }
                if (!claimValue) {
                    validation.push({
                        message: 'Claim value is required',
                        name: model.get('claimValueId')
                    });
                }
            });

            if (immutableClaims.length > 0) {
                warnings.push({
                    message: 'By removing required claims, anonymous access will effectively be disabled'
                });
            }

            if (warnings.length > 0) {
                this.configuration.set('validationWarnings', warnings);
            } else {
                this.configuration.unset('validationWarnings');
            }

            this.configuration.set('validatedFields', idArray);

            if (validation.length > 0) {
                this.configuration.set('validationErrors', validation);
                return validation;
            }
            this.configuration.unset('validationErrors');
        },
        onRender: function () {
            this.listItems.show(new AnonClaimsMultiValueCollection({
                collection: this.collectionArray
            }));

            this.updateValues();
            this.setupPopOvers();
        },
        addItem: function (value) {
            var claimName = value, claimValue = '';
            var parts = value.split(/=/);

            if (parts.length > 1) {
                claimName = parts[0];
                claimValue = parts[1];
            }

            var claims = this.model.get('availableClaims');
            this.collectionArray.add(new Backbone.Model({
                claimValue: claimValue,
                claimValueId: 'claimValue' + this.claimIdCounter,
                claimName: claimName,
                claimNameId: 'claimName' + this.claimIdCounter,
                availableClaims: claims,
                immutableClaims: this.model.get('immutableClaims')
            }));
            this.claimIdCounter++;
        },
        /**
         * Creates a new text field for the properties collection.
         */
        plusButton: function () {
            this.addItem('');
        },
        /**
         * Set up the popovers based on if the selector has a description.
         */
        setupPopOvers: function () {
            var view = this;

            var options, selector = ".claims-warning";
            options = {
                title: "Claims Warning",
                content: "This is a required claim attribute that should not be removed or edited",
                trigger: 'hover'
            };
            view.$(selector).popover(options);

        }
    });

    var AnonWarningModal = Modal.extend({
        template: 'anonWarningModal',
        onRender: function () {
            this.show();
        },
        onClose: function () {
            this.destroy();
        }
    });

    return AnonClaimsView;
});