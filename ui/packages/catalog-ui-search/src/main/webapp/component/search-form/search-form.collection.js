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
 var wreqr = require('wreqr');
 var _ = require('underscore');
 var $ = require('jquery');
 var Backbone = require('backbone');
 var SearchForm = require('./search-form');
 var Common = require('js/Common');
 let user = require('component/singletons/user-instance');

 const fixFilter = function(filter) {
    if (filter.filters) {
        filter.filters.forEach(fixFilter);
    } else {
        filter.defaultValue = filter.defaultValue || '';
        filter.value = filter.value || filter.defaultValue;
    }
 }

 const fixTemplates = function(templates) {
    templates.forEach((template) => {
        return fixFilter(template.filterTemplate);
    });
 };
 
 let systemTemplates = [];
 let templatePromise = $.ajax({
    type: 'GET',
    context: this,
    url: '/search/catalog/internal/forms/query',
    contentType: 'application/json',
    success: function (data) {
        fixTemplates(data);
        systemTemplates = data;
    }
 });

module.exports = Backbone.AssociatedModel.extend({
    defaults: {
        doneLoading: false,
        searchForms: []
    },
    initialize: function () {
        this.addSearchForm(new SearchForm({type: 'basic'}));
        this.addSearchForm(new SearchForm({type: 'text'}));
        this.addCustomForms();
        wreqr.vent.on('deleteTemplateById', this.deleteTemplateById);
    },
    relations: [{
        type: Backbone.Many,
        key: 'searchForms',
        collectionType: Backbone.Collection.extend({
            model: SearchForm,
            initialize: function() {

            }
        })
    }],
    addCustomForms: function() {
        templatePromise.then(() => {
            if (!this.isDestroyed) {
                $.each(systemTemplates, (index, value) => {
                    // if (this.checkIfOwnerOrSystem(value)) {
                        var utcSeconds = value.created / 1000;
                        var d = new Date(0);
                        d.setUTCSeconds(utcSeconds);
                        this.addSearchForm(new SearchForm({
                            createdOn: Common.getHumanReadableDate(d),
                            id: value.id,
                            name: value.title,
                            type: 'custom',
                            filterTemplate: value.filterTemplate,
                            accessIndividuals: value.accessIndividuals,
                            accessGroups: value.accessGroups,
                            createdBy: value.creator
                        }));
                    // }
                });
                this.doneLoading();
            }
        });
    },
    getCollection: function() {
        return this.get('searchForms');
    },
    addSearchForm: function(searchForm) {
        this.get('searchForms').add(searchForm);
    },
    getDoneLoading: function() {
        return this.get('doneLoading');
    },
    checkIfOwnerOrSystem: function(template) {
        let myEmail = user.get('user').get('email');
        let templateCreator = template.creator;
        return myEmail === templateCreator || templateCreator === "System Template";
    },
    doneLoading: function() {
        this.set('doneLoading', true);
    },
    deleteTemplateById: function(id) {
        systemTemplates = _.filter(systemTemplates, function(template) {
            return template.id !== id
       });
    }
 });