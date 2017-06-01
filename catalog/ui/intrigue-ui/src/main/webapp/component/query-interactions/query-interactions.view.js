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
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    './query-interactions.hbs',
    'js/CustomElements',
    'js/store',
    'decorator/menu-navigation.decorator',
    'decorator/Decorators',
    'component/lightbox/lightbox.view.instance',
    'component/query-feedback/query-feedback.view'
], function (wreqr, Marionette, _, $, template, CustomElements, store, MenuNavigationDecorator, Decorators, lightboxInstance, QueryFeedbackView) {

    return Marionette.ItemView.extend(Decorators.decorate({
        template: template,
        tagName: CustomElements.register('query-interactions'),
        modelEvents: {
        },
        events: {
            'click .interaction-run': 'handleRun',
            'click .interaction-stop': 'handleCancel',
            'click .interaction-delete': 'handleDelete',
            'click .interaction-duplicate': 'handleDuplicate',
            'click .interaction-deleted': 'handleDeleted',
            'click .interaction-historic': 'handleHistoric',
            'click .interaction-feedback': 'handleFeedback',
            'click': 'handleClick'
        },
        ui: {
        },
        initialize: function(){
            if (!this.model.get('result')) {
                this.startListeningToSearch();
            }
            this.handleResult();
        },
        onRender: function(){
        },
        startListeningToSearch: function(){
            this.listenToOnce(this.model, 'change:result', this.startListeningForResult);
        },
        startListeningForResult: function(){
            this.listenToOnce(this.model.get('result'), 'sync error', this.handleResult);
        },
        handleRun: function(){
            this.model.startSearch();
        },
        handleCancel: function(){
            this.model.cancelCurrentSearches();
        },
        handleDelete: function(){
            this.model.collection.remove(this.model);
        },
        handleDeleted: function(){
            this.model.startSearch({
                limitToDeleted: true
            });
        },
        handleDuplicate: function(){
            if (this.model.collection.canAddQuery()){
                var copyAttributes = JSON.parse(JSON.stringify(this.model.attributes));
                delete copyAttributes.id;
                delete copyAttributes.result;
                var newQuery = new this.model.constructor(copyAttributes);
                store.setQueryByReference(newQuery);
            }
        },
        handleHistoric: function(){
            this.model.startSearch({
                limitToHistoric: true
            });
        },
        handleFeedback: function(){
            lightboxInstance.model.updateTitle('Search Quality Feedback');
            lightboxInstance.model.open();
            lightboxInstance.lightboxContent.show(new QueryFeedbackView({
                model: this.model
            }));
        },  
        handleResult: function(){
            this.$el.toggleClass('has-results', this.model.get('result') !== undefined);
        },
        handleClick: function(){
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        }
    }, MenuNavigationDecorator));
});
