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
  'underscore',
  'icanhaz',
  'backbone',
  'marionette',
  'text!templates/uploadModal.handlebars',
  'text!templates/fileInfo.handlebars',
  'text!templates/error.handlebars',
  'text!templates/certificateDetail.handlebars',
  'handlebars',
  'js/model/FileHelper',
  'js/model/UrlCertificate',
  'fileupload',
], function(
  $,
  _,
  ich,
  Backbone,
  Marionette,
  uploadModal,
  fileInfo,
  error,
  certificateDetail,
  Handlebars,
  FileHelper,
  UrlCertificate
) {
  const UploadModalView = {}

  ich.addTemplate('uploadModal', uploadModal)
  ich.addTemplate('fileInfo', fileInfo)
  ich.addTemplate('error', error)
  ich.addTemplate('certificateDetail', certificateDetail)

  const BaseModal = Marionette.LayoutView.extend({
    // use the Backbone constructor paradigm to allow extending of classNames
    constructor: function() {
      this.className = 'modal fade ' + this.className // add on modal specific classes.
      Marionette.LayoutView.prototype.constructor.apply(this, arguments)
    },
    // by default, "destroy" just destroys the modal
    destroy: function() {
      const view = this
      // we add this listener because we do not want to remove the dom before the animation completes.
      this.$el.one('hidden.bs.modal', function() {
        view.destroy()
      })
      this.hide()
    },
    show: function() {
      this.$el.modal({
        backdrop: 'static',
        keyboard: false,
      })
    },
    hide: function() {
      this.$el.modal('hide')
    },
  })

  const UrlCertView = Marionette.ItemView.extend({
    template: 'certificateDetail',
    modelEvents: {
      change: 'render',
    },
  })

  UploadModalView.UploadModal = BaseModal.extend({
    template: 'uploadModal',
    className: 'upload-modal',
    events: {
      'click .save': 'save',
      'click .cancel': 'close',
      'click .show-url-btn': 'showUrlCerts',
      'click .nav-tabs a': 'checkSave',
      'keyup #alias': 'validateTextInput',
      'keyup *': 'checkSave',
    },
    ui: {
      alias: '#alias',
      keypass: '#keypass',
      storepass: '#storepass',
    },
    regions: {
      fileuploadregion: '.file-upload-region',
      fileeditregion: '.file-edit-region',
      errorregion: '.error-region',
      urlcertregion: '.url-cert-region',
    },
    showUrlCerts: function(e) {
      const view = this
      const url = this.$('.urlField').val()
      const encodedUrl = btoa(url)
      const model = new UrlCertificate.Response()
      model
        .fetch({ url: model.url + '/' + encodedUrl })
        .done(function() {
          view.urlModel.unset('error')
          if (view.urlModel.get('status') >= 400) {
            view.urlModel.set(
              'error',
              'Error while looking up certificates. Check URL.'
            )
            view.disable()
          } else if (
            view.urlModel.get('value') &&
            Object.keys(view.urlModel.get('value').attributes).length === 0
          ) {
            view.urlModel.set(
              'error',
              'Unable to determine certificates. Check URL.'
            )
            view.disable()
          } else {
            view.checkSave(e)
          }
        })
        .fail(function(result) {
          view.disable()
          if (result.status && result.status >= 400) {
            view.urlModel.set('error', result.statusText + ': ' + result.status)
          }
        })
      this.urlcertregion.show(new UrlCertView({ model: model }))
      this.urlModel = model
    },
    isValid: function(e) {
      const activeTab = this.$('.nav-tabs li.active a').attr('href')
      const target = (e.currentTarget || {}).hash || activeTab
      if (target === '#upload') {
        return this.ui.alias.val() !== '' && this.file.isValid()
      } else if (target === '#url') {
        return (
          this.urlcertregion.currentView !== undefined &&
          this.urlcertregion.currentView.model.attributes.error === undefined &&
          this.urlcertregion.currentView.model.keys().length > 0
        )
      }
      return false
    },
    // Checks if the modal save button can be enabled if this is valid.
    checkSave: function(e) {
      if (this.isValid(e)) {
        this.enable()
      } else {
        this.disable()
      }
    },
    validateTextInput: function(e) {
      const input = $(e.target)
      if (input.val() !== '') {
        input
          .parent()
          .parent()
          .removeClass('has-error')
      } else {
        input
          .parent()
          .parent()
          .addClass('has-error')
      }
    },
    initialize: function() {
      this.file = new FileHelper()
      this.error = new Backbone.Model()
    },
    enable: function() {
      this.$('.save').removeAttr('disabled')
    },
    disable: function() {
      this.$('.save').attr('disabled', 'disabled')
    },
    onSelect: function(e, data) {
      this.file.setData(data)
      this.checkSave(e)
    },
    onRender: function() {
      this.errorregion.show(
        new Marionette.ItemView({
          modelEvents: {
            change: 'render',
          },
          model: this.error,
          template: 'error',
        })
      )
      this.fileuploadregion.show(
        new Marionette.ItemView({
          modelEvents: {
            change: 'render',
          },
          serializeModel: function() {
            const value = this.model.get('value')
            if (_.isString(value)) {
              return [value]
            }
            return value
          },
          model: this.file,
          template: 'fileInfo',
        })
      )
      this.$('.fileupload').fileupload({
        url: this.url,
        paramName: 'file',
        dataType: 'json',
        maxFileSize: 5000000,
        maxNumberOfFiles: 1,
        dropZone: this.$el,
        add: _.bind(this.onSelect, this),
      })
      this.disable()
    },
    save: function() {
      const activeTab = this.$('.nav-tabs li.active a').attr('href')
      const that = this
      if (activeTab === '#upload') {
        const alias = this.ui.alias.val()
        const keypass = this.ui.keypass.val()
        const storepass = this.ui.storepass.val()

        this.file.load(function() {
          const model = that.options.collection.create(
            {
              alias: alias,
              keypass: keypass,
              storepass: storepass,
              file: that.file.toJSON(),
            },
            {
              wait: true,
              success: _.bind(function() {
                that.options.collection.parents[0].fetch()
                that.destroy()
              }, this),
            }
          )
          if (!model.isValid()) {
            that.error.set('value', model.validate())
          }
        })
      } else if (activeTab === '#url') {
        const url = this.$('.urlField').val()
        const encodedUrl = btoa(url)
        this.urlModel
          .fetch({ url: this.urlModel.saveUrl + '/' + encodedUrl })
          .done(function(result) {
            that.options.collection.parents[0].fetch({ reset: true })
            const obj = _.reduce(result.value, function(memo, val) {
              return !(!memo || !val)
            })
            if (obj.success) {
              that.destroy()
            }
          })
      }
    },
  })

  return UploadModalView
})
