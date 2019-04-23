const ContentView = require('../content.view')
const alertInstance = require('../../alert/alert.js')
const ResultSelectorView = require('../../result-selector/result-selector.view.js')
const VisualizationView = require('../../golden-layout/golden-layout.view.js')

module.exports = ContentView.extend({
  className: 'is-alert',
  selectionInterface: alertInstance,
  initialize: function() {
    this._mapView = new VisualizationView({
      selectionInterface: alertInstance,
      configName: 'goldenLayoutAlert',
    })
  },
  onFirstRender() {
    this.listenTo(alertInstance, 'change:currentAlert', this.updateContentLeft)
  },
  onRender: function() {
    this.updateContentLeft()
    if (this._mapView) {
      this.contentRight.show(this._mapView)
    }
  },
  updateContentLeft: function() {
    this.contentLeft.show(
      new ResultSelectorView({
        model: alertInstance.get('currentQuery'),
        selectionInterface: alertInstance,
      })
    )
  },
  unselectQueriesAndResults: function() {
    alertInstance.clearSelectedResults()
  },
})
