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
    'jquery',
    'text!templates/installer/configuration.handlebars',
    'text!templates/installer/configurationItem.handlebars',
    './Certificate.view.js',
    'modelbinder',
    'perfectscrollbar',
    'multiselect'
    ], function (Marionette, ich, _, Backbone, $, configurationTemplate, configurationItemTemplate, CertificateView) {

    ich.addTemplate('configurationTemplate', configurationTemplate);
    ich.addTemplate('configurationItemTemplate', configurationItemTemplate);


/*
* MODEL
*/
    var SystemProperty = Backbone.Model.extend({
        validate: function(attrs) {
            var validation = [];
            if (attrs.title.indexOf("Port") > -1) {
                var value = attrs.value;
                if (value && ! $.isNumeric(value)) {
                    validation.push({
                        message: 'Port must contain only digits.',
                        id: attrs.key
                    });
                }
            }

            if (validation.length > 0) {
                return validation;
            }

            // send validModel event to clear error message
            this.trigger('validModel', this);
        }
    });


/*
* COLLECTION
*/
    var SystemProperties = Backbone.Collection.extend({
        model: SystemProperty,
        url:'/jolokia/exec/org.codice.ddf.ui.admin.api:type=SystemPropertiesAdminMBean/readSystemProperties',
        saveUrl:'/jolokia/exec/org.codice.ddf.ui.admin.api:type=SystemPropertiesAdminMBean/writeSystemProperties',

        parse: function (response /*, options*/){
            // Return the value which will be the list of system property objects
            return response.value;
        },

        save: function() {

            var mbean = 'org.codice.ddf.ui.admin.api:type=SystemPropertiesAdminMBean';
            var operation = 'writeSystemProperties';

            var data = {
                type: 'EXEC',
                mbean: mbean,
                operation: operation
            };

            var propertiesMap = {};
            _.each(this.models, function(model){
                propertiesMap[model.get('key')] = model.get('value');
            });


            data.arguments = [propertiesMap];
            data = JSON.stringify(data);

            return $.ajax({
                type: 'POST',
                contentType: 'application/json',
                data: data,
                url: this.saveUrl
            });
        }
    });


    var systemProperties = new SystemProperties();
    systemProperties.fetch({});



/*
* Layout
*/
    var ConfigurationView = Marionette.Layout.extend({
        template: 'configurationTemplate',
        className: 'full-height',
        model: systemProperties,
        regions: {
            configurationItems: '#config-form',
            certificates: '#certificate-configuration'
        },

        initialize: function(options) {

            this.navigationModel = options.navigationModel;

            this.listenTo(this.navigationModel, 'previous', this.previous);
            this.listenTo(this.navigationModel,'next', this.next);

            this.listenTo(this.model, 'invalid', function(model, error){
                var layout = this;

                this.model.each(function(m){
                    if (model.get('key') === m.get('key')) {
                        layout.$("[name='" + m.get('key') + "Error']").hide();
                    }

                    _.each(error, function(errorItem){
                       if (m.get('key') === errorItem.id) {
                        layout.$("[name='" + m.get('key') + "Error']").show().html(errorItem.message);
                       }
                    });
                });
            });

            // clear error message on model validation
            this.listenTo(this.model, 'validModel', function(model) {
                var layout = this;

                this.model.each(function(m){
                    if (m.get('key') === model.get('key')) {
                        layout.$("[name='" + m.get('key') + "Error']").hide();
                    }
                });
            });

        },
        next: function() {
            var layout = this;

            // loop through models and check for hostname change, validation errors and set redirect url
            var hostChange = true;
            var hasErrors = false;

            this.model.each(function (model) {
                hasErrors = hasErrors || model.validationError;

                if(model.get('title') === 'Host') {
                    if(model.get('value') === model.get('defaultValue')){
                        hostChange = false;
                    }
                } else if(model.get('title') === 'HTTPS Port') {
                    layout.navigationModel.set('redirectUrl','https://localhost:'+model.get('value')+'/admin/index.html');
                }
            });


            if (! hasErrors) {

                var propertySave = this.model.save();
                if(propertySave){
                    propertySave.done(function(){
                        if(hostChange) {

                            var certSave = layout.certificates.currentView.saveCerts();
                            if (certSave) {

                                certSave.done(function () {
                                    if(layout.certificates.currentView.hasErrors()){
                                        layout.navigationModel.nextStep('Unable to save certificates. Check errors messages.', 0);
                                    } else {
                                        layout.navigationModel.nextStep('', 100);
                                    }
                                });

                                certSave.fail(function () {
                                    layout.navigationModel.nextStep('Unable to save certificates: check logs', 0);
                                });

                            } else {
                                layout.navigationModel.nextStep('Certificate validation failed. Check inputs', 0);
                            }

                        } else {
                            layout.navigationModel.nextStep('', 100);
                        }
                    });

                    propertySave.fail(function (){
                        layout.navigationModel.nextStep('Unable to Save Configuration: check logs', 0);
                    });

                } else {
                    layout.navigationModel.nextStep('System property validation failed. Check inputs.', 0);
                }
            } else {
                layout.navigationModel.nextStep('System property validation failed. Check inputs.', 0);
            }

        },
        previous: function () {
            this.navigationModel.previousStep();
        },
        onRender: function () {
            var view = this;

            var sysPropsView = new SystemPropertiesView({collection: this.model});
            var certificateView =  new CertificateView();

            this.configurationItems.show(sysPropsView);
            this.certificates.show(certificateView);

            _.defer(function() {
                view.$('#system-configuration-settings').perfectScrollbar({useKeyboard: false});
            });
        }
    });

/*
* Item View
*/
var SystemPropertyView = Marionette.ItemView.extend({
    className: 'well well-sm white',
    template: 'configurationItemTemplate',
    initialize: function () {
        this.modelBinder = new Backbone.ModelBinder();
    },
    onRender: function () {
        var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
        this.modelBinder.bind(this.model, this.el, bindings, {modelSetOptions: {validate: true}});
    }
});

/*
* Collection View
*/
var SystemPropertiesView = Marionette.CollectionView.extend({
    itemView: SystemPropertyView
});

    return ConfigurationView;
});