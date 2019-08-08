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
class MockMap {
  testData = null
  constructor() {
    this.testData = {
      layerCount: 0,
      interactions: {},
      interactionsCount: 0,
      layers: [],
      eventListeners: {
        pointerdrag: new Set(),
        mousemove: new Set(),
      },
    }
  }
  addInteraction(i) {
    this.testData.interactions[i] = i
    this.testData.interactionsCount++
  }
  removeInteraction(i) {
    this.testData.interactions[i]
    this.testData.interactionsCount--
  }
  addLayer(layer: any) {
    this.testData.layerCount++
    this.testData.layers.push(layer)
  }
  getTestData() {
    return this.testData
  }
  on(event: string, listener: any) {
    this.testData.eventListeners[event].add(listener)
  }
  un(event: string, listener: any) {
    this.testData.eventListeners[event].delete(listener)
  }
}

export default MockMap
