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
/* global setTimeout */
const SearchFormViews = require('component/search-form/search-form.view')
const properties = require('properties')
const lightboxResultInstance = require('component/lightbox/result/lightbox.result.view');
const lightboxInstance = lightboxResultInstance.generateNewLightbox();
const QueryResult = properties.hasExperimentalEnabled() ? require('component/result-form/result-form.view') : {}
const SearchFormModel = require('component/search-form/search-form.js')
const CustomElements = require('js/CustomElements')

module.exports = SearchFormViews.extend({
  initialize: function () {
    SearchFormViews.prototype.initialize.call(this)
  },
  changeView: function () {
    if (properties.hasExperimentalEnabled()) {
      this.triggerCloseDropdown();
      lightboxInstance.model.updateTitle(this.model.get('type') === 'new-result' ? '' : this.model.get('name'));
      lightboxInstance.model.open();
      lightboxInstance.lightboxContent.show(new QueryResult({
        model: this.model.get('type') === 'new-result' ? new SearchFormModel({name: ''}) : this.model,
      }));
    }
  },
  triggerCloseDropdown: function() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
    this.options.queryModel.trigger('closeDropdown');
  }
})
