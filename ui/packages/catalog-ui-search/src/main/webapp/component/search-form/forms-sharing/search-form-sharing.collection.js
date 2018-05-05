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
 const _ = require('underscore');
 const $ = require('jquery');
 const Backbone = require('backbone');
 const SearchForm = require('../search-form');
 const Common = require('js/Common');
 const user = require('component/singletons/user-instance');

 let sharedTemplates = [];
 const templatePromise = $.ajax({
    type: 'GET',
    context: this,
    url: '/search/catalog/internal/forms/query',
    contentType: 'application/json',
    success: function (data) {
        sharedTemplates = data;
    }
});

 module.exports = Backbone.AssociatedModel.extend({
   model: SearchForm,
   defaults: {
        doneLoading: false,
        sharedSearchForms: []
   },
   initialize: function() {
       this.addMySharedForms();
   },
    relations: [{
        type: Backbone.Many,
        key: 'sharedSearchForms',
        collectionType: Backbone.Collection.extend({
            model: SearchForm,
            initialize: function() {}
        })
    }],
   addMySharedForms: function() {
       templatePromise.then(() => {
            if (!this.isDestroyed){
                $.each(sharedTemplates, (index, value) => {
                    if (this.checkIfShareable(value)) {
                        let utcSeconds = value.created / 1000;
                        let d = new Date(0);
                        d.setUTCSeconds(utcSeconds);
                        this.addSearchForm(new SearchForm({
                            createdOn: Common.getHumanReadableDate(d),
                            id: value.id,
                            name: value.title,
                            type: 'custom',
                            filterTemplate: JSON.stringify(value.filterTemplate),
                            accessIndividuals: value.accessIndividuals,
                            accessGroups: value.accessGroups,
                            createdBy: value.creator
                        }));
                    }
                });
                this.doneLoading();
            }
       });
   },
   checkIfShareable: function(template) {
       if (this.checkIfInGroup(template) || this.checkIfInIndividiuals(template)) {
           return true;
       }
       return false;
   },
   checkIfInGroup: function(template) {
       let myGroups = user.get('user').get('roles');
       let roleIntersection = myGroups.filter(function(n) {
        return template.accessGroups.indexOf(n) !== -1;
       });

       return !_.isEmpty(roleIntersection);
   },
   checkIfInIndividiuals: function(template) {
       let myEmail = [user.get('user').get('email')];
       let accessIndividualIntersection = myEmail.filter(function(n) {
        return template.accessIndividuals.indexOf(n) !== -1;
       });

       return !_.isEmpty(accessIndividualIntersection);
   },
    addSearchForm: function(searchForm) {
        this.get('sharedSearchForms').add(searchForm);
    },
    getDoneLoading: function() {
        return this.get('doneLoading');
    },
    doneLoading: function() {
        this.set('doneLoading', true);
    },
    getCollection: function() {
        return this.get('sharedSearchForms');
    },
 });