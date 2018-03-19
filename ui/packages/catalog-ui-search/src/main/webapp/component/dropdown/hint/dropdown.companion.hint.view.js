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
    'marionette',
    'underscore',
    'jquery',
    '../dropdown.companion.view',
    'js/CustomElements'
], function (Marionette, _, $, DropdownCompanionView, CustomElements) {

    var namespace = CustomElements.getNamespace();

    return DropdownCompanionView.extend({
        className: 'is-hint',
        listenForOutsideClick: function () {
            DropdownCompanionView.prototype.listenForOutsideClick.call(this);
            $(namespace+'help').on('mousedown.' + this.cid, function (event) {
                if (this.$el.find(event.target).length === 0) {
                    this.close();
                }
            }.bind(this));
        }
    });
});
