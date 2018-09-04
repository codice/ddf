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
var Marionette = require('marionette');
var _ = require('underscore');
var CustomElements = require('js/CustomElements');
var SearchInteractionsDropdownView = require('component/dropdown/search-interactions/dropdown.search-interactions.view');
var DropdownModel = require('component/dropdown/dropdown');
const React = require('react');

const zeroWidthSpace = "\u200B";

module.exports = Marionette.LayoutView.extend({
    events: {
        'change input': 'updateQueryName',
        'keyup input': 'updateQueryName',
        'click > .is-actions > .trigger-edit': 'focus'
    },
    regions: {
        searchInteractions: '> .is-actions > .search-interactions'
    },
    template(props) {
        return (
            <React.Fragment key={props.id}>
                <input className="query-title" placeholder="Search Name" defaultValue={props.title} data-help="Displays the name given to the search."/>
                <div className="is-actions">
                    <span className="button-title">
                        {props.title}
                    </span>
                    <span className="is-button fa fa-pencil trigger-edit"></span>
                    <div className="is-button search-interactions">
                    </div>
                </div>
            </React.Fragment>
        )   
    },
    tagName: CustomElements.register('query-title'),
    initialize: function(options) {
        this.updateQueryName = _.throttle(this.updateQueryName, 200);
        this.listenTo(this.model, 'change:title', this.handleTitleUpdate);
    },
    onDomRefresh: function() {
        if (!this.model._cloneOf) {
            this.$el.find('input').select();
        }
    },
    onRender: function(){
        this.updateQueryName();
        this.showSearchInteractions();
    },
    showSearchInteractions: function() {
        this.searchInteractions.show(new SearchInteractionsDropdownView({
            model: new DropdownModel(),
            modelForComponent: this.model,
            dropdownCompanionBehaviors: {
                navigation: {}
            }
        }));
    },
    focus: function(){
        this.$el.find('input').focus();
    },
    getSearchTitle: function(){
        var title = this.$el.find('input').val();
        return title !== "" ? title : 'Search Name';
    },
    handleTitleUpdate: function() {
        this.$el.find('input').val(this.model.get('title'));
        this.updateQueryName();
    },
    updateQueryName: function(e) {
        this.$el.find('.button-title').html(this.getSearchTitle() + zeroWidthSpace);
        this.save();
    },
    save: function(){
        this.model.set('title', this.$el.find('input').val());
    }
});