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
define([
    'marionette',
    'underscore',
    'jquery',
    'text!./metacard-advanced.hbs',
    'js/CustomElements',
    'js/store',
    'component/input/metacard/input-metacard.collection.view',
    'component/input/metacard/input-metacard.collection'
], function (Marionette, _, $, template, CustomElements, store, InputMetacardCollectionView,
             InputMetacardCollection) {

    return Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.getSelectedResults().first();
        },
        template: template,
        tagName: CustomElements.register('metacard-advanced'),
        modelEvents: {
        },
        events: {
            'click .metacardAdvanced-edit': 'edit',
            'click .metacardAdvanced-save': 'save',
            'click .metacardAdvanced-cancel': 'cancel'
        },
        regions: {
            metacardProperties: '.metacardAdvanced-properties'
        },
        initialize: function (options) {
            if (options.model === undefined){
                this.setDefaultModel();
            }
        },
        onBeforeShow: function(){
            this.metacardProperties.show(new InputMetacardCollectionView({
                collection: InputMetacardCollection.create(this.model)
            }));
            this.metacardProperties.currentView.$el.addClass("is-list");
            //this.metacardProperties.currentView.turnOnLimitedWidth();
        },
        edit: function(){
            this.$el.addClass('is-editing');
            this.metacardProperties.currentView.turnOnEditing();
            this.metacardProperties.currentView.focus();
        },
        cancel: function(){
            this.$el.removeClass('is-editing');
            this.metacardProperties.currentView.turnOffEditing();
            this.metacardProperties.currentView.revert();
        },
        save: function(){
            this.$el.removeClass('is-editing');
            this.metacardProperties.currentView.turnOffEditing();
            this.metacardProperties.currentView.save();
        }
    });
});
