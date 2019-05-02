const template = require('./input-location.hbs')
const InputView = require('../input.view')
const LocationView = require('../../location-old/location-old.view.js')

module.exports = InputView.extend({
  template: template,
  events: {
    'click .input-revert': 'revert',
  },
  regions: {
    locationRegion: '.location-region',
  },
  serializeData: function() {
    const value = this.model.get('value')
    return {
      label: value,
    }
  },
  onRender: function() {
    this.initializeRadio()
    InputView.prototype.onRender.call(this)
  },
  listenForChange: function() {
    this.listenTo(
      this.locationRegion.currentView.model,
      'change',
      this.triggerChange
    )
  },
  isValid: function() {
    return this.locationRegion.currentView.isValid()
  },
  initializeRadio: function() {
    this.locationRegion.show(
      new LocationView({
        model: this.model,
      })
    )
  },
  handleReadOnly: function() {
    this.$el.toggleClass('is-readOnly', this.model.isReadOnly())
  },
  handleValue: function() {
    this.locationRegion.currentView.model.set('value', this.model.get('value'))
  },
  getCurrentValue: function() {
    return this.locationRegion.currentView.getCurrentValue()
  },
  triggerChange: function() {
    this.model.set('value', this.getCurrentValue())
  },
})
