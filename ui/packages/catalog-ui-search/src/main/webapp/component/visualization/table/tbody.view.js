const Marionette = require('marionette');
const CustomElements = require('../../../js/CustomElements.js');
const RowView = require('./row.view');
require('../../../behaviors/selection.behavior.js')

module.exports = Marionette.CollectionView.extend({
  tagName: CustomElements.register('result-tbody'),
  className: 'is-tbody is-list has-list-highlighting',
  behaviors: function() {
    return {
      selection: {
        selectionInterface: this.options.selectionInterface,
        selectionSelector: `> *`,
      },
    }
  },
  childView: RowView,
  childViewOptions: function() {
    return {
      selectionInterface: this.options.selectionInterface,
    }
  },
  initialize: function(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
  },
})
