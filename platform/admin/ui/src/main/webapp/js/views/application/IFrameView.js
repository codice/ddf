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
/* global define,setInterval,clearInterval */
define([
    'marionette',
    'icanhaz',
    'text!iframeView',
    'text!waitForVisible'
    ],function (Marionette, ich, iframeView, waitForVisible) {

    ich.addTemplate('iframeView',iframeView);
    ich.addTemplate('waitForVisible',waitForVisible);

    var IFrameView = Marionette.ItemView.extend({
        template: 'iframeView',
        className: 'iframe-view'
    });

    return Marionette.Layout.extend({
        template: 'waitForVisible',
        isVisible: function () {
            return this.$el.is(':visible');
        },
        regions: { iframe: '.wait-for-visible' },
        initialize: function () {
            // delay rendering of iframe until the element is actually visible
            var interval = setInterval(function () {
                if (this.isVisible()) {
                    // once this is visible, show iframe and stop polling for visibility
                    this.iframe.show(new IFrameView({ model: this.model }));
                    clearInterval(interval);
                }
            }.bind(this), 250);
        }
    });

});
