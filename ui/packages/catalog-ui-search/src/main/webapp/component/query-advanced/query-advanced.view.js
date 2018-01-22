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
/*global define, setTimeout*/
define([
    'marionette',
    'underscore',
    'jquery',
    './query-advanced.hbs',
    'js/CustomElements',
    'component/filter-builder/filter-builder.view',
    'component/filter-builder/filter-builder',
    'js/cql',
    'js/store',
    'component/query-settings/query-settings.view'
], function (Marionette, _, $, template, CustomElements, FilterBuilderView, FilterBuilderModel, cql,
            store, QuerySettingsView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('query-advanced'),
        modelEvents: {
        },
        events: {
            'click .editor-edit': 'edit',
            'click .editor-cancel': 'cancel',
            'click .editor-save': 'save'
        },
        regions: {
            querySettings: '.query-settings',
            queryAdvanced: '.query-advanced'
        },
        ui: {
        },
        onBeforeShow: function(){
            this.model = this.model._cloneOf ? store.getQueryById(this.model._cloneOf) : this.model;
            this.querySettings.show(new QuerySettingsView({
                model: this.model
            }));
            this.queryAdvanced.show(new FilterBuilderView({
                model: new FilterBuilderModel()
            }));
            if (this.model.get('cql')) {
                this.queryAdvanced.currentView.deserialize(cql.simplify(cql.read(this.model.get('cql'))));
            }
            this.queryAdvanced.currentView.turnOffEditing();
            this.edit();
        },
        focus: function(){
            var tabbable =  _.filter(this.$el.find('[tabindex], input, button'), function(element){
                return element.offsetParent !== null;
            });
            if (tabbable.length > 0){
                $(tabbable[0]).focus();
            }
        },
        edit: function(){
            this.$el.addClass('is-editing');
            this.querySettings.currentView.turnOnEditing();
            this.queryAdvanced.currentView.turnOnEditing();
        },
        cancel: function(){
            this.$el.removeClass('is-editing');
            this.onBeforeShow();
        },
        save: function(){
            this.$el.removeClass('is-editing');
            this.querySettings.currentView.saveToModel();

            this.model.set({
                cql: this.queryAdvanced.currentView.transformToCql()
            });
        },
        setDefaultTitle: function(){
            this.model.set('title', this.model.get('cql'));
        }
    });
});
