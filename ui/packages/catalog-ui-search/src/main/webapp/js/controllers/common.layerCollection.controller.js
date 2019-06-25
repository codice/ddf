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

/*jshint newcap: false, bitwise: false */

const Marionette = require('marionette')

const Controller = Marionette.Object.extend({
  initialize(options) {
    this.collection = options.collection
    this.layerForCid = {}

    this.listenTo(this.collection, 'change:alpha', this.setAlpha)
    this.listenTo(this.collection, 'change:show change:alpha', this.setShow)

    // subclasses must implement reIndexLayers()
    this.listenTo(this.collection, 'sort', this.reIndexLayers)
    this.listenTo(this.collection, 'add', this.addLayer)
    this.listenTo(this.collection, 'remove', this.removeLayer)
  },
})

module.exports = Controller
