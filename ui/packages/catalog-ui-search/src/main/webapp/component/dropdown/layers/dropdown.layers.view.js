const DropdownView = require('../dropdown.view')
const template = require('./dropdown.layers.hbs')
const LayersView = require('../../layers/layers.view.js')
const user = require('../../singletons/user-instance.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-layers',
  componentToShow: LayersView,
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = user.get('user>preferences')
  },
})
