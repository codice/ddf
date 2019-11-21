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
    points: [],
    line: undefined,
  },
  /*
   * Sets the measurement state to the given new state.
   * Valid measurement states are:
   *   - NONE
   *   - START
   *   - END
   */
  changeMeasurementState(state) {
    // the current distance should be 0 when in the NONE or START state
    if (state === 'NONE' || state === 'START') {
      this.set({
        measurementState: state,
        currentDistance: 0,
      })
    } else {
      this.set({ measurementState: state })
    }
  },
  /*
   * Appends the given point to the array of points being tracked.
   */
  addPoint(point) {
    this.set({
      points: [...this.get('points'), point],
    })
  },
  /*
   * Sets the line to the given new line. This represents the line on the map
   * being used for the ruler measurement.
   */
  setLine(line) {
    this.set({ line })
  },
  /*
   * Resets the model's line and returns the old one.
   */
  removeLine() {
    const line = this.get('line')
    this.set({ line: undefined })

    return line
  },
  /*
   * Resets the model's array of points.
   */
  clearPoints() {
    this.set({ points: [] })
  },
  /*
   * Sets the current distance to the new given distance (in meters).
   */
  setCurrentDistance(distance) {
    this.set({ currentDistance: distance })
  },
  addDistanceInfo(distanceInfo) {
    this.set({distanceInfo})
  },
  setDistanceInfoPosition(left, bottom) {
    this.set({ distanceInfo: {left, bottom}})
  },
  removeDistanceInfo() {
    this.set({distanceInfo: undefined})
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
