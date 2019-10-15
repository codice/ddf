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

import wrapNum from '../../../react-component/utils/wrap-num/wrap-num.tsx'

const Backbone = require('backbone')
const MetacardModel = require('../../../js/model/Metacard.js')
const mtgeo = require('mt-geo')
const usngs = require('usng.js')
const converter = new usngs.Converter()
const usngPrecision = 6

module.exports = Backbone.AssociatedModel.extend({
  relations: [
    {
      type: Backbone.One,
      key: 'targetMetacard',
      relatedModel: MetacardModel,
      isTransient: true,
    },
  ],
  defaults: {
    mouseLat: undefined,
    mouseLon: undefined,
    coordinateValues: {
      dms: '',
      lat: '',
      lon: '',
      mgrs: '',
      utmUps: '',
    },
    target: undefined,
    targetMetacard: undefined,
    measurementState: 'NONE',
    currentDistance: 0,
    billboards: [],
    linePrimitive: undefined,
  },
  /*
   * Sets the measurement state to the given new state.
   * Valid measurement states are:
   *   - NONE
   *   - START
   *   - END
   */
  changeMeasurementState(state) {
    this.set({ measurementState: state })
  },
  /*
   * Appends the given Billboard to the array of Billboards being tracked.
   */
  addBillboard(billboard) {
    this.set({
      billboards: [...this.get('billboards'), billboard],
    })
  },
  /*
   * Sets the line Primitive to the given new line Primitive. This represents the line on the map
   * being used for the ruler measurement.
   */
  setLinePrimitive(primitive) {
    this.set({ linePrimitive: primitive })
  },
  /*
   * Resets the model's line Primitive and returns the old one.
   */
  removeLinePrimitive() {
    const linePrimitive = this.get('linePrimitive')
    this.set({ linePrimitive: undefined })

    return linePrimitive
  },
  /*
   * Resets the model's array of Billboards.
   */
  clearBillboards() {
    this.set({ billboards: [] })
  },
  /*
   * Sets the current distance to the new given distance (in meters).
   */
  setCurrentDistance(distance) {
    this.set({ currentDistance: distance })
  },
  isOffMap() {
    return this.get('mouseLat') === undefined
  },
  clearMouseCoordinates() {
    this.set({
      mouseLat: undefined,
      mouseLon: undefined,
    })
  },
  updateMouseCoordinates(coordinates) {
    this.set({
      mouseLat: Number(coordinates.lat.toFixed(6)), // wrap in Number to chop off trailing zero
      mouseLon: Number(wrapNum(coordinates.lon, -180, 180).toFixed(6)),
    })
  },
  updateClickCoordinates() {
    const lat = this.get('mouseLat')
    const lon = this.get('mouseLon')
    const dms = `${mtgeo.toLat(lat)} ${mtgeo.toLon(lon)}`
    const mgrs = converter.isInUPSSpace(lat)
      ? undefined
      : converter.LLtoUSNG(lat, lon, usngPrecision)
    const utmUps = converter.LLtoUTMUPS(lat, lon)
    this.set({
      coordinateValues: {
        lat,
        lon,
        dms,
        mgrs,
        utmUps,
      },
    })
  },
})
