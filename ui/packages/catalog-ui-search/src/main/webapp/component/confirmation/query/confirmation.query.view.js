const template = require('./confirmation.query.hbs')
const ConfirmationView = require('../confirmation.view')
const QuerySelectDropdown = require('../../dropdown/query-select/dropdown.query-select.view.js')
const DropdownModel = require('../../dropdown/dropdown.js')

module.exports = ConfirmationView.extend({
  template: template,
  className: 'is-query',
  modelEvents: {
    'change:choice': 'close',
  },
  events: {
    click: 'handleOutsideClick',
    'click .confirmation-no': 'handleNo',
    'click .confirmation-replace': 'handleReplace',
    'click .confirmation-new': 'handleNew',
    mousedown: 'handleMousedown',
  },
  regions: {
    querySelect: '.confirmation-query',
  },
  handleMousedown: function(e) {
    e.stopPropagation()
    this.querySelect.currentView.model.close()
  },
  onRender: function() {
    this.querySelect.show(
      new QuerySelectDropdown({
        model: new DropdownModel({
          value: undefined,
        }),
        hideActions: true,
        dropdownCompanionBehaviors: {
          navigation: {},
        },
      })
    )
    this.listenTo(
      this.querySelect.currentView.model,
      'change:value',
      this.handleValue
    )
    ConfirmationView.prototype.onRender.call(this)
    this.handleValue()
  },
  handleValue: function() {
    const value = this.getValue()
    this.$el.toggleClass('has-value', value !== undefined && value !== '')
  },
  getValue: function() {
    return this.querySelect.currentView.model.get('value')
  },
  handleNew: function() {
    this.model.makeChoice(true)
  },
  handleReplace: function() {
    this.model.makeChoice(this.getValue())
  },
})
