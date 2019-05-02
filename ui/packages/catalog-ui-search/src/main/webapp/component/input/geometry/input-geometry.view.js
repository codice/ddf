const template = require('./input-geometry.hbs')
const InputView = require('../input.view')
const LocationView = require('../../location-new/location-new.view.js')

module.exports = InputView.extend({
  template: template,
  events: {
    'click .input-revert': 'revert',
  },
  regions: {
    locationRegion: '.location-region',
  },
  serializeData() {
    const value = this.model.get('value')
    return {
      label: value,
    }
  },
  onRender() {
    this.initializeRadio()
    InputView.prototype.onRender.call(this)
  },
  listenForChange() {
    this.listenTo(
      this.locationRegion.currentView.model,
      'change',
      this.triggerChange
    )
  },
  initializeRadio() {
    this.locationRegion.show(
      new LocationView({
        model: this.model,
      })
    )
  },
  handleReadOnly() {
    this.$el.toggleClass('is-readOnly', this.model.isReadOnly())
  },
  handleValue() {
    this.locationRegion.currentView.model.set('wkt', this.model.get('value'))
  },
  getCurrentValue() {
    return this.locationRegion.currentView.getCurrentValue()
  },
  isValid() {
    return this.locationRegion.currentView.isValid()
  },
  triggerChange() {
    this.model.set('value', this.getCurrentValue())
    this.model.set('isValid', this.isValid())
  },
})
