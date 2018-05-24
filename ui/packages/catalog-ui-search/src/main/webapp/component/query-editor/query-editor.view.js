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
/*global require*/
const Marionette = require('marionette');
const template = require('./query-editor.hbs');
const CustomElements = require('js/CustomElements');
const QueryBasic = require('component/query-basic/query-basic.view');
const QueryAdvanced = require('component/query-advanced/query-advanced.view');
const QueryTitle = require('component/query-title/query-title.view');
const QueryAdhoc = require('component/query-adhoc/query-adhoc.view');
const cql = require('js/cql');
const CQLUtils = require('js/CQLUtils');
const store = require('js/store');
const user = require('component/singletons/user-instance');

function isNested(filter) {
    var nested = false;
    filter.filters.forEach(function (subfilter) {
        nested = nested || subfilter.filters;
    });
    return nested;
}

function isTypeLimiter(filter) {
    var typesFound = {};
    filter.filters.forEach(function (subfilter) {
        typesFound[CQLUtils.getProperty(subfilter)] = true;
    });
    typesFound = Object.keys(typesFound);
    return (typesFound.length === 2) && (typesFound.indexOf('metadata-content-type') >= 0) &&
        (typesFound.indexOf('datatype') >= 0);
}

function isAnyDate(filter) {
    var propertiesToCheck = ['created', 'modified', 'effective', 'metacard.created', 'metacard.modified'];
    var typesFound = {};
    var valuesFound = {};
    if (filter.filters.length === propertiesToCheck.length) {
        filter.filters.forEach(function (subfilter) {
            typesFound[subfilter.type] = true;
            valuesFound[subfilter.value] = true;
            var indexOfType = propertiesToCheck.indexOf(CQLUtils.getProperty(subfilter));
            if (indexOfType >= 0) {
                propertiesToCheck.splice(indexOfType, 1);
            }
        });
        return propertiesToCheck.length === 0 && Object.keys(typesFound).length === 1 && Object.keys(valuesFound).length === 1;
    }
    return false;
}

function translateFilterToBasicMap(filter) {
    var propertyValueMap = {};
    var downConversion = false;
    if (filter.filters) {
        filter.filters.forEach(function (filter) {
            if (!filter.filters) {
                propertyValueMap[CQLUtils.getProperty(filter)] = propertyValueMap[CQLUtils.getProperty(filter)] || [];
                if (propertyValueMap[CQLUtils.getProperty(filter)].filter(function (existingFilter) {
                        return existingFilter.type === filter.type;
                    }).length === 0) {
                    propertyValueMap[CQLUtils.getProperty(filter)].push(filter);
                }
            } else if (!isNested(filter) && isAnyDate(filter)) {
                propertyValueMap['anyDate'] = propertyValueMap['anyDate'] || [];
                if (propertyValueMap['anyDate'].filter(function (existingFilter) {
                        return existingFilter.type === filter.filters[0].type;
                    }).length === 0) {
                    propertyValueMap['anyDate'].push(filter.filters[0]);
                }
            } else if (!isNested(filter) && isTypeLimiter(filter)) {
                propertyValueMap[CQLUtils.getProperty(filter.filters[0])] = propertyValueMap[CQLUtils.getProperty(filter.filters[0])] || [];
                filter.filters.forEach(function (subfilter) {
                    propertyValueMap[CQLUtils.getProperty(filter.filters[0])].push(subfilter);
                });
            } else {
                downConversion = true;
            }
        });
    } else {
        propertyValueMap[CQLUtils.getProperty(filter)] = propertyValueMap[CQLUtils.getProperty(filter)] || [];
        propertyValueMap[CQLUtils.getProperty(filter)].push(filter);
    }
    return {
        propertyValueMap: propertyValueMap,
        downConversion: downConversion
    };
}

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('query-editor'),
    regions: {
        queryContent: '> .editor-content > .content-form',
        queryTitle: '> .editor-content > .content-title'
    },
    originalType: '',
    events: {
        'click .editor-edit': 'edit',
        'click .editor-cancel': 'cancel',
        'click .editor-save': 'save',
        'click .editor-saveRun': 'saveRun'
    },
    initialize: function () {
        this.model = this.model._cloneOf ? store.getQueryById(this.model._cloneOf) : this.model;        
        this.listenTo(this.model, 'resetToDefaults change:type', this.reshow);
        this.listenTo(this.model, 'revert', this.revert);
        this.originalType = this.model.get('type');
    },
    revert: function() {
        if (this.model.get('type') !== this.originalType) {
            this.model.set('type', this.originalType);
        } else {
            this.reshow();
        }
    },
    reshow: function() {
        switch (this.model.get('type')) {
            case 'text':
                this.showText();
                break;
            case 'basic':
                this.showBasic();
                break;
            case 'advanced':
                this.showAdvanced();
                break;
            case 'custom':
                this.showCustom();
                break;
        }
        this.edit();
    },
    onBeforeShow: function () {
        this.reshow();
        this.showTitle();
    },
    showTitle: function() {
        this.queryTitle.show(new QueryTitle({
            model: this.model
        }));
    },
    showText: function () {
        this.queryContent.show(new QueryAdhoc({
            model: this.model
        }));
    },
    showBasic: function () {
        this.queryContent.show(new QueryBasic({
            model: this.model
        }));
    },
    showCustom: function () {
        this.queryContent.show(new QueryAdvanced({
            model: this.model,
            isForm: true,
            isFormBuilder: false
        }));
    },
    handleEditOnShow: function(){
        if (this.$el.hasClass('is-editing')){
            this.edit();
        }
    },
    showAdvanced: function () {
        this.queryContent.show(new QueryAdvanced({
            model: this.model
        }));
    },
    edit: function(){
        this.$el.addClass('is-editing');
        this.queryContent.currentView.edit();
    },
    cancel: function(){
        this.$el.removeClass('is-editing');
        this.onBeforeShow();
    },
    save: function(){
        this.queryContent.currentView.save();
        this.queryTitle.currentView.save();
        if (store.getCurrentQueries().get(this.model) === undefined){
            store.getCurrentQueries().add(this.model);
        }
        this.cancel();
        this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        this.originalType = this.model.get('type');
    },
    saveRun: function(){
        this.queryContent.currentView.save();
        this.queryTitle.currentView.save();
        if (store.getCurrentQueries().get(this.model) === undefined){
            store.getCurrentQueries().add(this.model);
        }
        this.cancel();
        this.model.startSearch();
        store.setCurrentQuery(this.model);
        this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        this.originalType = this.model.get('type');
    }
});