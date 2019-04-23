const $ = require('jquery')
const _ = require('underscore')
const template = require('./table-visibility.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const user = require('../../singletons/user-instance.js')
const properties = require('../../../js/properties.js')
const metacardDefinitions = require('../../singletons/metacard-definitions.js')

module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('table-visibility'),
  events: {
    'click .column': 'toggleVisibility',
    'click .footer-cancel': 'destroy',
    'click .footer-save': 'handleSave',
  },
  initialize: function(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
    this.listenTo(
      this.options.selectionInterface,
      'reset:activeSearchResults add:activeSearchResults',
      this.render
    )
  },
  serializeData: function() {
    const prefs = user.get('user').get('preferences')
    const results = this.options.selectionInterface
      .getActiveSearchResults()
      .toJSON()
    const preferredHeader = user
      .get('user')
      .get('preferences')
      .get('columnOrder')
    const hiddenColumns = user
      .get('user')
      .get('preferences')
      .get('columnHide')
    const availableAttributes = this.options.selectionInterface.getActiveSearchResultsAttributes()

    return preferredHeader.map(function(property) {
      return {
        label: properties.attributeAliases[property],
        id: property,
        hidden: hiddenColumns.indexOf(property) >= 0,
        notCurrentlyAvailable:
          availableAttributes.indexOf(property) === -1 ||
          properties.isHidden(property) ||
          metacardDefinitions.isHiddenTypeExceptThumbnail(property),
      }
    })
  },
  toggleVisibility: function(e) {
    $(e.currentTarget).toggleClass('is-hidden-column')
  },
  onRender: function() {},
  handleSave: function() {
    const prefs = user.get('user').get('preferences')
    prefs.set(
      'columnHide',
      _.map(this.$el.find('.is-hidden-column'), function(element) {
        return element.getAttribute('data-propertyid')
      })
    )
    prefs.savePreferences()
    this.destroy()
  },
})
