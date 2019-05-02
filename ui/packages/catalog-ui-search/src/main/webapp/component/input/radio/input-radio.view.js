const template = require('./input-radio.hbs')
const InputView = require('../input.view')
const RadioView = require('../../radio/radio.view.js')

module.exports = InputView.extend({
  template: template,
  events: {
    'click .input-revert': 'revert',
  },
  regions: {
    radioRegion: '.radio-region',
  },
  listenForChange: function() {
    this.$el.on(
      'click',
      function() {
        this.model.set('value', this.getCurrentValue())
      }.bind(this)
    )
  },
  serializeData: function() {
    const value = this.model.get('value')
    const choice = this.model
      .get('property')
      .get('radio')
      .filter(function(choice) {
        return (
          JSON.stringify(choice.value) === JSON.stringify(value) ||
          JSON.stringify(choice) === JSON.stringify(value)
        )
      })[0]
    return {
      label: choice ? choice.label : value,
    }
  },
  onRender: function() {
    this.initializeRadio()
    InputView.prototype.onRender.call(this)
  },
  initializeRadio: function() {
    this.radioRegion.show(
      RadioView.createRadio({
        options: this.model
          .get('property')
          .get('radio')
          .map(function(value) {
            if (value.label) {
              return {
                label: value.label,
                value: value.value,
                title: value.title,
              }
            } else {
              return {
                label: value,
                value: value,
                title: value.title,
              }
            }
          }),
        defaultValue: [this.model.get('value')],
      })
    )
  },
  handleReadOnly: function() {
    this.$el.toggleClass('is-readOnly', this.model.isReadOnly())
  },
  handleValue: function() {
    this.radioRegion.currentView.model.set('value', this.model.get('value'))
  },
  getCurrentValue: function() {
    const currentValue = this.radioRegion.currentView.model.get('value')
    return currentValue
  },
})
