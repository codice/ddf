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
    'js/view/ModalRegistry.view.js',
    'wreqr',
    'underscore',
    'jquery',
    'js/view/Utils.js',
    'js/model/Service.js',
    'text!templates/configModal.handlebars'
],
function (ich,Marionette,Backbone,ModalRegistry,wreqr,_,$,Utils,Service,modalConfig) {

    ich.addTemplate('modalConfig', modalConfig);

    var ModalConfig = {};

    ModalConfig.View = Marionette.Layout.extend({
        template: 'modalConfig',
        className: 'modal',
        events: {
            "click .submit-button": "submitData"
        },
        regions: {
            details: '.modal-details'
        },
        initialize: function(options){
            _.bindAll.apply(_, [this].concat(_.functions(this)));
            this.registry = options.registry;
            this.modelBinder = new Backbone.ModelBinder();
        },
        serializeData: function(){
            var data = {};

            if(this.model) {
                data = this.model.toJSON();
            }
            var configNames = [];
            var regConfigs = this.model.get('registryConfiguration');
            regConfigs.forEach(function(config){
                var service = config.get('service').get('name');
                if(configNames.indexOf(service) === -1){
                    configNames.push(service);
                }
            });
            data.configNames = configNames;
            return data;
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
        submitData: function (event) {
            this.closeAndUnbind();
            var configSelected = event.currentTarget.id;
            wreqr.vent.trigger("showModal",
                new ModalRegistry.View({
                    model: this.model,
                    registry: this.registry,
                    mode: 'add',
                    registryType: configSelected
                })
            );
        }
    });

    return ModalConfig;
});
