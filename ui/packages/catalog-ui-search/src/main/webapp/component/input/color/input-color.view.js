const _ = require('underscore')
const $ = require('jquery')
const template = require('./input-color.hbs')
const InputView = require('../input.view')
require('spectrum-colorpicker')

function validTextColour(stringToTest) {
  if (
    [null, undefined, '', 'inherit', 'transparent'].indexOf(stringToTest) !== -1
  ) {
    return false
  }

  const image = document.createElement('img');
  image.style.color = 'rgb(0, 0, 0)'
  image.style.color = stringToTest
  if (image.style.color !== 'rgb(0, 0, 0)') {
    return true
  }
  image.style.color = 'rgb(255, 255, 255)'
  image.style.color = stringToTest
  return image.style.color !== 'rgb(255, 255, 255)'
}

function removeAlpha(color) {
  const $input = $(document.createElement('input'));
  const hexString = $input
    .spectrum()
    .spectrum('set', color)
    .spectrum('get')
    .toHexString();
  $input.spectrum('destroy')
  return hexString
}

function sanitizeIncomingValue(color) {
  if (!validTextColour(color)) {
    return 'white' // default color
  } else {
    return removeAlpha(color) // remove transparency
  }
}

module.exports = InputView.extend({
  template: template,
  events: {},
  serializeData: function() {
    return _.extend(this.model.toJSON(), {
      cid: this.cid,
    })
  },
  onRender: function() {
    this.initializeColorpicker()
    InputView.prototype.onRender.call(this)
  },
  initializeColorpicker: function() {
    this.$el.find('input').spectrum({
      showInput: true,
      showInitial: true,
    })
    this.$el.find('.sp-replacer').addClass('is-input')
    this.$el
      .find('.sp-dd')
      .replaceWith(
        '<button class="is-primary"><span class="fa fa-caret-down"></span></button>'
      )
  },
  handleReadOnly: function() {
    this.$el.toggleClass('is-readOnly', this.model.isReadOnly())
  },
  handleValue: function() {
    this.$el
      .find('input')
      .spectrum('set', sanitizeIncomingValue(this.model.getValue()))
  },
  focus: function() {
    this.$el.find('input').select()
  },
  getCurrentValue: function() {
    const currentValue = this.$el
      .find('input')
      .spectrum('get')
      .toHexString();
    if (currentValue) {
      return currentValue
    } else {
      return 'white'
    }
  },
  listenForChange: function() {
    this.$el.on(
      'input change keyup',
      function() {
        this.model.set('value', this.getCurrentValue())
        this.handleValue()
      }.bind(this)
    )
  },
  onDestroy: function() {
    const colorpicker = this.$el.find('input');
    if (colorpicker) {
      colorpicker.spectrum('destroy')
    }
  },
})
