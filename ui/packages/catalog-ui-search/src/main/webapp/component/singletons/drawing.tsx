const Backbone = require('backbone')
const wreqr = require('../../js/wreqr.js')
const $ = require('jquery')

type DrawingType = Backbone.Model & {
  turnOnDrawing: (model: Backbone.Model) => void
  turnOffDrawing: () => void
  isDrawing: () => boolean
  getDrawModel: () => Backbone.Model
}

export const Drawing = new (Backbone.Model.extend({
  defaults: {
    drawing: false,
    drawingModel: undefined,
  },
  initialize() {
    this.listenTo(wreqr.vent, 'search:drawline', this.turnOnDrawing)
    this.listenTo(wreqr.vent, 'search:drawcircle', this.turnOnDrawing)
    this.listenTo(wreqr.vent, 'search:drawpoly', this.turnOnDrawing)
    this.listenTo(wreqr.vent, 'search:drawbbox', this.turnOnDrawing)
    this.listenTo(wreqr.vent, 'search:drawstop', this.turnOffDrawing)
    this.listenTo(wreqr.vent, 'search:drawend', this.turnOffDrawing)
  },
  turnOnDrawing(model: Backbone.Model) {
    this.set('drawing', true)
    this.set('drawingModel', model)
    $('html').toggleClass('is-drawing', true)
  },
  turnOffDrawing() {
    this.set('drawing', false)
    $('html').toggleClass('is-drawing', false)
  },
  getDrawModel() {
    return this.get('drawingModel')
  },
  isDrawing() {
    return this.get('drawing')
  },
}))() as DrawingType
