const $ = require('jquery')
const DropdownCompanionView = require('../dropdown.companion.view')
const CustomElements = require('../../../js/CustomElements.js')

const namespace = CustomElements.getNamespace()

module.exports = DropdownCompanionView.extend({
  className: 'is-hint',
  listenForOutsideClick: function() {
    DropdownCompanionView.prototype.listenForOutsideClick.call(this)
    $(namespace + 'help').on(
      'mousedown.' + this.cid,
      function(event) {
        if (this.$el.find(event.target).length === 0) {
          this.close()
        }
      }.bind(this)
    )
  },
})
