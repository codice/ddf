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
/*global define*/
define([
    'jquery',
    'underscore',
    'icanhaz',
    'backbone',
    'marionette',
    'text!templates/uploadModal.handlebars',
    'text!templates/fileInfo.handlebars',
    'text!templates/error.handlebars',
    'handlebars',
    'js/model/FileHelper',
    'fileupload'
], function ($, _, ich, Backbone, Marionette, uploadModal, fileInfo, error, Handlebars, FileHelper) {

    var UploadModalView = {};

    ich.addTemplate('uploadModal', uploadModal);
    ich.addTemplate('fileInfo', fileInfo);
    ich.addTemplate('error', error);

    var BaseModal = Marionette.LayoutView.extend({
        // use the Backbone constructor paradigm to allow extending of classNames
        constructor: function () {
            this.className = 'modal fade ' + this.className; // add on modal specific classes.
            Marionette.LayoutView.prototype.constructor.apply(this, arguments);
        },
        // by default, "destroy" just destroys the modal
        destroy: function () {
            var view = this;
            // we add this listener because we do not want to remove the dom before the animation completes.
            this.$el.one('hidden.bs.modal', function () {
                view.destroy();
            });
            this.hide();
        },
        show: function () {
            this.$el.modal({
                backdrop: 'static',
                keyboard: false
            });
        },
        hide: function () {
            this.$el.modal('hide');
        }
    });

    UploadModalView.UploadModal = BaseModal.extend({
        template: 'uploadModal',
        className: 'upload-modal',
        events: {
            'click .save': 'save',
            'click .cancel': 'close',
            'keyup #alias': 'validateTextInput',
            'keyup *': 'checkSave'
        },
        ui: {
            alias: '#alias',
            keypass: '#keypass',
            storepass: '#storepass'
        },
        regions: {
            fileuploadregion: '.file-upload-region',
            fileeditregion: '.file-edit-region',
            errorregion: '.error-region'
        },
        isValid: function () {
            return this.ui.alias.val() !== '' && this.file.isValid();
        },
        // Checks if the modal save button can be enabled if this is valid.
        checkSave: function () {
            if (this.isValid()) {
                this.enable();
            } else {
                this.disable();
            }
        },
        validateTextInput: function (e) {
            var input = $(e.target);
            if (input.val() !== '') {
                input.parent().parent().removeClass('has-error');
            } else {
                input.parent().parent().addClass('has-error');
            }
        },
        initialize: function () {
            this.file = new FileHelper();
            this.error = new Backbone.Model();
        },
        enable: function () {
            this.$('.save').removeAttr('disabled');
        },
        disable: function () {
            this.$('.save').attr('disabled', 'disabled');
        },
        onSelect: function (e, data) {
            this.file.setData(data);
            this.checkSave();
        },
        onRender: function () {
            this.errorregion.show(new Marionette.ItemView({
                modelEvents: {
                    change: 'render'
                },
                model: this.error,
                template: 'error'
            }));
            this.fileuploadregion.show(new Marionette.ItemView({
                modelEvents: {
                    change: 'render'
                },
                serializeModel: function () {
                    var value = this.model.get('value');
                    if (_.isString(value)) {
                        return [value];
                    }
                    return value;
                },
                model: this.file,
                template: 'fileInfo'
            }));
            this.$('.fileupload').fileupload({
                url: this.url,
                paramName: 'file',
                dataType: 'json',
                maxFileSize: 5000000,
                maxNumberOfFiles: 1,
                dropZone: this.$el,
                add: _.bind(this.onSelect, this)
            });
            this.disable();
        },
        save: function () {

            var that = this;
            var alias = this.ui.alias.val();
            var keypass = this.ui.keypass.val();
            var storepass = this.ui.storepass.val();

            this.file.load(function () {
                var model = that.options.collection.create({
                    alias: alias,
                    keypass: keypass,
                    storepass: storepass,
                    file: that.file.toJSON()
                }, {
                    wait: true,
                    success: _.bind(function () {
                        that.options.collection.parents[0].fetch();
                        that.destroy();
                    }, this)
                });
                if (!model.isValid()) {
                    that.error.set('value', model.validate());
                }

            });
        }
    });

    return UploadModalView;
});
