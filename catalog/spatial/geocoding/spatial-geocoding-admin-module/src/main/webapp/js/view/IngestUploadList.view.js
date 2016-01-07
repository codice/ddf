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
        'backbone.marionette',
        './IngestUploadListItem.view.js',
        'fileupload'
    ],
    function (Marionette, UploadListItem) {
        // The order in which the files will appear in the list (higher number = closer to
        // the top of the list)
        var order = {
            uploading: 4,
            start: 3,
            failed: 2,
            done: 1
        };

        var UploadList = Marionette.CollectionView.extend({
            tagName: 'ul',
            className: 'file-list',
            childView: UploadListItem,
            childEvents: {
                'stateChanged': function() {
                    // Re-sort the views when a child changes state so the files are reordered in
                    // the list based on their upload state.
                    this.reorder();
                }
            },
            viewComparator: function(item1, item2) {
                var state1 = item1.get('state');
                var state2 = item2.get('state');

                return order[state2] - order[state1];
            },
            initialize: function() {
                this.listenTo(this.collection, "startUpload", this.startUpload);
            },

            startUpload: function() {
                this.collection.each(function(item) {
                    item.trigger('startItemUpload');
                });
            }
        });
        return UploadList;
    });