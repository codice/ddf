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
/*global require*/
const Marionette = require('marionette');
const CustomElements = require('js/CustomElements');
const template = require('./source-item.hbs');
const lightboxInstance = require('component/lightbox/lightbox.view.instance');
const SourceAppView = require('component/source-app/source-app.view');

module.exports = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('source-item'),
    modelEvents: {
        'change': 'render'
    },
    events: {
        'click button': 'onActionClicked'
    },
    onActionClicked(event) {

        var target = event.currentTarget;

        if(target !== null) {
            const url = target.getAttribute('data-url');
            const title = target.getAttribute('data-title');

            lightboxInstance.model.updateTitle(title);
            lightboxInstance.model.open();
            lightboxInstance.lightboxContent.show(new SourceAppView({ url }));
        }
    },
    onRender(){
        this.$el.toggleClass('is-available', this.model.get('available'));

    }
});