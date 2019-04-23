const Marionette = require('marionette')
const template = require('./upload-menu.hbs')
const CustomElements = require('../../js/CustomElements.js')
const uploadInstance = require('../upload/upload.js')
const Common = require('../../js/Common.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('upload-menu'),
  onFirstRender: function() {
    this.listenTo(uploadInstance, 'change:currentUpload', this.render)
  },
  serializeData: function() {
    if (uploadInstance.get('currentUpload') === undefined) {
      return {}
    }
    return {
      when: Common.getMomentDate(
        uploadInstance.get('currentUpload').get('sentAt')
      ),
    }
  },
})
