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

const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./select.hbs')
const CustomElements = require('../../js/CustomElements.js')
const Common = require('../../js/Common.js')

module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('select'),
  className: function() {
    let className = ''
    if (this.model.get('hasNoValue')) {
      className += ' hasNoValue'
    }
    if (this.model.get('isThumbnail') && !this.model.get('hasNoValue')) {
      className += ' isThumbnail'
    }
    if (this.model.get('filterChoice') === true) {
      className += ' isFilterChoice'
    }
    return className
  },
  attributes: function() {
    return {
      'data-hits': this.model.get('hits'),
      'data-help': this.model.get('help'),
    }
  },
  onRender: function() {
    if (this.model.get('description')) {
      this.$el.attr('data-help', this.model.get('description'))
    }
  },
  serializeData: function() {
    const modelJSON = this.model.toJSON()
    if (modelJSON.label.constructor === Array) {
      modelJSON.label = modelJSON.label.join(' | ')
    }
    if (modelJSON.description) {
      // add line breaks to separate the description from the label
      // within the tooltip
      modelJSON.description = '\n\n' + modelJSON.description
    }
    if (modelJSON.isThumbnail && !modelJSON.hasNoValue) {
      modelJSON.img = Common.getImageSrc(modelJSON.value[0])
    }
    return modelJSON
  },
})
