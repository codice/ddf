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
define(function (require) {

    var Marionette = require('marionette'),
        ich = require('icanhaz');

    ich.addTemplate('welcomeTemplate', require('text!/installer/templates/welcome.handlebars'));

    var WelcomeView = Marionette.ItemView.extend({
        template: 'welcomeTemplate',
        tagName: 'div',
        className: 'full-height',
        initialize: function() {
            this.listenTo(this.model,'next', this.next);
            this.listenTo(this.model,'previous', this.previous);
        },
        onClose: function() {
            this.stopListening(this.model);
        },
        next: function() {
            //this is your hook to perform any validation you need to do before going to the next step
            this.model.nextStep();
        },
        previous: function() {
            //this is your hook to perform any teardown that must be done before going to the previous step
            this.model.previousStep();
        }
    });

    return WelcomeView;
});