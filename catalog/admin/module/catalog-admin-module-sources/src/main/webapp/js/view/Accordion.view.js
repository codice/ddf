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
    'icanhaz',
    'js/view/Accordion.view.js',
    'text!templates/accordion.hbs'
], function (Marionette, ich, AccordionView, accordion) {
    if (!ich.accordion) {
        ich.addTemplate('accordion', accordion);
    }

    AccordionView = Marionette.Layout.extend({
        template: 'accordion',
        tagName: 'div',
        regions: {
            accordionContent: '.accordion-content'
        },
        events: {
            'click .accordion-title': 'toggle'
        },
        toggle: function () {
            this.$el.find('.accordion-content').toggleClass('show');
            this.$el.find(".fa-chevron-down, .fa-chevron-up").toggleClass('fa-chevron-down fa-chevron-up');
        },
        onRender: function () {
            this.accordionContent.show(this.model.get('contentView'));
        }
    });
    return AccordionView;
});