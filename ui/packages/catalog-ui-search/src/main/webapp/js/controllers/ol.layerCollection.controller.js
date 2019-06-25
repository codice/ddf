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

const _ = require('underscore')
const ol = require('openlayers')
const properties = require('../properties.js')
const CommonLayerController = require('./common.layerCollection.controller.js')
const user = require('../../component/singletons/user-instance.js')

const createTile = (
  { show, alpha, ...options },
  Source,
  Layer = ol.layer.Tile
) =>
  new Layer({
    visible: show,
    preload: Infinity,
    opacity: alpha,
    source: new Source(options),
  })

const OSM = opts => {
  const { url } = opts
  return createTile(
    {
      ...opts,
      url: url + (url.indexOf('/{z}/{x}/{y}') === -1 ? '/{z}/{x}/{y}.png' : ''),
    },
    ol.source.OSM
  )
}

const BM = opts => {
  const imagerySet = opts.imagerySet || opts.url
  return createTile({ ...opts, imagerySet }, ol.source.BingMaps)
}

const WMS = opts => {
  const params = opts.params || {
    LAYERS: opts.layers,
    ...opts.parameters,
  }
  return createTile({ ...opts, params }, ol.source.TileWMS)
}

const WMT = async opts => {
  const { url } = opts
  const parser = new ol.format.WMTSCapabilities()

  const res = await window.fetch(url)
  const text = await res.text()
  const result = parser.read(text)

  if (result.Contents.Layer.length === 0) {
    throw new Error('WMTS map layer source has no layers.')
  }

  let { layer, matrixSet } = opts

  /* If tileMatrixSetID is present (Cesium WMTS keyword) set matrixSet (OpenLayers WMTS keyword) */
  if (opts.tileMatrixSetID !== undefined) {
    matrixSet = opts.tileMatrixSetID
  }

  if (layer === undefined) {
    layer = result.Contents.Layer[0].Identifier
  }

  const options = ol.source.WMTS.optionsFromCapabilities(result, {
    layer,
    matrixSet,
    ...opts,
  })

  if (options === null) {
    throw new Error('WMTS map layer source could not be setup.')
  }

  return createTile(opts, () => new ol.source.WMTS(options))
}

const AGM = opts => {
  // We strip the template part of the url because we will manually format
  // it in the `tileUrlFunction` function.
  const url = opts.url.replace('tile/{z}/{y}/{x}', '')

  // arcgis url format:
  //      http://<mapservice-url>/tile/<level>/<row>/<column>
  //
  // reference links:
  //  - https://openlayers.org/en/latest/examples/xyz-esri-4326-512.html
  //  - https://developers.arcgis.com/rest/services-reference/map-tile.htm
  const tileUrlFunction = tileCoord => {
    const [z, x, y] = tileCoord
    return `${url}/tile/${z - 1}/${-y - 1}/${x}`
  }

  return createTile({ ...opts, tileUrlFunction }, ol.source.XYZ)
}

const SI = opts => {
  const imageExtent =
    opts.imageExtent || ol.proj.get(properties.projection).getExtent()
  return createTile(
    { ...opts, imageExtent, ...opts.parameters },
    ol.source.ImageStatic,
    ol.layer.Image
  )
}

const sources = { OSM, BM, WMS, WMT, AGM, SI }

const createLayer = (type, opts) => {
  const fn = sources[type]

  if (fn === undefined) {
    throw new Error(`Unsupported map layer type ${type}`)
  }

  return fn(opts)
}

const Controller = CommonLayerController.extend({
  initialize() {
    // there is no automatic chaining of initialize.
    CommonLayerController.prototype.initialize.apply(this, arguments)
  },
  makeMap(options) {
    this.collection.forEach(model => {
      this.addLayer(model)
    })

    const view = new ol.View({
      projection: ol.proj.get(properties.projection),
      center: ol.proj.transform([0, 0], 'EPSG:4326', properties.projection),
      zoom: options.zoom,
    })

    const config = {
      target: options.element,
      view,
      interactions: ol.interaction.defaults({ doubleClickZoom: false }),
    }

    if (options.controls !== undefined) {
      config.controls = options.controls
    }

    this.map = new ol.Map(config)
    this.isMapCreated = true
    return this.map
  },
  onDestroy() {
    if (this.isMapCreated) {
      this.map.setTarget(null)
      this.map = null
    }
  },
  async addLayer(model) {
    const { id, type } = model.toJSON()
    const opts = _.omit(model.attributes, 'type', 'label', 'index', 'modelCid')
    opts.show = model.shouldShowLayer()

    try {
      const layer = await Promise.resolve(createLayer(type, opts))
      this.map.addLayer(layer)
      this.layerForCid[id] = layer
      this.reIndexLayers()
    } catch (e) {
      model.set('warning', e.message)
    }
  },
  removeLayer(model) {
    const id = model.get('id')
    const layer = this.layerForCid[id]
    if (layer !== undefined) {
      this.map.removeLayer(layer)
    }
    delete this.layerForCid[id]
    this.reIndexLayers()
  },
  setAlpha(model) {
    const layer = this.layerForCid[model.id]
    if (layer !== undefined) {
      layer.setOpacity(model.get('alpha'))
    }
  },
  setShow(model) {
    const layer = this.layerForCid[model.id]
    if (layer !== undefined) {
      layer.setVisible(model.shouldShowLayer())
    }
  },
  reIndexLayers() {
    this.collection.forEach(function(model, index) {
      const layer = this.layerForCid[model.id]
      if (layer !== undefined) {
        layer.setZIndex(-(index + 1))
      }
    }, this)
    user.savePreferences()
  },
})

module.exports = Controller
