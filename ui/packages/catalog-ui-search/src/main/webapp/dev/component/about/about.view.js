const Marionette = require('marionette')
const template = require('./about.hbs')
const CustomElements = require('js/CustomElements')
const router = require('component/router/router')

module.exports = Marionette.LayoutView.extend({
  template: template,
  className: 'pad-bottom',
  tagName: CustomElements.register('dev-about'),
  regions: {
    tabs: '> .content',
  },
  onBeforeShow() {},
  serializeData() {},
})
