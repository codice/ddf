const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const template = require('./visualization.hbs')
const OpenlayersView = require('./maps/openlayers/openlayers.view.js')
const CombinedMapView = require('./combined-map/combined-map.view.js')
const HistogramView = require('./histogram/histogram.view.js')
const TableView = require('./table/table-viz.view.js')
const user = require('../singletons/user-instance.js')

function getActiveVisualization() {
  return user
    .get('user')
    .get('preferences')
    .get('visualization')
}

function getPreferences() {
  return user.get('user').get('preferences')
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('visualization'),
  template: template,
  regions: {
    activeVisualization: '.visualization-container',
  },
  events: {},
  initialize: function() {
    this.listenTo(getPreferences(), 'change:visualization', this.onBeforeShow)
  },
  onBeforeShow: function() {
    switch (getActiveVisualization()) {
      case '2dmap':
        this.showOpenlayers()
        break
      case '3dmap':
        this.showCesium()
        break
      case 'histogram':
        this.showHistogram()
        break
      case 'table':
        this.showTable()
        break
    }
  },
  showOpenlayers: function() {
    this.activeVisualization.show(
      new OpenlayersView({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  showCesium: function() {
    this.activeVisualization.show(
      new CombinedMapView({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  showHistogram: function() {
    this.activeVisualization.show(
      new HistogramView({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  showTable: function() {
    this.activeVisualization.show(
      new TableView({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
})
