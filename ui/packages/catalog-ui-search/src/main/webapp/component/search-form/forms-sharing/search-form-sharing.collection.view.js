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
 let Marionette = require('marionette');
 let _ = require('underscore');
 let $ = require('jquery');
 let SearchFormView = require('../search-form.view');
 let SearchFormSharingCollection = require('./search-form-sharing.collection');
 let CustomElements = require('js/CustomElements');

 module.exports = Marionette.CollectionView.extend({
     childView: SearchFormView,
     className: 'is-list is-inline has-list-highlighting',
     initialize: function(options) {
        var searchFormSharingCollection = new SearchFormSharingCollection();
        this.collection = searchFormSharingCollection.getCollection();
        this.searchFormSharingCollection = searchFormSharingCollection;
        this.options = options;
     },
     childViewOptions: function() {
        return {
            queryModel: this.options.model
        };
     },
 });