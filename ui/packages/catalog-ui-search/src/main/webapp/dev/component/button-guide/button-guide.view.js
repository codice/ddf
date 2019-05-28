const template = require('./button-guide.hbs')
const CustomElements = require('../../../js/CustomElements.js')
const BaseGuideView = require('../base-guide/base-guide.view.js')

module.exports = BaseGuideView.extend({
  template,
  tagName: CustomElements.register('dev-button-guide'),
  styles: {
    button: require('!raw-loader!./button.less'),
  },
})
