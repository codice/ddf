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
 var _ = require('underscore');
 var $ = require('jquery');
 var Backbone = require('backbone');
 var SearchForm = require('../search-form');
 var Common = require('js/Common');

 let sharedTemplates = [];
 let templatePromise = $.ajax({
    type: 'GET',
    context: this,
    url: '/search/catalog/internal/forms/sharing',
    contentType: 'application/json',
    success: function (data) {
        sharedTemplates = data;
    }
});

 module.exports = Backbone.Collection.extend({
   model: SearchForm,
   initialize: function() {
       this.addMySharedForms();
   },
   addMySharedForms: function() {
       templatePromise.then(() => {
            if (!this.isDestroyed){
                $.each(sharedTemplates, (index, value) => {
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
                        accessGroups: value.accessGroups
                    }));
                });
                this.trigger("doneLoading");
            }
       });
   }
 });