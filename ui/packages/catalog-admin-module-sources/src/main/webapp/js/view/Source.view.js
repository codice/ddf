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
  'js/view/ModalSource.view.js',
  'js/view/EmptyView.js',
  'js/model/Service.js',
  'js/model/Status.js',
  'wreqr',
  'js/view/Utils.js',
  'templates/deleteModal.handlebars',
  'templates/deleteSource.handlebars',
  'templates/sourcePage.handlebars',
  'templates/sourceList.handlebars',
  'templates/sourceRow.handlebars',
], function(
  ich,
  Marionette,
  _,
  $,
  Q,
  ModalSource,
  EmptyView,
  Service,
  Status,
  wreqr,
  Utils,
  deleteModal,
  deleteSource,
  sourcePage,
  sourceList,
  sourceRow
) {
  var SourceView = {}

  ich.addTemplate('deleteModal', deleteModal)
  ich.addTemplate('deleteSource', deleteSource)
  ich.addTemplate('sourcePage', sourcePage)
  ich.addTemplate('sourceList', sourceList)
  ich.addTemplate('sourceRow', sourceRow)

  SourceView.SourceRow = Marionette.Layout.extend({
    template: 'sourceRow',
    tagName: 'tr',
    className: 'highlight-on-hover',
    regions: {
      editModal: '.modal-container',
    },
    events: {
      'change .configurationSelect': 'changeConfiguration',
      'click .configurationSelect': 'handleSelector',
      'click td': 'editSource',
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.listenTo(this.model, 'change', this.render)

      if (this.model.has('currentConfiguration')) {
        this.listenTo(
          wreqr.vent,
          'status:update-' + this.model.attributes.currentConfiguration.id,
          this.updateStatus
        )
      }
    },
    serializeData: function() {
      var data = {}

      if (this.model && this.model.has('currentConfiguration')) {
        data.currentConfiguration = this.model
          .get('currentConfiguration')
          .toJSON()
        data.currentUrl = this.model.get('currentUrl')
        data.isLoopbackUrl = this.model.get('isLoopbackUrl')
      } else {
        data.currentConfiguration = undefined
      }
      if (this.model && this.model.has('disabledConfigurations')) {
        data.disabledConfigurations = this.model
          .get('disabledConfigurations')
          .toJSON()
      }

      if (typeof this.model.get('available') === 'undefined') {
        data.loading = true
      } else {
        data.available = this.model.get('available')
      }

      data.name = this.model.get('name')

      return data
    },
    handleSelector: function(evt) {
      evt.stopPropagation()
    },
    editSource: function(evt) {
      evt.stopPropagation()
      var service = this.model
      wreqr.vent.trigger('editSource', service)
    },
    changeConfiguration: function(evt) {
      var model = this.model
      var currentConfig = model.get('currentConfiguration')
      var disabledConfigs = model.get('disabledConfigurations')
      var $select = $(evt.currentTarget)
      var optionSelected = $select.find('option:selected')
      var valueSelected = optionSelected.val()
      var cfgToDisable
      var deferred = $.Deferred().resolve()

      if (valueSelected === 'Disabled') {
        cfgToDisable = currentConfig
        if (!_.isUndefined(cfgToDisable)) {
          deferred = deferred.then(function() {
            return cfgToDisable.makeDisableCall()
          })
        }
      } else {
        var cfgToEnable = disabledConfigs.find(function(cfg) {
          return valueSelected + '_disabled' === cfg.get('fpid')
        })

        if (cfgToEnable) {
          cfgToDisable = currentConfig

          if (!_.isUndefined(cfgToDisable)) {
            deferred = deferred.then(function() {
              return cfgToDisable.makeDisableCall()
            })
          }

          deferred = deferred.then(function() {
            return cfgToEnable.makeEnableCall()
          })
        }
      }
      deferred.then(function() {
        wreqr.vent.trigger('refreshSources')
      })
      evt.stopPropagation()
    },
    updateStatus: function(status) {
      this.model.set('available', status.get('value'))
      this.render()
    },
  })

  SourceView.SourceTable = Marionette.CompositeView.extend({
    template: 'sourceList',
    itemView: SourceView.SourceRow,
    emptyView: EmptyView.sources,
    itemViewContainer: 'tbody',

    initialize: function() {
      this.showSourcesLoading()
    },
    collectionEvents: {
      request: 'showSourcesLoading',
      sync: 'hideSourcesLoading',
    },
    showSourcesLoading: function() {
      this.$el.find('table').addClass('hide')
      this.$el.find('#sources-loading').removeClass('hide')
    },
    hideSourcesLoading: function() {
      this.$el.find('#sources-loading').addClass('hide')
      this.$el.find('table').removeClass('hide')
    },
  })

  SourceView.SourcePage = Marionette.Layout.extend({
    template: 'sourcePage',
    events: {
      'click #removeSourceButton': 'removeSource',
      'click #addSourceButton': 'addSource',
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.listenTo(wreqr.vent, 'editSource', this.editSource)
      this.listenTo(wreqr.vent, 'refreshSources', this.refreshSources)
      this.listenTo(wreqr.vent, 'changeConfiguration', this.changeConfiguration)
      new SourceView.ModalController({
        application: this,
      })
    },
    regions: {
      collectionRegion: '#sourcesRegion',
      sourcesModal: '#sources-modal',
    },
    onShow: function() {
      this.refreshButton = Utils.refreshButton(
        '#sourceRefreshButton',
        '#sourceRefreshButton > i.fa-refresh',
        this.refreshSources
      )
    },
    onDestroy: function() {
      this.refreshButton.close()
    },
    onRender: function() {
      var collection = this.model.get('collection')
      var table = new SourceView.SourceTable({
        model: this.model,
        collection: collection,
      })
      this.collectionRegion.show(table)
      this.refreshSources()
    },
    refreshSources: function() {
      var view = this
      view.model.get('collection').trigger('request')
      view.model.get('model').clear()
      view.model.get('model').fetch({
        success: function() {
          view.model.get('collection').sort()
          view.model.get('collection').trigger('reset')
          view.model.get('collection').trigger('sync')
          view.refreshButton.done()
        },
      })
    },
    editSource: function(service) {
      wreqr.vent.trigger(
        'showModal',
        new ModalSource.View({
          model: this.model.getSourceModelWithServices(service),
          source: this.model,
          mode: 'edit',
        })
      )
    },
    removeSource: function() {
      if (this.model) {
        wreqr.vent.trigger(
          'showModal',
          new SourceView.DeleteModal({
            model: this.model,
            collection: this.model.get('collection'),
          })
        )
      }
    },
    addSource: function() {
      if (this.model) {
        wreqr.vent.trigger(
          'showModal',
          new ModalSource.View({
            model: this.model.getSourceModelWithServices(),
            source: this.model,
            mode: 'add',
          })
        )
      }
    },
  })

  SourceView.ModalController = Marionette.Controller.extend({
    initialize: function(options) {
      this.application = options.application
      this.listenTo(wreqr.vent, 'showModal', this.showModal)
    },
    showModal: function(modalView) {
      // Global div for workaround with iframe resize and modals
      var region = this.application.getRegion('sourcesModal')
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

  SourceView.DeleteItem = Marionette.ItemView.extend({
    template: 'deleteSource',
  })

  SourceView.DeleteModal = Marionette.CompositeView.extend({
    template: 'deleteModal',
    className: 'modal',
    itemView: SourceView.DeleteItem,
    itemViewContainer: '.modal-body',
    events: {
      'click .submit-button': 'deleteSources',
      'click .deleteSource': 'toggleChecks',
      'click .selectSourceDelete': 'toggleChecks',
    },
    behaviors: [
      {
        behaviorClass: Utils.modalDismissalBehavior,
      },
    ],

    toggleChecks: function(e) {
      var view = this
      var checked = e.target.checked
      // Loop through all the source views available to get the one relevant to the click.
      // We don't want to go deleting/checking the wrong source/service.
      var sourceView = view.children.find(function(childView) {
        return childView.model.get('name') === e.target.value
      })

      // Check to see if user clicked the source (top-level) checkbox or a service checkbox.
      if (e.target.className === 'deleteSource') {
        // If user clicked source checkbox, toggle all services underneath to match it.
        sourceView.$('.selectSourceDelete').each(function() {
          this.checked = checked
        })
      } else if (e.target.className === 'selectSourceDelete') {
        // If user deselected a service checkbox, make sure to also deselect the source checkbox.
        if (!checked) {
          sourceView.$('.deleteSource')[0].checked = false

          // Check to see if a user has manually selected all the services in a source, if they have,
          // also select the source checkbox.
        } else if (checked) {
          var allChecked = true
          sourceView.$('.selectSourceDelete').each(function() {
            allChecked = allChecked && this.checked
          })
          sourceView.$('.deleteSource')[0].checked = allChecked
        }
      }
    },
    deleteSources: function() {
      var view = this
      var toDelete = []
      view.collection.each(function(item) {
        var currentConfig = item.get('currentConfiguration')
        var disableConfigs = item.get('disabledConfigurations')
        view.$('.selectSourceDelete').each(function(index, content) {
          if (content.checked) {
            var id = currentConfig ? currentConfig.get('id') : null
            if (id === content.id) {
              toDelete.push(view.model.createDeletePromise(item, currentConfig))
            } else if (disableConfigs) {
              disableConfigs.each(function(disabledConfig) {
                if (disabledConfig.get('id') === content.id) {
                  toDelete.push(
                    view.model.createDeletePromise(item, disabledConfig)
                  )
                }
              })
            }
          }
        })
      })

      //remove queued source configurations and entire source config if necessary
      if (toDelete.length > 0) {
        //remove all selected configurations from the current item
        Q.all(toDelete)
          .then(function(results) {
            _.each(results, function(result) {
              var item = result.source
              if (item.size() <= 0) {
                //if no type configurations, delete the entire source.
                view.model.get('collection').removeSource(item)
                view.model
                  .get('model')
                  .get('value')
                  .remove(item)
              }
            })
            wreqr.vent.trigger('refreshSources')
            view.$el.modal('hide')
          })
          .done()
      }
    },
  })

  return SourceView
})
