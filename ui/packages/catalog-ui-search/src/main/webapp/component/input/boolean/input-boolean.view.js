const template = require('./input-boolean.hbs')
const InputView = require('../input.view')

module.exports = InputView.extend({
  template: template,
  getCurrentValue: function() {
    return this.$el.find('input').is(':checked')
  },
  handleValue: function() {
    this.$el.find('input').attr('checked', Boolean(this.model.getValue()))
  },
  events: {
    'click label': 'triggerCheckboxClick',
  },
  triggerCheckboxClick: function() {
    this.$el.find('input').click()
  },
})
