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

const Marionette = require('marionette');
const _ = require('underscore');
const $ = require('jquery');
const template = require('./query-custom.hbs');
const CustomElements = require('js/CustomElements');
const FilterBuilderView = require('component/filter-builder/filter-builder.view');
const FilterBuilderModel = require('component/filter-builder/filter-builder');
const cql = require('js/cql');
const store = require('js/store');
const QuerySettingsView = require('component/query-settings/query-settings.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('query-custom'),
    modelEvents: {
    },
    events: {
        'click .editor-edit': 'edit',
        'click .editor-cancel': 'cancel',
        'click .editor-save': 'save'
    },
    regions: {
        querySettings: '.query-settings',
        queryCustom: '.query-custom'
    },
    initialize: function() {
        this.$el.toggleClass('is-form-builder', this.options.isFormBuilder === true);
        this.$el.toggleClass('is-form', this.options.isForm === true);
    },
    onBeforeShow: function(){
        this.model = this.model._cloneOf ? store.getQueryById(this.model._cloneOf) : this.model;
        
        this.queryCustom.show(new FilterBuilderView({
            model: new FilterBuilderModel(),
            isForm: this.options.isForm || false,
            isFormBuilder: this.options.isFormBuilder || false
        }));
        this.querySettings.show(new QuerySettingsView({
            model: this.model
        }));

        if (this.options.isForm === true && this.model.get('filterTree') !== undefined) {
            this.setCqlFromFilter(this.model.get('filterTree').filterTemplate);
        }
        else if (this.model.get('cql')) {
            this.queryAdvanced.currentView.deserialize(cql.simplify(cql.read(this.model.get('cql'))));
        }
        this.queryAdvanced.currentView.turnOffEditing();
        this.edit();
    },
    edit: function(){
        this.$el.addClass('is-editing');
        this.querySettings.currentView.turnOnEditing();
        //if (this.options.isForm === true && this.options.isFormBuilder === true) {
        if (this.options.isFormBuilder === true) {
            this.queryCustom.currentView.turnOnEditing();
        }
    },
    cancel: function(){
        this.$el.removeClass('is-editing');
    },
    save: function(){
        this.$el.removeClass('is-editing');
        this.querySettings.currentView.saveToModel();

        this.model.set({
            cql: this.queryCustom.currentView.transformToCql(),
            filterTree: this.queryCustom.currentView.getFilters()
        });
    },
    setDefaultTitle: function() {
        this.model.set('title', 'New Form');
    },
    getFilterTree: function() {
        return this.queryAdvanced.currentView.getFilters();
    },
    setCqlFromFilter: function(filterTemplate) {
        this.queryAdvanced.currentView.model.set('operator', filterTemplate.type);
        this.queryAdvanced.currentView.setFilters(filterTemplate.filters);
        var filter = this.queryAdvanced.currentView.transformToCql();
        this.model.set({
            cql: filter
        });
    }
});
