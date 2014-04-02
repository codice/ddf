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
/** Main view page for add. */
define([
    'marionette',
    'icanhaz',
    'text!/installer/templates/navigation.handlebars'
    ], function (Marionette, ich, navigationTemplate) {

    ich.addTemplate('navigationTemplate', navigationTemplate);

    var WelcomeView = Marionette.ItemView.extend({
        template: 'navigationTemplate',
        tagName: 'div',
        events: {
            'click .previous': 'previous',
            'click .next': 'next'
        },
        initialize: function() {
            this.listenTo(this.model, 'change', this.render);
        },
        previous: function() {
            this.model.trigger('previous');
        },
        next: function() {
            this.model.trigger('next');
        }
    });

    return WelcomeView;
});