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

 let systemTemplates = [];
 let templatePromise = $.ajax({
    type: 'GET',
    context: this,
    url: '/search/catalog/internal/forms/query',
    contentType: 'application/json',
    success: function (data) {
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
        wreqr.vent.on("deleteTemplateById", this.deleteTemplateById);
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
                    var utcSeconds = value.created / 1000;
                    var d = new Date(0);
                    d.setUTCSeconds(utcSeconds);
                    this.addSearchForm(new SearchForm({createdOn: Common.getHumanReadableDate(d), id: value.id, name: value.title, type: 'custom', filterTemplate: value.filterTemplate}));
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
    doneLoading: function() {
        this.set('doneLoading', true);
    },
    deleteTemplateById: function(id) {
        systemTemplates = _.filter(systemTemplates, function(template) {
            return template.id !== id
       });
    }
 });