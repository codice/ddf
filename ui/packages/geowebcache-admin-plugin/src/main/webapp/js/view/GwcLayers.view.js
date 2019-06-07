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
  'jquery',
  'backbone',
  'underscore',
  'backbone.marionette',
  'handlebars',
  'icanhaz',
  'text!templates/gwcLayerPage.handlebars',
  'text!templates/gwcLayerTable.handlebars',
  'js/view/Modal',
  'text!templates/addLayerModal.handlebars',
], function(
  $,
  Backbone,
  _,
  Marionette,
  Handlebars,
  ich,
  gwcLayerPage,
  gwcLayerTable,
  Modal,
  addLayerModal
) {
  var EMPTY_INPUT_DIV =
    "<div><input type='text' class='form-control input-list'></input><a href='#' class='fa fa-minus-square fa-lg minus-button'></a></div>"

  var GwcLayersView = {}

  ich.addTemplate('gwcLayerTable', gwcLayerTable)
  ich.addTemplate('gwcLayerPage', gwcLayerPage)
  ich.addTemplate('addLayerModal', addLayerModal)

  GwcLayersView.LayersPage = Marionette.LayoutView.extend({
    template: 'gwcLayerPage',
    regions: {
      layersTable: '.gwc-layers-table',
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
    },
    onRender: function() {
      this.layersTable.show(
        new GwcLayersView.LayersTable({ model: this.model })
      )
    },
  })

  GwcLayersView.Modal = Modal.extend({
    template: 'addLayerModal',
    events: {
      'click .add-layer': 'submitLayer',
      'click .add-mime': 'addMime',
      'click .add-wms-name': 'addWmsName',
      'click .minus-button': 'removeInput',
    },
    initialize: function() {
      Modal.prototype.initialize.apply(this, arguments)
    },
    onRender: function() {
      if (typeof this.options.layers !== 'undefined') {
        this.renderFields()
      }
      this.setupPopOver(
        '[data-toggle="wms-layers-popover"]',
        'The name(s) of WMS layers that exist at the URL specified above.  ' +
          'If no WMS Layer names are specified, GeoWebCache will look for the Layer Name specified in the name field.  Otherwise, ' +
          'it will attempt to find all layer names added here and combine them into one layer.'
      )
    },
    addMime: function() {
      $('.layer-mime').append(EMPTY_INPUT_DIV)
    },
    removeInput: function(e) {
      var element = $(e.target).parent()
      element.remove()
    },
    addWmsName: function() {
      $('.layer-wms-name').append(EMPTY_INPUT_DIV)
    },
    addLayer: function() {
      this.layers.addLayer(this.generateLayerDataFromFields())
    },
    renderFields: function() {
      var layer
      var that = this
      var name = this.options.name
      var layers = this.options.layers.get('layers')

      $.each(layers, function(index, value) {
        if (value.name === name) {
          layer = value
        }
      })

      if (layer.xmlLayers.length > 0) {
        this.$('.layer-wms-name').empty()
        $.each(layer.xmlLayers, function(index, value) {
          that
            .$('.layer-wms-name')
            .append(
              "<div><input type='text' class='form-control input-list' value='" +
                value +
                "'></input><a href='#' class='fa fa-minus-square fa-lg minus-button'></a></div>"
            )
        })
      }

      this.$('.layer-name').val(this.options.name)
      this.$('.layer-url').val(layer.xmlUrl)
      this.$('.layer-mime').empty()

      $.each(layer.mimeTypes, function(index, value) {
        that
          .$('.layer-mime')
          .append(
            "<div><input type='text' class='form-control input-list' value='" +
              value +
              "'></input><a href='#' class='fa fa-minus-square fa-lg minus-button'></a></div>"
          )
      })
    },
    submitLayer: function() {
      if (typeof this.options.layers !== 'undefined') {
        this.updateLayer()
      } else {
        this.addLayer()
      }
    },
    updateLayer: function() {
      this.options.layers.updateLayer(this.generateLayerDataFromFields())
    },
    generateLayerDataFromFields: function() {
      var name = $('.layer-name').val()
      var url = $('.layer-url').val()
      var mimeElements = $('.layer-mime').children()
      var mimes = []

      $.each(mimeElements, function(index, value) {
        var text = $(value)
          .find('input')
          .val()
        if (text !== '') {
          mimes.push(text)
        }
      })

      var wmsLayerNameElements = $('.layer-wms-name').children()
      var wmsLayerNames = []

      $.each(wmsLayerNameElements, function(index, value) {
        var text = $(value)
          .find('input')
          .val()
        if (text !== '') {
          wmsLayerNames.push(text)
        }
      })
      return {
        name: name,
        oldName: this.options.name,
        mimes: mimes,
        url: url,
        wmsLayerNames: wmsLayerNames,
      }
    },
    setupPopOver: function(selector, content) {
      var options = {
        trigger: 'hover',
        content: content,
      }
      this.$el.find(selector).popover(options)
    },
  })

  GwcLayersView.LayersTable = Marionette.LayoutView.extend({
    template: 'gwcLayerTable',
    events: {
      'click .remove-layer': 'removeLayer',
      'click .showUpdateModal': 'showUpdateModal',
      'click .showModal': 'showModal',
    },
    regions: {
      modalRegion: '.update-modal-region',
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.listenTo(this.model, 'change:layers', this.render)
    },
    removeLayer: function(el) {
      var name = $(el.target).attr('name')
      this.model.deleteLayer(name)
    },
    showUpdateModal: function(e) {
      this.modal = new GwcLayersView.Modal({
        name: $(e.target).text(),
        layers: this.model,
      })
      this.modalRegion.show(this.modal)
      this.modal.show()
      this.$('.modal').removeClass('fade')
    },
    showModal: function() {
      this.modal = new GwcLayersView.Modal()
      this.modalRegion.show(this.modal)
      this.modal.layers = this.model
      this.modal.show()
      this.$('.modal').removeClass('fade')
    },
  })
  return GwcLayersView
})
