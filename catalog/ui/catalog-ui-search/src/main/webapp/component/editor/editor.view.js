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
    'text!./editor.hbs',
    'js/CustomElements',
], function (Marionette, _, $, template, CustomElements) {

    return Marionette.LayoutView.extend({
        setDefaultModel: function(){
            //override
        },
        template: template,
        tagName: CustomElements.register('editor'),
        modelEvents: {
        },
        events: {
            'click .editor-edit': 'edit',
            'click .editor-save': 'save',
            'click .editor-cancel': 'cancel'
        },
        regions: {
            editorProperties: '.editor-properties'
        },
        initialize: function (options) {
            if (options.model === undefined){
                this.setDefaultModel();
            }
        },
        onBeforeShow: function(){
            //override
        },
        edit: function(){
            this.$el.addClass('is-editing');
            this.editorProperties.currentView.turnOnEditing();
            this.editorProperties.currentView.focus();
        },
        cancel: function(){
            this.$el.removeClass('is-editing');
            this.editorProperties.currentView.turnOffEditing();
            this.editorProperties.currentView.revert();
        },
        save: function(){
            this.$el.removeClass('is-editing');
            this.editorProperties.currentView.turnOffEditing();
            this.editorProperties.currentView.save();
            this.onSave(this.editorProperties.currentView.toJSON());
        },
        onSave: function(){
            //override
        },
        toJSON: function(){
            return this.editorProperties.currentView.toJSON();
        }
    });
});
