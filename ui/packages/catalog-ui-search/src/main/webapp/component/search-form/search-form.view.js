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
 var Marionette = require('marionette');
 var $ = require('jquery');
 var template = require('./search-form.hbs');
 var CustomElements = require('js/CustomElements');
 var user = require('component/singletons/user-instance');

 module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('search-form'),
    className: 'is-button',
    events: {
        'click': 'changeView'
    },
    onRender: function() {
        if (this.model.get('type') === 'basic' || this.model.get('type') === 'text') {
            this.$el.addClass('is-static');
        }
    },
    changeView: function() {

        switch(this.model.get('type')) {
            case 'basic':
                this.options.queryModel.set('type', 'basic');
                user.getQuerySettings().set('type', 'basic');
                break;
            case 'text':
                this.options.queryModel.set('type', 'text');
                user.getQuerySettings().set('type', 'text');
                break;
            case 'custom':
                var oldType = this.options.queryModel.get('type');
                this.options.queryModel.set({
                    type: 'custom',
                    title: this.model.get('name')
                });
                user.getQuerySettings().set({
                    type: 'custom',
                    template: this.model.toJSON()
                });
                if (oldType  === 'custom') {
                    this.options.queryModel.trigger('change:type');
                }

                break;
        }

        user.savePreferences();
        this.triggerCloseDropdown();
    },
    triggerCloseDropdown: function() {
        this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
    }
});