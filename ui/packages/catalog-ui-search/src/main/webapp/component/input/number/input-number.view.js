const template = require('./input-number.hbs');
const InputView = require('../input.view');

module.exports = InputView.extend({
  template: template,
  getCurrentValue: function() {
    const value = this.$el.find('input').val();
    if (value !== '') {
      return Number(value)
    } else {
      return value
    }
  },
  isValid: function() {
    return this.getCurrentValue() !== ''
  },
})
