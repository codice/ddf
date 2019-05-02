const ContentView = require('../content.view')
const uploadInstance = require('../../upload/upload.js')
const ResultSelectorView = require('../../result-selector/result-selector.view.js')
const VisualizationView = require('../../golden-layout/golden-layout.view.js')

module.exports = ContentView.extend({
  className: 'is-upload',
  selectionInterface: uploadInstance,
  initialize: function() {
    this._mapView = new VisualizationView({
      selectionInterface: uploadInstance,
      configName: 'goldenLayoutUpload',
    })
  },
  onFirstRender() {
    this.listenTo(
      uploadInstance,
      'change:currentUpload',
      this.updateContentLeft
    )
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
        model: uploadInstance.get('currentQuery'),
        selectionInterface: uploadInstance,
      })
    )
  },
  unselectQueriesAndResults: function() {
    uploadInstance.clearSelectedResults()
  },
})
