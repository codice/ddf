const _ = require('underscore')
const template = require('./attributes-rearrange.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance.js')
const properties = require('../../js/properties.js')
const Sortable = require('sortablejs')
const metacardDefinitions = require('../singletons/metacard-definitions.js')

function calculateAvailableAttributesFromSelection(selectionInterface) {
  const types = _.union.apply(
    this,
    selectionInterface.getSelectedResults().map(result => {
      return [result.get('metacardType')]
    })
  )
  const possibleAttributes = _.intersection.apply(
    this,
    types.map(type => {
      return Object.keys(metacardDefinitions.metacardDefinitions[type])
    })
  )
  return selectionInterface
    .getSelectedResults()
    .reduce(function(currentAvailable, result) {
      currentAvailable = _.union(
        currentAvailable,
        Object.keys(
          result
            .get('metacard')
            .get('properties')
            .toJSON()
        )
      )
      return currentAvailable
    }, [])
    .filter(attribute => possibleAttributes.indexOf(attribute) >= 0)
    .filter(function(property) {
      if (metacardDefinitions.metacardTypes[property]) {
        return !metacardDefinitions.metacardTypes[property].hidden
      } else {
        announcement.announce({
          title: 'Missing Attribute Definition',
          message:
            'Could not find information for ' +
            property +
            ' in definitions.  If this problem persists, contact your Administrator.',
          type: 'warn',
        })
        return false
      }
    })
    .sort((a, b) => metacardDefinitions.attributeComparator(a, b))
}

module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('attributes-rearrange'),
  initialize: function(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
    this.listenTo(
      this.options.selectionInterface,
      'reset:activeSearchResults add:activeSearchResults',
      this.render
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:inspector-summaryShown',
      this.render
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:inspector-detailsHidden',
      this.render
    )
  },
  getShown: function() {
    if (this.options.summary) {
      const usersChoice = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      if (usersChoice.length > 0) {
        return usersChoice
      } else {
        return properties.summaryShow
      }
    } else {
      return calculateAvailableAttributesFromSelection(
        this.options.selectionInterface
      )
    }
  },
  getHidden: function() {
    if (this.options.summary) {
      const usersChoice = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      if (usersChoice.length > 0) {
        return calculateAvailableAttributesFromSelection(
          this.options.selectionInterface
        ).filter(attr => usersChoice.indexOf(attr) === -1)
      } else {
        return calculateAvailableAttributesFromSelection(
          this.options.selectionInterface
        ).filter(attr => properties.summaryShow.indexOf(attr) === -1)
      }
    } else {
      return user
        .get('user')
        .get('preferences')
        .get('inspector-detailsHidden')
    }
  },
  getPreferredOrder: function() {
    if (this.options.summary) {
      const usersShown = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      const usersOrder = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryOrder')
      if (usersOrder.length > 0) {
        return usersOrder
      } else {
        return properties.summaryShow
      }
    } else {
      return user
        .get('user')
        .get('preferences')
        .get('inspector-detailsOrder')
    }
  },
  getNewAttributes: function() {
    if (this.options.summary) {
      const usersShown = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      const usersOrder = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryOrder')
      if (usersShown.length > 0 || usersOrder.length > 0) {
        return usersShown.filter(function(attr) {
          return usersOrder.indexOf(attr) === -1
        })
      } else {
        return []
      }
    } else {
      const detailsOrder = user
        .get('user')
        .get('preferences')
        .get('inspector-detailsOrder')
      return calculateAvailableAttributesFromSelection(
        this.options.selectionInterface
      ).filter(function(attr) {
        return detailsOrder.indexOf(attr) === -1
      })
    }
  },
  serializeData: function() {
    const preferredHeader = this.getPreferredOrder()
    const newAttributes = this.getNewAttributes()
    newAttributes.sort(function(a, b) {
      return metacardDefinitions.attributeComparator(a, b)
    })
    const hidden = this.getHidden()
    const availableAttributes = calculateAvailableAttributesFromSelection(
      this.options.selectionInterface
    )

    return _.union(preferredHeader, newAttributes).map(function(property) {
      return {
        label: properties.attributeAliases[property],
        id: property,
        hidden: hidden.indexOf(property) >= 0,
        notCurrentlyAvailable:
          availableAttributes.indexOf(property) === -1 ||
          properties.isHidden(property) ||
          metacardDefinitions.isHiddenTypeExceptThumbnail(property),
      }
    })
  },
  onRender: function() {
    Sortable.create(this.el, {
      onEnd: () => {
        this.handleSave()
      },
    })
  },
  handleSave: function() {
    const prefs = user.get('user').get('preferences')
    const key = this.options.summary
      ? 'inspector-summaryOrder'
      : 'inspector-detailsOrder'
    prefs.set(
      key,
      _.map(this.$el.find('.column'), function(element) {
        return element.getAttribute('data-propertyid')
      })
    )
    prefs.savePreferences()
  },
})
