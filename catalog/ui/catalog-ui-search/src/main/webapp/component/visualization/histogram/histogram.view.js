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
    'jquery',
    'underscore',
    'marionette',
    'js/CustomElements',
    './histogram.hbs',
    'plotly.js',
    'component/property/property',
    'component/property/property.view',
    'component/singletons/metacard-definitions',
    'js/Common'
], function (wreqr, $, _, Marionette, CustomElements, template, Plotly, Property, PropertyView, metacardDefinitions, Common) {

    function calculateAvailableAttributes(results){
        var availableAttributes = [];
        results.forEach(function(result){
            availableAttributes = _.union(availableAttributes, Object.keys(result.get('metacard').get('properties').toJSON()));
        });
        return availableAttributes.filter(function(attribute){
            return metacardDefinitions.metacardTypes[attribute] !== undefined;
        });
    }

    function calculateAttributeArray(results, attribute){
        var attributes = [];
        results.forEach(function(result){
            if (metacardDefinitions.metacardTypes[attribute].multivalued){
                var resultValues = result.get('metacard').get('properties').get(attribute);
                if (resultValues) {
                    resultValues.forEach(function (value) {
                        addValueForAttributeToArray(attributes, attribute, value);
                    });
                } else {
                    addValueForAttributeToArray(attributes, attribute, resultValues);
                }
            } else {
                addValueForAttributeToArray(attributes, attribute, result.get('metacard').get('properties').get(attribute));
            }
        });
        return attributes;
    }

    function findMatchesForAttributeValues(results, attribute, values){
        return results.filter(function(result){
            if (metacardDefinitions.metacardTypes[attribute].multivalued) {
                var resultValues = result.get('metacard').get('properties').get(attribute);
                if (resultValues) {
                    for (var i = 0; i < resultValues.length; i++) {
                        if (checkIfValueIsValid(values, attribute, resultValues[i])){
                            return true;
                        }
                    }
                    return false;
                } else {
                    return checkIfValueIsValid(values, attribute, resultValues);
                }
            } else {
                return checkIfValueIsValid(values, attribute, result.get('metacard').get('properties').get(attribute));
            }
        })
    }

    function checkIfValueIsValid(values, attribute, value) {
        if (value !== undefined){
            switch(metacardDefinitions.metacardTypes[attribute].type){
                case 'DATE':
                    return values.indexOf(Common.getHumanReadableDate(value)) >= 0;
                    break;
                default:
                    return values.indexOf(value.toString()) >= 0;
                    break;
            }
        } else {
            return values.indexOf("") >= 0;
        }
    }

    function addValueForAttributeToArray(valueArray, attribute, value){
        if (value !== undefined){
            switch(metacardDefinitions.metacardTypes[attribute].type){
                case 'DATE':
                    valueArray.push(Common.getHumanReadableDate(value));
                    break;
                default:
                    valueArray.push(value.toString());
                    break;
            }
        } else {
            valueArray.push("");
        }
    }

    var layout = {
        autosize: true,
        paper_bgcolor:'rgba(0,0,0,0)',
        plot_bgcolor: 'rgba(0,0,0,0)',
        font: {
            family: '"Droid Sans","Helvetica Neue",Helvetica,Arial,sans-serif',
            size: 18,
            color: 'white'
        },
        margin: {
            l: 100,
            r: 100,
            t: 100,
            b: 200,
            pad: 20,
            autoexpand: true
        },
        barmode: 'overlay',
        xaxis: {
            fixedrange: true
        },
        yaxis: {
            fixedrange: true
        },
        showlegend: true
    };

    return Marionette.LayoutView.extend({
        tagName: CustomElements.register('histogram'),
        template: template,
        regions: {
            histogramAttribute: '.histogram-attribute'
        },
        events: {
        },
        initialize: function(){
            this.showHistogram = _.throttle(this.showHistogram, 200);
            this.handleResize = _.throttle(this.handleResize, 120);
            this.removeResizeHandler().addResizeHandler();
            this.setupListeners();
        },
        showHistogram: function(){
            if (this.histogramAttribute.currentView.getCurrentValue()[0]){
                var histogramElement = this.el.querySelector('.histogram-container');
                Plotly.newPlot(histogramElement, this.determineData(), layout, {
                    displayModeBar: false
                });
                this.handleResize();
                this.listenToHistogram();
            } else {
                this.el.querySelector('.histogram-container').innerHTML = '';
            }
        },
        showHistogramAttributeSelector: function(){
            var defaultValue = [];
            if (this.histogramAttribute.currentView) {
                defaultValue = this.histogramAttribute.currentView.getCurrentValue();
            }
            this.histogramAttribute.show(new PropertyView({
                model: new Property({
                    enumFiltering: true,
                    enum: calculateAvailableAttributes(this.options.selectionInterface.getActiveSearchResults()),
                    value: defaultValue,
                    id: 'Attribute'
                })
            }));
            this.histogramAttribute.currentView.turnOnEditing();
            this.histogramAttribute.currentView.turnOnLimitedWidth();
            $(this.histogramAttribute.currentView.el).on('change', this.showHistogram.bind(this));
        },
        onBeforeShow: function(){
            this.showHistogramAttributeSelector();
            this.showHistogram();
            this.handleEmpty();
        },
        determineData: function(){
            var activeResults = this.options.selectionInterface.getActiveSearchResults();
            var selectedResults = this.options.selectionInterface.getSelectedResults();

            return [
                {
                    x: calculateAttributeArray(activeResults, this.histogramAttribute.currentView.getCurrentValue()[0]),
                    opacity:.5,
                    type: 'histogram',
                    name: 'Hits        '
                }, {
                    x: calculateAttributeArray(selectedResults, this.histogramAttribute.currentView.getCurrentValue()[0]),
                    opacity:.5,
                    type: 'histogram',
                    name: 'Selected'
                }
            ]

        },
        handleEmpty: function(){
            this.$el.toggleClass('is-empty', this.options.selectionInterface.getActiveSearchResults().length === 0);
        },
        handleResize: function(){
            var histogramElement = this.el.querySelector('.histogram-container');
            this.$el.find('rect.drag').off('mousedown');
            if (histogramElement._context) {
                Plotly.relayout(histogramElement, {autosize: true})
            }
            this.$el.find('rect.drag').on('mousedown', function(event){
                this.shiftKey = event.shiftKey;
                this.metaKey = event.metaKey;
                this.ctrlKey = event.ctrlKey;
            }.bind(this));
        },
        addResizeHandler: function(){
            this.listenTo(wreqr.vent, 'resize', this.handleResize);
            $(window).on('resize.histogram', this.handleResize.bind(this));
            return this;
        },
        removeResizeHandler: function(){
            $(window).off('resize.histogram');
            return this;
        },
        onDestroy: function(){
            this.removeResizeHandler();
        },
        setupListeners: function(){
            this.listenTo(this.options.selectionInterface, 'reset:activeSearchResults', this.onBeforeShow);
            this.listenTo(this.options.selectionInterface.getSelectedResults(), 'update', this.showHistogram);
            this.listenTo(this.options.selectionInterface.getSelectedResults(), 'add', this.showHistogram);
            this.listenTo(this.options.selectionInterface.getSelectedResults(), 'remove', this.showHistogram);
            this.listenTo(this.options.selectionInterface.getSelectedResults(), 'reset', this.showHistogram);
        },
        listenToHistogram: function(){
            this.el.querySelector('.histogram-container').on('plotly_click', this.plotlyClickHandler.bind(this));
        },
        plotlyClickHandler: function(data){
            var alreadySelected = this.pointsSelected.indexOf(data.points[0].pointNumber) >= 0;
            if (this.shiftKey){
                this.handleShiftClick(data);
            } else if (this.ctrlKey || this.metaKey){
                this.handleControlClick(data, alreadySelected);
            } else {
                this.options.selectionInterface.clearSelectedResults();
                this.resetPointSelection();
                this.handleControlClick(data, alreadySelected);
            }
            this.resetKeyTracking();
        },
        handleControlClick: function(data, alreadySelected){
            var attributeToCheck = this.histogramAttribute.currentView.getCurrentValue()[0];
            if (alreadySelected){
                this.options.selectionInterface.removeSelectedResult(findMatchesForAttributeValues(
                    this.options.selectionInterface.getActiveSearchResults(),
                    attributeToCheck,
                    [data.points[0].x]
                ));
                this.pointsSelected.splice(this.pointsSelected.indexOf(data.points[0].pointNumber), 1);
            } else {
                this.options.selectionInterface.addSelectedResult(findMatchesForAttributeValues(
                    this.options.selectionInterface.getActiveSearchResults(),
                    attributeToCheck,
                    [data.points[0].x]
                ));
                this.pointsSelected.push(Math.max.apply(this, data.points.map(function(point){
                    return point.pointNumber;
                })));
            }
        },
        handleShiftClick: function(data, alreadySelected){
            var indexClicked = Math.max.apply(this, data.points.map(function(point){
                return point.pointNumber;
            }));
            var firstIndex = this.pointsSelected.length === 0 ? -1 : this.pointsSelected.reduce(function(currentMin, point){
                return Math.min(currentMin, point);
            }, this.pointsSelected[0]);
            var lastIndex = this.pointsSelected.length === 0 ? -1 : this.pointsSelected.reduce(function(currentMin, point){
                return Math.max(currentMin, point);
            }, this.pointsSelected[0]);
            if (firstIndex === -1 && lastIndex === -1){
                this.options.selectionInterface.clearSelectedResults();
                this.handleControlClick(data, alreadySelected);
            } else if (indexClicked <= firstIndex) {
                this.selectBetween(indexClicked, firstIndex);
            } else if (indexClicked >= lastIndex) {
                this.selectBetween(lastIndex, indexClicked + 1);
            } else {
                this.selectBetween(firstIndex, indexClicked + 1);
            }
        },
        selectBetween: function(firstIndex, lastIndex){
            var attributeToCheck = this.histogramAttribute.currentView.getCurrentValue()[0];
            var categories = this.retrieveCategoriesFromPlotly();
            var validCategories = categories.slice(firstIndex, lastIndex);
            this.options.selectionInterface.addSelectedResult(findMatchesForAttributeValues(
                this.options.selectionInterface.getActiveSearchResults(),
                attributeToCheck,
                validCategories
            ));
        },
        retrieveCategoriesFromPlotly: function(){
            // This is an internal variable for Plotly, so it might break if we update Plotly in the future.
            // Regardless, there was no other way to reliably get the categories.
            return this.el.querySelector('.histogram-container')._fullLayout.xaxis._categories;
        },
        resetKeyTracking: function(){
            this.shiftKey = false;
            this.metaKey = false;
            this.ctrlKey = false;
        },
        resetPointSelection: function(){
            this.pointsSelected = [];
        },
        shiftKey: false,
        metaKey: false,
        ctrlKey: false,
        pointsSelected: []
    });

});