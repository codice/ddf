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

define([
  'icanhaz',
  'marionette',
  'underscore',
  'jquery',
  'q',
  'js/view/ModalConfig.view.js',
  'js/view/ModalRegistry.view.js',
  'js/view/EmptyView.js',
  'wreqr',
  'js/view/Utils.js',
  'text!templates/deleteRegistryModal.handlebars',
  'text!templates/deleteRegistry.handlebars',
  'text!templates/registryPage.handlebars',
  'text!templates/registryList.handlebars',
  'text!templates/registryRow.handlebars',
  'poller',
  'js/model/RemoteStatus.js',
  'js/model/Status.js',
], function(
  ich,
  Marionette,
  _,
  $,
  Q,
  ModalConfig,
  ModalRegistry,
  EmptyView,
  wreqr,
  Utils,
  deleteRegistryModal,
  deleteRegistry,
  registryPage,
  registryList,
  registryRow,
  poller,
  RemoteStatus,
  Status
) {
  var RegistryView = {}

  ich.addTemplate('deleteRegistryModal', deleteRegistryModal)
  ich.addTemplate('deleteRegistry', deleteRegistry)
  ich.addTemplate('registryPage', registryPage)
  ich.addTemplate('registryRow', registryRow)
  ich.addTemplate('registryList', registryList)

  RegistryView.RegistryRow = Marionette.Layout.extend({
    template: 'registryRow',
    tagName: 'tr',
    className: 'highlight-on-hover',
    regions: {
      editModal: '.modal-container',
    },
    events: {
      'click td': 'editRegistry',
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.listenTo(this.model, 'change', this.render)
      if (this.model.get('registryConfiguration').at(0)) {
        this.setupPollers(this.model.get('registryConfiguration').at(0))
      }
    },
    editRegistry: function(evt) {
      evt.stopPropagation()
      var service = this.model
      wreqr.vent.trigger('editRegistry', service)
    },
    updateStatus: function() {
      this.model.set('available', this.statusModel.get('value'))
      this.render()
    },
    //function to called on every RegistryRow when the refresh button is pressed
    updateRemoteStatus: function() {
      if (this.remoteStatusModel.get('value')) {
        this.model.set('id', this.remoteStatusModel.get('value').id)
        this.render()
      }
    },
    setupPollers: function(config) {
      var pid = config.id
      var view = this
      this.statusModel = new Status.Model(pid)
      this.remoteStatusModel = new RemoteStatus.Model(pid)
      this.listenTo(this.statusModel, 'sync', this.updateStatus)
      this.listenTo(this.remoteStatusModel, 'sync', this.updateRemoteStatus)

      var options = {
        delay: 30000,
      }

      this.statusPoller = poller.get(view.statusModel, options)
      this.remoteStatusPoller = poller.get(view.remoteStatusModel, options)
      this.statusPoller.start()
      this.remoteStatusPoller.start()
    },
    onClose: function() {
      if (this.statusPoller) {
        this.statusPoller.stop()
        this.remoteStatusPoller.stop()
      }
    },
  })

  RegistryView.DeleteItem = Marionette.ItemView.extend({
    template: 'deleteRegistry',
  })

  RegistryView.DeleteModal = Marionette.CompositeView.extend({
    template: 'deleteRegistryModal',
    className: 'modal',
    itemView: RegistryView.DeleteItem,
    itemViewContainer: '.modal-body',
    events: {
      'click .submit-button': 'deleteRegistries',
    },
    deleteRegistries: function() {
      var view = this
      var toDelete = []
      view.collection.each(function(item) {
        var currentConfig = item.get('registryConfiguration').at(0)
        view.$('.selectRegistryDelete').each(function(index, content) {
          if (content.checked) {
            var id = item ? item.id : null
            if (id === content.value) {
              toDelete.push(view.model.createDeletePromise(item, currentConfig))
            }
          }
        })
      })

      //remove registry
      if (toDelete.length > 0) {
        //remove all selected configurations from the current item
        Q.all(toDelete)
          .then(function(results) {
            _.each(results, function(result) {
              var item = result.registry
              if (item.size() <= 0) {
                //if no type configurations, delete the entire source.
                view.model.get('collection').removeRegistry(item)
                view.model
                  .get('model')
                  .get('value')
                  .remove(item)
              }
            })
            wreqr.vent.trigger('refreshRegistries')
            view.$el.modal('hide')
          })
          .done()
      }
    },
  })

  RegistryView.RegistryTable = Marionette.CompositeView.extend({
    template: 'registryList',
    itemView: RegistryView.RegistryRow,
    emptyView: EmptyView.registries,
    itemViewContainer: 'tbody',
  })

  RegistryView.RegistryPage = Marionette.Layout.extend({
    template: 'registryPage',
    events: {
      'click .removeRegistryLink': 'removeRegistry',
      'click .addRegistryLink': 'addRegistry',
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.listenTo(wreqr.vent, 'editRegistry', this.editRegistry)
      this.listenTo(wreqr.vent, 'refreshRegistries', this.refreshRegistries)
      new RegistryView.ModalController({
        application: this,
      })
    },
    regions: {
      collectionRegion: '#registriesRegion',
      registryModal: '#registries-modal',
    },
    onShow: function() {
      this.refreshButton = Utils.refreshButton(
        '.refreshButton',
        this.refreshRegistries
      )
    },
    onDestroy: function() {
      this.refreshButton.close()
    },
    onRender: function() {
      var collection = this.model.get('collection')
      this.collectionRegion.show(
        new RegistryView.RegistryTable({
          model: this.model,
          collection: collection,
        })
      )
    },
    refreshRegistries: function() {
      var view = this
      view.model.get('model').clear()

      view.model.get('model').fetch({
        success: function() {
          view.model.get('collection').sort()
          view.model.get('collection').trigger('reset')
          view.refreshButton.done()
        },
      })
    },
    addRegistry: function() {
      var self = this.model
      if (self) {
        var regConfigs = self.getRegistryModel().get('registryConfiguration')
        //If there are more than 1 type of registry configuration available (i.e. CSW, DNA, etc)
        if (regConfigs.size() > 1) {
          wreqr.vent.trigger(
            'showModal',
            new ModalConfig.View({
              model: self.getRegistryModel(),
              registry: self,
            })
          )
        } else {
          wreqr.vent.trigger(
            'showModal',
            new ModalRegistry.View({
              model: self.getRegistryModel(),
              registry: self,
              mode: 'add',
              registryType: self
                .getRegistryModel()
                .get('registryConfiguration')
                .models[0].get('name'),
            })
          )
        }
      }
    },
    removeRegistry: function() {
      if (this.model) {
        wreqr.vent.trigger(
          'showModal',
          new RegistryView.DeleteModal({
            model: this.model,
            collection: this.model.get('collection'),
          })
        )
      }
    },
    editRegistry: function(service) {
      var self = this.model
      var startOfType = service.id.lastIndexOf('(')
      var strLength = service.id.length
      var registryType = service.id.substring(startOfType + 1, strLength - 1)
      wreqr.vent.trigger(
        'showModal',
        new ModalRegistry.View({
          model: self.getRegistryModel(service),
          registry: self,
          mode: 'edit',
          registryType: registryType,
        })
      )
    },
  })

  RegistryView.ModalController = Marionette.Controller.extend({
    initialize: function(options) {
      this.application = options.application
      this.listenTo(wreqr.vent, 'showModal', this.showModal)
    },
    showModal: function(modalView) {
      // Global div for workaround with iframe resize and modals
      var region = this.application.getRegion('registryModal')
      var collectionRegion = this.application.getRegion('collectionRegion')
      var iFrameModalDOM = $('#IframeModalDOM')
      modalView.$el.on('hidden.bs.modal', function() {
        iFrameModalDOM.hide()
      })
      modalView.$el.on('shown.bs.modal', function() {
        var extraHeight =
          modalView.el.firstChild.clientHeight - collectionRegion.$el.height()
        if (extraHeight > 0) {
          iFrameModalDOM.height(extraHeight)
          iFrameModalDOM.show()
        }
      })
      region.show(modalView)
      region.currentView.$el.modal()
    },
  })

  return RegistryView
})
