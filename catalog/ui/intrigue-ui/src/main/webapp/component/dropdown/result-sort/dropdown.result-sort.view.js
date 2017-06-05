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
    '../dropdown.view',
    './dropdown.result-sort.hbs',
    'component/result-sort/result-sort.view',
    'component/singletons/user-instance'
], function (Marionette, _, $, DropdownView, template, ComponentView, user) {

    return DropdownView.extend({
        template: template,
        className: 'is-resultSort',
        componentToShow: ComponentView,
        initialize: function(){
            DropdownView.prototype.initialize.call(this);
            this.listenTo(user.get('user').get('preferences'), 'change:resultSort', this.handleSort);
            this.handleSort();
        },
        handleSort: function(){
            var resultSort = user.get('user').get('preferences').get('resultSort');
            this.$el.toggleClass('has-sort', Boolean(resultSort));
        }
    });
});
