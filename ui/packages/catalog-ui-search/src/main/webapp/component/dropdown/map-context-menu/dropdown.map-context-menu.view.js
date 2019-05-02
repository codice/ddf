const DropdownView = require('../dropdown.view')
const template = require('./dropdown.map-context-menu.hbs')
const MapContextMenuView = require('../../map-context-menu/map-context-menu.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-mapContextMenu',
  componentToShow: MapContextMenuView,
})
