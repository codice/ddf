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
/*global define, alert*/
define([
    'marionette',
    'underscore',
    'jquery',
    'text!./Lightbox.hbs',
    'js/CustomElements',
    'js/store'
], function (Marionette, _, $, LightboxTemplate, CustomElements, store) {

    var componentName = 'lightbox';

    var LightboxView = Marionette.LayoutView.extend({
        template: LightboxTemplate,
        tagName: CustomElements.register(componentName),
        modelEvents: {
            'all': 'render',
            'change:open': 'handleOpen'
        },
        events: {
            'click': 'handleOutsideClick',
            'click .lightbox-close': 'close'
        },
        regions: {
            'lightboxContent': '.lightbox-content'
        },
        initialize: function () {
            $('body').append(this.el);
            this.listenTo(store.get('workspaces'), 'change:currentWorkspace', this.close);
            this.listenForClose();
        },
        listenForClose: function () {
            this.$el.on(CustomElements.getNamespace()+'close-'+componentName, function () {
                this.close();
            }.bind(this));
        },
        handleOpen: function () {
            this.$el.toggleClass('is-open', this.model.isOpen());
        },
        handleOutsideClick: function (event) {
            if (event.target === this.el) {
                this.close();
            }
        },
        close: function () {
            this.model.close();
        }
    });

    return LightboxView;
});
