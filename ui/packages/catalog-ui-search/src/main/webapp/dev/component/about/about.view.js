const Marionette = require('marionette')
const template = require('./about.hbs')
const CustomElements = require('../../../js/CustomElements.js')

module.exports = Marionette.LayoutView.extend({
  template,
  className: 'pad-bottom',
  tagName: CustomElements.register('dev-about'),
  regions: {
    tabs: '> .content',
  },
  onBeforeShow() {},
  serializeData() {},
})
