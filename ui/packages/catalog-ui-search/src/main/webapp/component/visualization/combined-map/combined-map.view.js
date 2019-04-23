const template = require('./combined-map.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const CesiumView = require('../maps/cesium/cesium.view.js')
const OpenlayersView = require('../maps/openlayers/openlayers.view.js')
const featureDetection = require('../../singletons/feature-detection.js')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('combined-map'),
  template: template,
  regions: {
    mapContainer: '> .map-container',
  },
  onRender: function() {
    this.listenToOnce(featureDetection, 'change:cesium', this.render)
    if (featureDetection.supportsFeature('cesium')) {
      this.mapContainer.show(new CesiumView(this.options))
    } else {
      this.mapContainer.show(new OpenlayersView(this.options))
    }
  },
})
