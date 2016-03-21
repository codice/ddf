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
    'text!templates/preferences/preferences.menu.handlebars',
    'js/view/preferences/PreferencesModal.view',
    'wreqr'
], function (Marionette, preferencesMenuItem, Modal, wreqr) {
    return Marionette.LayoutView.extend({
        tagName: 'li',
        className: 'dropdown',
        template: preferencesMenuItem,
        regions: { modalRegion: '.modal-region' },
        events: { 'click .showModal': 'showModal' },
        showModal: function () {
            var modal = new Modal({ model: this.model });
            wreqr.vent.trigger('showModal', modal);
        }
    });
});
