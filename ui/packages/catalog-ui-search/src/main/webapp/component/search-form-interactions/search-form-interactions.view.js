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
/*global define, window*/


var  wreqr = require('wreqr');
var  Marionette = require('marionette');
var  template = require('./search-form-interactions.hbs');
var  CustomElements = require('js/CustomElements');
var  user = require('component/singletons/user-instance');
var  LoadingView = require('component/loading/loading.view');
var  lightboxInstance = require('component/lightbox/lightbox.view.instance');
var  announcement = require('component/announcement');
var  ConfirmationView = require('component/confirmation/confirmation.view');

module.exports =  Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('search-form-interactions'),
        className: 'composed-menu',
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click .interaction-default': 'handleMakeDefault',
            'click .interaction-clear': 'handleClearDefault',
            'click .interaction-trash': 'handleTrash',
            'click .search-form-interaction': 'handleClick'
        },
        ui: {},
        initialize: function() {
            this.checkIfDefaultSearchForm();
        },
        onRender: function() {
            this.checkIfSubscribed();
        },
        checkIfSubscribed: function() {
            this.$el.toggleClass('is-subscribed', Boolean(this.model.get('subscribed')));
        },
        handleTrash: function() {
            var loginUser = user.get('user');
            if(loginUser.get('username') === this.model.get('createdBy'))
            {
                this.listenTo(ConfirmationView.generateConfirmation({
                    prompt: 'This will permanently delete the template. Are you sure? ',
                    no: 'Cancel',
                    yes: 'Delete'
                }),
                'change:choice',
                function(confirmation) {
                    if (confirmation.get('choice')) {
                        var loadingview = new LoadingView();
                            this.model.url = '/search/catalog/internal/forms/' + this.model.id;
                            this.model.destroy({
                                wait: true,
                                error: function(model, xhr, options){
                                    announcement.announce({
                                        title: 'Error!',
                                        message: "Unable to delete the forms: " + xhr.responseText,
                                        type: 'error'
                                    });
                                    throw new Error('Error Deleting Template: ' + xhr.responseText);                                  
                                }
                            }); 
                        this.removeCachedTemplate(this.model.id);
                        loadingview.remove();
                    }
                }.bind(this));                        
            }
            else{
                this.messageNotifier(
                    'Error!',
                    'Unable to delete the form: You are not the author',
                    'error'
                );
                throw new Error('Unable to delete the form: You are not the author ');                
            }
            this.trigger("doneLoading");
        },
        handleMakeDefault: function() {
            var templateJSON = {
                filterTemplate: this.model.get('filterTemplate'),
                model: this.model
            };

            user.getQuerySettings().set('defaultTemplate', JSON.parse(JSON.stringify(templateJSON)));
            user.savePreferences();
            this.messageNotifier(
                'Success!', 
                `\"${this.model.get('name')}\" Saved As Default Query Form`, 
                'success'
            );
        },
        handleClearDefault: function() {
            user.getQuerySettings().set('defaultTemplate', null);
            user.savePreferences();
            this.messageNotifier(
                'Success!', 
                `Default Query Form Cleared`, 
                'success'
            );
        },
        checkIfDefaultSearchForm: function() {
            var storedTemplate = user.getQuerySettings().toJSON();
            var currentTemplate = this.model;

            this.$el.find('.interaction-default').toggleClass('is-hidden', storedTemplate['defaultTemplate'] != undefined &&
                storedTemplate['defaultTemplate'].model.name == currentTemplate.get('name'));
        },
        messageNotifier: function(title, message, type) {
            announcement.announce({
                title: title,
                message: message,
                type: type
            });
            this.checkIfDefaultSearchForm();
        },
        handleClick: function() {
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
        },
        removeCachedTemplate: function(id){
            wreqr.vent.trigger("deleteTemplateById", id);
        }
    });
