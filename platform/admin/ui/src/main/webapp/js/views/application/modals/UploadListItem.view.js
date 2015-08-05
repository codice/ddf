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
/* global define */
define([
    'marionette',
    'jquery',
    'icanhaz',
    'fileupload',
    'text!templates/application/modals/newFileRow.handlebars'
],
function (Marionette,$,ich,fileupload,newFileRow) {

    ich.addTemplate('newFileRow',newFileRow);

    var UploadItem = Marionette.ItemView.extend({
        template: 'newFileRow',
        tagName:'li',
        className: 'file-list-item',
        events: {
            'click .start-upload':'startUpload'
        },
        modelEvents: {
            'change:progress': 'changeProgress',
            'change:state': 'render' // only re-render on state changes
        },
        startUpload: function(){
            this.model.set({'state':'uploading'});
            $.blueimp.fileupload.prototype
                .options.add.call(this.model.fileuploadObject.ref, this.model.fileuploadObject.e, this.model.fileuploadObject.data);
            this.model.trigger('uploading');  // push up to whoever wants to listen.
        },
        doneUpload: function(){
            this.model.set({'state':'done'});
        },
        changeProgress: function(){
            var progress = this.model.get('progress');
            this.$('.progress-percent-text').html(progress + "%");
            this.$(".progress-bar").attr('aria-valuenow', progress).css({width: progress+'%'});
        }
    });

    return UploadItem;
});