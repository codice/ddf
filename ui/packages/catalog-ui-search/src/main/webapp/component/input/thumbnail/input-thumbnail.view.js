const _ = require('underscore')
const $ = require('jquery')
const template = require('./input-thumbnail.hbs')
const InputView = require('../input.view')
const announcement = require('../../announcement/index.jsx')
const Common = require('../../../js/Common.js')

function handleError() {
  announcement.announce({
    title: 'Image Upload Failed',
    message:
      'There was an issue loading your image.  Please try again or recheck what you are attempting to upload.',
    type: 'error',
  })
  this.model.revert()
  this.render()
}

module.exports = InputView.extend({
  template: template,
  events: {
    'click button': 'upload',
    'change input': 'handleUpload',
  },
  listenForChange: $.noop,
  serializeData: function() {
    return _.extend(this.model.toJSON(), { cid: this.cid })
  },
  handleUpload: function(e) {
    const self = this
    const img = this.$el.find('img')[0]
    const reader = new FileReader()
    reader.onload = function(event) {
      img.onload = function() {
        self.model.set('value', self.getCurrentValue())
        self.handleEmpty()
        self.resizeButton()
      }
      img.onerror = handleError.bind(self)
      img.src = event.target.result
    }
    reader.onerror = handleError.bind(self)
    reader.readAsDataURL(e.target.files[0])
  },
  handleValue: function() {
    const self = this
    const img = this.$el.find('img')[0]
    const lnk = this.$el.find('a')
    img.onload = function() {
      self.resizeButton()
    }
    if (this.model.getValue() && this.model.getValue().constructor === String) {
      img.src = Common.getImageSrc(this.model.getValue())
      lnk.attr('href', Common.getResourceUrlFromThumbUrl(img.src))
    }
    this.handleEmpty()
  },
  resizeButton: function() {
    this.$el.find('button').css('height', this.el.querySelector('img').height)
  },
  focus: function() {
    this.$el.find('input').select()
  },
  handleEdit: function() {
    this.$el.toggleClass('is-editing', this.model.isEditing())
  },
  handleEmpty: function() {
    if (
      !(this.model.getValue() && this.model.getValue().constructor === String)
    ) {
      this.$el.toggleClass('is-empty', true)
    } else {
      this.$el.toggleClass('is-empty', false)
    }
  },
  upload: function() {
    this.$el.find('input').click()
  },
  getCurrentValue: function() {
    const img = this.el.querySelector('img')
    return img.src.split(',')[1]
  },
  listenForResize: function() {
    $(window)
      .off('resize.' + this.cid)
      .on(
        'resize.' + this.cid,
        _.throttle(
          function(event) {
            this.resizeButton()
          }.bind(this),
          16
        )
      )
  },
  stopListeningForResize: function() {
    $(window).off('resize.' + this.cid)
  },
  onRender: function() {
    InputView.prototype.onRender.call(this)
    this.listenForResize()
  },
  onDestroy: function() {
    this.stopListeningForResize()
  },
})
