/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

/** Main view page for add. */
define([
  'marionette',
  'icanhaz',
  'underscore',
  'backbone',
  'wreqr',
  'templates/configuration/configurationEdit.handlebars',
  'templates/configuration/configurationItem.handlebars',
  'templates/configuration/textTypeListHeader.handlebars',
  'templates/configuration/textTypeList.handlebars',
  'templates/configuration/checkboxType.handlebars',
], function(
  Marionette,
  ich,
  _,
  Backbone,
  wreqr,
  configurationEdit,
  configurationItem,
  textTypeListHeader,
  textTypeList,
  checkboxTypeTemplate
) {
  var ConfigurationEditView = {}

  if (!ich['configuration.configurationItem']) {
    ich.addTemplate('configuration.configurationItem', configurationItem)
  }
  if (!ich['configuration.configurationEdit']) {
    ich.addTemplate('configuration.configurationEdit', configurationEdit)
  }
  if (!ich['configuration.textTypeListHeader']) {
    ich.addTemplate('configuration.textTypeListHeader', textTypeListHeader)
  }
  if (!ich['configuration.textTypeList']) {
    ich.addTemplate('configuration.textTypeList', textTypeList)
  }
  if (!ich['configuration.checkboxTypeTemplate']) {
    ich.addPartial('configuration.checkboxTypeTemplate', checkboxTypeTemplate)
  }

  ConfigurationEditView.ConfigurationMultiValuedEntry = Marionette.ItemView.extend(
    {
      template: 'configuration.textTypeList',
      tagName: 'tr',
      initialize: function() {
        this.modelBinder = new Backbone.ModelBinder()
      },
      events: {
        'click .minus-button': 'minusButton',
      },
      minusButton: function() {
        this.model.collection.remove(this.model)
      },
      onRender: function() {
        var bindings = Backbone.ModelBinder.createDefaultBindings(
          this.el,
          'name'
        )
        this.modelBinder.bind(this.model, this.$el, bindings)
      },
      onClose: function() {
        this.modelBinder.unbind()
      },
    }
  )

  ConfigurationEditView.ConfigurationMultiValueCollection = Marionette.CollectionView.extend(
    {
      itemView: ConfigurationEditView.ConfigurationMultiValuedEntry,
      tagName: 'table',
    }
  )

  ConfigurationEditView.ConfigurationMultiValuedItem = Marionette.Layout.extend(
    {
      template: 'configuration.textTypeListHeader',
      itemView: ConfigurationEditView.ConfigurationMultiValueCollection,
      tagName: 'div',
      regions: {
        listItems: '#listItems',
      },
      /**
       * Button events, right now there's a submit button
       * I do not know where to go with the cancel button.
       */
      events: {
        'click .plus-button': 'plusButton',
      },
      modelEvents: {
        change: 'updateValues',
      },
      initialize: function(options) {
        _.bindAll.apply(_, [this].concat(_.functions(this)))
        this.configuration = options.configuration
        this.collectionArray = new Backbone.Collection()
        this.listenTo(wreqr.vent, 'refresh', this.updateValues)
        this.listenTo(wreqr.vent, 'beforesave', this.saveValues)
      },
      updateValues: function() {
        var csvVal,
          view = this
        if (
          this.configuration.get('properties') &&
          this.configuration.get('properties').get(this.model.get('id'))
        ) {
          csvVal = this.configuration
            .get('properties')
            .get(this.model.get('id'))
        } else {
          csvVal = this.model.get('defaultValue')
        }
        this.collectionArray.reset()
        if (csvVal && csvVal !== '') {
          if (_.isArray(csvVal)) {
            _.each(csvVal, function(item) {
              view.addItem(item)
            })
          } else {
            _.each(csvVal.split(/[,]+/), function(item) {
              view.addItem(item)
            })
          }
        }
      },
      saveValues: function() {
        var values = []
        this.collectionArray.models.forEach(function(model) {
          values.push(model.get('value'))
        })
        if (this.configuration.get('configurations')) {
          this.configuration
            .get('configurations')
            .models[0].get('properties')
            .set(this.model.get('id'), values)
        } else {
          this.configuration.get('properties').set(this.model.get('id'), values)
        }
      },
      onRender: function() {
        this.listItems.show(
          new ConfigurationEditView.ConfigurationMultiValueCollection({
            collection: this.collectionArray,
          })
        )

        this.updateValues()
      },
      addItem: function(value) {
        this.collectionArray.add(
          new Backbone.Model({
            value: value,
          })
        )
      },
      /**
       * Creates a new text field for the properties collection.
       */
      plusButton: function() {
        this.addItem('')
      },
    }
  )

  ConfigurationEditView.ConfigurationItem = Marionette.ItemView.extend({
    template: 'configuration.configurationItem',
    events: {
      'change input': 'updateModel',
      'change select': 'updateModel',
    },
    /**
     * Save all values properties into the sourceModal
     */
    serializeData: function() {
      var modelJSON = this.model.toJSON()
      var value = this.options.configuration
        .get('properties')
        .get(this.model.get('id'))

      if (modelJSON.type === 11) {
        if (value !== undefined) {
          if (Array.isArray(value)) {
            value = value[0] === 'true'
          } else if (typeof value === 'string') {
            value = value === 'true'
          }
        } else {
          value = this.model.get('defaultValue')[0] === 'true'
        }
      } else {
        value = value || this.model.get('defaultValue')
      }

      return _.extend(modelJSON, {
        defaultValue: value,
        options: modelJSON.optionLabels.map(function(optionLabel, index) {
          return {
            label: optionLabel,
            value: modelJSON.optionValues[index],
            selected: modelJSON.optionValues[index] === value,
          }
        }),
      })
    },
    updateModel: function(e) {
      var config = this.options.configuration
      config.get('properties').set(this.model.get('id'), e.target.value)
    },
  })

  ConfigurationEditView.ConfigurationCollection = Marionette.CollectionView.extend(
    {
      itemView: ConfigurationEditView.ConfigurationItem,
      initialize: function(options) {
        this.service = options.service
        this.listenTo(wreqr.vent, 'poller:start', this.render)
      },
      onRender: function() {
        this.setupPopOvers()
      },
      buildItemView: function(item, ItemViewType, itemViewOptions) {
        var view
        var configuration = this.options.configuration
        this.collection.forEach(function(property) {
          if (item.get('id') === property.id) {
            if (property.description) {
              item.set({
                description: property.description,
              })
            }
            if (property.note) {
              item.set({
                note: property.note,
              })
            }
            var options = _.extend(
              {
                model: item,
                configuration: configuration,
              },
              itemViewOptions
            )

            view = new ItemViewType(options)
          }
        })
        if (view) {
          return view
        }
        return new Marionette.ItemView()
      },
      /**
       * Set up the popovers based on if the selector has a description.
       */
      setupPopOvers: function() {
        var view = this
        this.service.get('metatype').forEach(function(each) {
          if (!_.isUndefined(each.get('description'))) {
            var options,
              selector = ".description[data-title='" + each.id + "']"
            options = {
              title: each.get('name'),
              content: each.get('description'),
              trigger: 'hover',
            }
            view.$(selector).popover(options)
          }
        })
      },
      getItemView: function(item) {
        if (item.get('cardinality') > 0 || item.get('cardinality') < 0) {
          return ConfigurationEditView.ConfigurationMultiValuedItem
        } else {
          return ConfigurationEditView.ConfigurationItem
        }
      },
    }
  )
  return ConfigurationEditView
})
