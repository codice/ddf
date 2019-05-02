const Marionette = require('marionette')
const _ = require('underscore')
const store = require('../../../js/store.js')
const GeometryView = require('./geometry.view')

const GeometryCollectionView = Marionette.CollectionView.extend({
  childView: GeometryView,
  childViewOptions: function() {
    return {
      map: this.options.map,
      selectionInterface: this.options.selectionInterface,
      clusterCollection: this.options.clusterCollection,
    }
  },
  initialize: function(options) {
    this.render = _.throttle(this.render, 200)
    this.options.map.onLeftClick(this.onMapLeftClick.bind(this))
    this.render()
  },
  onMapLeftClick: function(event, mapEvent) {
    if (
      mapEvent.mapTarget &&
      mapEvent.mapTarget !== 'userDrawing' &&
      !store.get('content').get('drawing')
    ) {
      if (event.shiftKey) {
        this.handleShiftClick(mapEvent.mapTarget)
      } else if (event.ctrlKey || event.metaKey) {
        this.handleCtrlClick(mapEvent.mapTarget)
      } else {
        this.handleClick(mapEvent.mapTarget)
      }
    }
  },
  handleClick: function(id) {
    if (id.constructor === String) {
      this.options.selectionInterface.clearSelectedResults()
      this.options.selectionInterface.addSelectedResult(this.collection.get(id))
    }
  },
  handleCtrlClick: function(id) {
    if (id.constructor === String) {
      this.options.selectionInterface.addSelectedResult(this.collection.get(id))
    }
  },
  handleShiftClick: function(id) {
    if (id.constructor === String) {
      this.options.selectionInterface.addSelectedResult(this.collection.get(id))
    }
  },
})

module.exports = GeometryCollectionView
