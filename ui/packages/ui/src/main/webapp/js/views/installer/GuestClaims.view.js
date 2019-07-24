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
  'backbone.marionette',
  'underscore',
  'backbone',
  'js/models/Service',
  'js/wreqr.js',
  'jquery',
  '../Modal',
  'templates/installer/guestClaims.handlebars',
  'templates/installer/guestClaimProfiles.handlebars',
  'templates/installer/guestClaimsListHeader.handlebars',
  'templates/installer/guestClaimsList.handlebars',
  'templates/installer/guestWarningModal.handlebars',
  'templates/installer/guestClaimsTable.handlebars',
], function(
  Marionette,
  _,
  Backbone,
  Service,
  wreqr,
  $,
  Modal,
  guestClaimsTemplate,
  guestClaimProfiles,
  guestClaimsListHeader,
  guestClaimsList,
  guestWarningModal,
  guestClaimsTable
) {
  var GuestClaimProfiles = Marionette.ItemView.extend({
    template: guestClaimProfiles,
    events: {
      'change .profile': 'updateValues',
    },
    initialize: function(options) {
      this.configuration = options.configuration
      this.model.set(
        'curProfile',
        this.configuration.get('properties').get('profile')
      )
    },
    updateValues: function(e) {
      var profileName = e.currentTarget[e.currentTarget.selectedIndex].label
      var profile = this.model.get('availableProfiles')[profileName]
      this.configuration.get('properties').set('profile', profileName)
      this.configuration.get('properties').set('attributes', profile)
      wreqr.vent.trigger('profileChanged')
    },
  })

  var GuestClaimsMultiValuedEntry = Marionette.ItemView.extend({
    template: guestClaimsList,
    tagName: 'tr',
    initialize: function() {
      this.modelBinder = new Backbone.ModelBinder()
      this.model.validate = this.validate
    },
    events: {
      'click .minus-button': 'minusButton',
      'click .available-claims': 'selectClaim',
    },
    modelEvents: {
      change: 'render',
    },
    minusButton: function() {
      this.model.collection.remove(this.model)
      this.checkValues()
    },
    onRender: function() {
      var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name')
      this.modelBinder.bind(this.model, this.$el, bindings)
      this.model.set(
        'showInfo',
        _.contains(
          this.model.get('immutableClaims'),
          this.model.get('claimName')
        )
      )
      this.checkValues()
      this.setupPopOvers()
    },
    onClose: function() {
      this.remove()
      this.unbind()
    },
    selectClaim: function(e) {
      var newValue = this.$(e.target).text()
      if (newValue === 'Add Custom Attribute...') {
        newValue = ''
      }
      this.$(e.target).addClass('selected-list-item')
      this.model.set('claimName', newValue)
      this.$('.claim-input').focus()
    },
    checkValues: function() {
      wreqr.vent.trigger('entriesChanged')
    },
    setupPopOvers: function() {
      var view = this

      var options,
        selector = '.claims-info'
      options = {
        title: 'Claims Info',
        content:
          'This is a required claim attribute that should not be removed or edited',
        trigger: 'hover',
      }
      view.$(selector).popover(options)
    },
  })

  var GuestClaimsMultiValueCollection = Marionette.CompositeView.extend({
    itemView: GuestClaimsMultiValuedEntry,
    template: guestClaimsTable,
    tagName: 'table',
    className: 'claim-table',
    appendHtml: function(collectionView, itemView) {
      collectionView.$('tbody').append(itemView.el)
    },
  })

  var GuestClaimsMultiValuedLayout = Marionette.Layout.extend({
    template: guestClaimsListHeader,
    tagName: 'div',
    regions: {
      listItems: '#listItems',
    },
    events: {
      'click .claim-add-attribute': 'plusButton',
    },
    modelEvents: {
      change: 'render',
    },
    initialize: function(options) {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.claimIdCounter = 0
      this.configuration = options.configuration
      this.collectionArray = new Backbone.Collection()
      this.listenTo(wreqr.vent, 'beforesave', this.saveValues)
      this.listenTo(wreqr.vent, 'profileChanged', this.updateValues)
      this.listenTo(wreqr.vent, 'entriesChanged', this.checkWarnings)
      this.updateValues()
    },
    updateValues: function() {
      var csvVal,
        view = this
      csvVal = this.configuration.get('properties').get('attributes')
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
      this.checkWarnings()
    },
    saveValues: function() {
      var values = []
      _.each(this.collectionArray.models, function(model) {
        values.push(model.get('claimName') + '=' + model.get('claimValue'))
      })
      var errors = this.validate()
      if (!errors) {
        this.configuration.get('properties').set('attributes', values)
      }
    },
    onRender: function() {
      this.listItems.show(
        new GuestClaimsMultiValueCollection({
          collection: this.collectionArray,
        })
      )
    },
    addItem: function(value) {
      var claimName = value,
        claimValue = ''
      var parts = value.split(/=/)

      if (parts.length > 1) {
        claimName = parts[0]
        claimValue = parts[1]
      }

      var claims = this.model.get('availableClaims')
      this.collectionArray.add(
        new Backbone.Model({
          claimValue: claimValue,
          claimValueId: 'claimValue' + this.claimIdCounter,
          claimName: claimName,
          claimNameId: 'claimName' + this.claimIdCounter,
          availableClaims: claims,
          immutableClaims: this.model.get('immutableClaims'),
        })
      )
      this.claimIdCounter++
    },
    checkWarnings: function() {
      var warnings = [],
        nameMap = {}
      var claimName, claimValue
      var immutableClaims = this.model.get('immutableClaims')

      _.each(this.collectionArray.models, function(model) {
        claimName = model.get('claimName')
        claimValue = model.get('claimValue')
        immutableClaims = _.without(immutableClaims, claimName)
        if (nameMap[claimName]) {
          warnings.push({
            message: 'Duplicate claim name ' + claimName,
          })
        } else if (claimName) {
          nameMap[claimName] = model.get('claimNameId')
        }
      })

      if (immutableClaims.length > 0) {
        _.each(immutableClaims, function(claim) {
          warnings.push({
            message:
              'By removing the required claim ' +
              claim +
              ', guest access will effectively be disabled',
          })
        })
      }
      this.model.set('warnings', warnings)
      if (warnings.length > 0) {
        this.configuration.set('validationWarnings', warnings)
        this.$('#warning-div').show()
      } else {
        this.configuration.unset('validationWarnings')
        this.$('#warning-div').hide()
      }
    },
    validate: function() {
      var validation = [],
        idArray = []
      var claimName, claimValue

      _.each(this.collectionArray.models, function(model) {
        claimName = model.get('claimName')
        claimValue = model.get('claimValue')
        idArray.push(model.get('claimNameId'))
        idArray.push(model.get('claimValueId'))

        if (!claimName) {
          validation.push({
            message: 'Claim name is required',
            name: model.get('claimNameId'),
          })
        }
        if (!claimValue) {
          validation.push({
            message: 'Claim value is required',
            name: model.get('claimValueId'),
          })
        }
      })

      this.checkWarnings()

      this.configuration.set('validatedFields', idArray)

      if (validation.length > 0) {
        this.configuration.set('validationErrors', validation)
        return validation
      }
      this.configuration.unset('validationErrors')
    },
    /**
     * Creates a new text field for the properties collection.
     */
    plusButton: function() {
      this.addItem('')
    },
  })

  var GuestWarningModal = Modal.extend({
    template: guestWarningModal,
    onRender: function() {
      this.show()
    },
    onClose: function() {
      this.destroy()
    },
  })

  var GuestClaimsView = Marionette.Layout.extend({
    template: guestClaimsTemplate,
    className: 'full-height',
    regions: {
      guestClaimProfiles: '#claims-profiles',
      guestClaimsItems: '#config-div',
      guestClaimsModal: '#warning-container',
    },
    events: {
      'click .claimsContinue': 'proceed',
      'click .claimsCancel': 'cancel',
    },
    initialize: function(options) {
      this.navigationModel = options.navigationModel
      this.navigationModel.set('hidePrevious', false)
      this.listenTo(this.navigationModel, 'next', this.next)
      this.listenTo(this.navigationModel, 'previous', this.previous)
      this.listenTo(wreqr.vent, 'showWarnings', this.verifyContinue)
      this.listenTo(wreqr.vent, 'saveClaimData', this.saveData)

      this.checkConfig()

      this.valObj = this.model.get('value').at(0)
      this.configObj = this.valObj.get('configurations').at(0)
      this.configObj.set('ignoreWarnings', false)

      //setup default profile if it doesn't exist
      if (!this.valObj.get('profiles').availableProfiles.Default) {
        this.valObj.get('profiles').profileNames = this.valObj
          .get('profiles')
          .profileNames.sort()
        this.valObj.get('profiles').profileNames.unshift('Default')
        this.valObj.get('profiles').availableProfiles.Default = this.valObj
          .get('metatype')
          .at(0)
          .get('defaultValue')
      }

      if (
        !_.contains(
          this.valObj.get('claims').availableClaims,
          'Add Custom Attribute...'
        )
      ) {
        this.valObj.get('claims').availableClaims = this.valObj
          .get('claims')
          .availableClaims.sort()
        this.valObj
          .get('claims')
          .availableClaims.push('Add Custom Attribute...')
      }
    },
    checkConfig: function() {
      if (
        this.model
          .get('value')
          .at(0)
          .get('configurations').length === 0
      ) {
        var configuration = new Service.Configuration()
        configuration.initializeFromService(this.model.get('value').at(0))
        configuration
          .get('properties')
          .set('service.pid', this.model.get('value').at(0).id)
        this.model
          .get('value')
          .at(0)
          .get('configurations')
          .add(configuration)
      }
    },
    onRender: function() {
      var view = this
      this.guestClaimProfiles.show(
        new GuestClaimProfiles({
          model: new Backbone.Model(this.valObj.get('profiles')),
          configuration: this.configObj,
        })
      )
      this.guestClaimsItems.show(
        new GuestClaimsMultiValuedLayout({
          model: new Backbone.Model(this.valObj.get('claims')),
          configuration: this.configObj,
        })
      )
    },
    onClose: function() {
      this.stopListening(this.navigationModel)
    },
    next: function() {
      var view = this
      this.configObj.set('ignoreWarnings', false)
      this.listenTo(this.configObj, 'invalid', function(model, errors) {
        this.configObj.get('validatedFields').forEach(function(fieldId) {
          view.$('[name=' + fieldId + 'Error]').hide()
        })
        errors.forEach(function(errorItem) {
          if (errorItem.name) {
            view
              .$('[name=' + errorItem.name + 'Error]')
              .show()
              .html(errorItem.message)
          }
        })
      })

      this.configObj.validate = this.validate
      this.submitData()

      //save the config
      var claims = this.configObj.attributes.properties.attributes
      if (
        typeof claims.profile !== 'undefined' &&
        claims.profile !== 'Default'
      ) {
        view.navigationModel.set('modified', true)
      } else {
        view.navigationModel.set('modified', false)
      }
      this.writeClaims(claims)
      this.saveData()
    },
    submitData: function() {
      wreqr.vent.trigger('beforesave')
      this.model.save()
    },
    saveData: function() {
      //save the config
      var view = this
      var saved = this.configObj.save()
      if (saved) {
        saved
          .done(function() {
            view.navigationModel.nextStep('', 100)
          })
          .fail(function() {
            view.navigationModel.nextStep(
              'Unable to Save Configuration: check logs',
              0
            )
          })
      }
    },
    writeClaims: function(attributes) {
      var data = {
        type: 'EXEC',
        mbean:
          'org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
        operation: 'updateGuestClaimsProfile',
        arguments: ['ddf.security.guest.realm', attributes],
      }
      $.ajax({
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        url:
          './jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/add',
      })
    },
    validate: function() {
      var errors = this.get('validationErrors')
      var warnings = this.get('validationWarnings')
      var ignoreWarnings = this.get('ignoreWarnings')
      var results = errors
      if (!errors && warnings && !ignoreWarnings) {
        wreqr.vent.trigger('showWarnings')
        results = warnings
      }
      return results
    },
    verifyContinue: function() {
      var modal = new GuestWarningModal({
        model: new Backbone.Model(this.configObj.get('validationWarnings')),
      })
      this.guestClaimsModal.show(modal)
    },
    proceed: function() {
      this.configObj.set('ignoreWarnings', true)
      this.$('#warning-container').on('hidden.bs.modal', function() {
        wreqr.vent.trigger('saveClaimData')
      })
    },
    previous: function() {
      this.navigationModel.previousStep('', 100)
    },
  })

  return GuestClaimsView
})
