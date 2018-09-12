/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
const { reactToMarionette } = require('component/transmute')
const LocationInput = reactToMarionette(require('./location'))

if (process.env.NODE_ENV !== 'production') {
  module.hot.accept('./location', () => {
    LocationInput.reload(require('./location'))
  })
}

const Marionette = require('marionette')
const _ = require('underscore')
const CustomElements = require('js/CustomElements')
const LocationNewModel = require('./location-new')

module.exports = Marionette.LayoutView.extend({
  template: () => `<div class="location-input"></div>`,
  tagName: CustomElements.register('location-new'),
  regions: {
    location: '.location-input',
  },
  initialize(options) {
    this.propertyModel = this.model
    this.model = new LocationNewModel()
    _.bindAll.apply(_, [this].concat(_.functions(this))) // underscore bindAll does not take array arg
  },
  onRender() {
    this.location.show(
      new LocationInput({
        model: this.model,
      })
    )
  },
  getCurrentValue() {
    return this.model.getValue()
  },
  isValid() {
    return this.model.isValid()
  },
})
