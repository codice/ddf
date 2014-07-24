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
    'backbone',
    'icanhaz',
    'text!/installer/templates/profile.handlebars'
],
    function (Marionette, Backbone, ich, finishTemplate) {

        ich.addTemplate('profileTemplate', finishTemplate);

        var ProfileView = Marionette.ItemView.extend({

            template: 'profileTemplate',
            tagName: 'div',
            className: 'full-height',
            events: {
                'click .profile-option':'doOptionSelected',
                'click .customize' : 'doCustomProfileToggle'
            },
            initialize: function(options) {
                this.navigationModel = options.navigationModel;
                this.model = new Backbone.Model(options.navigationModel.pick('selectedProfile','isCustomProfile'));
                this.listenTo(this.navigationModel,'next', this.next);
                this.listenTo(this.navigationModel,'previous', this.previous);

                this.listenTo(this.model, 'change', this.modelChanged);

                this.modelBinder = new Backbone.ModelBinder();
            },
            serializeData: function(){
                return {
                    profiles: this.collection.toJSON()
                };
            },
            onRender: function(){
                this.modelBinder.bind(this.model, this.el);
                this.modelChanged(); // trigger ui population from model.
                this.$('.profile-options').perfectScrollbar({useKeyboard: false});
            },
            onBeforeClose: function(){
                this.modelBinder.unbind();
                this.$('.profile-options').perfectScrollbar('destroy');
            },
            next: function() {
                //this is your hook to perform any validation you need to do before going to the next step

                this.navigationModel.set(this.model.toJSON());
                this.navigationModel.nextStep();
            },
            previous: function() {
                //this is your hook to perform any teardown that must be done before going to the previous step
                this.navigationModel.previousStep();
            },

            doOptionSelected: function(evt){
                var input = this.$(evt.currentTarget).find('input');
                this.model.set('selectedProfile', input.val());
            },

            modelChanged: function(){
                this.$('.profile-option').removeClass('selected');
                this.$('input[value=' +  this.model.get('selectedProfile') + ']').closest('.profile-option').addClass('selected');
                this.$('.customize').attr('data-is-customized', this.model.get('isCustomProfile'));
            },
            doCustomProfileToggle: function(){
                var newValue = !(this.model.get('isCustomProfile'));
                this.model.set('isCustomProfile',newValue);
            }
        });

        return ProfileView;
    });