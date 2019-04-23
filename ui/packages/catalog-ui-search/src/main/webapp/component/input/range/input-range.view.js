const template = require('./input-range.hbs')
const InputView = require('../input.view')

module.exports = InputView.extend({
  template: template,
  events: {
    'click .units-label': 'triggerFocus',
  },
  triggerFocus: function() {
    this.$el.find('input[type=number]').focus()
  },
  onRender: function() {
    this.listenToRange()
    this.listenToInput()
    InputView.prototype.onRender.call(this)
  },
  adjustValue: function(e) {
    let value = this.$el.find('input[type=number]').val()
    const max = this.model.get('property').get('max')
    const min = this.model.get('property').get('min')
    if (value > max) {
      value = max
      this.$el.find('input[type=number]').val(value)
    } else if (e.type === 'change' && value < min) {
      value = min
      this.$el.find('input[type=number]').val(value)
    }
    this.$el.find('.units-value').html(value)
    this.$el.find('input[type=range]').val(value)
  },
  listenToInput: function() {
    this.$el
      .find('input[type=number]')
      .off('change.range input.range')
      .on('change.range input.range', this.adjustValue.bind(this))
  },
  listenToRange: function() {
    this.$el
      .find('input[type=range]')
      .off('change.range input.range')
      .on(
        'change.range input.range',
        function(e) {
          const value = this.$el.find('input[type=range]').val()
          this.$el.find('input[type=number]').val(value)
          this.$el.find('.units-value').html(value)
        }.bind(this)
      )
  },
  listenForChange: function() {
    this.$el.on(
      'change keyup mouseup',
      function(e) {
        switch (e.target.type) {
          case 'range':
            if (e.type === 'mouseup' || e.type === 'keyup') {
              this.saveChanges()
            }
            break
          case 'number':
            if (e.type === 'change') {
              this.saveChanges()
            }
            break
        }
      }.bind(this)
    )
  },
  saveChanges: function() {
    let currentValue = this.$el.find('input[type=range]').val()
    currentValue = Math.min(
      Math.max(currentValue, this.model.get('property').get('min')),
      this.model.get('property').get('max')
    )
    this.model.set('value', currentValue)
  },
})
