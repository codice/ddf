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
    'js/Common',
    'properties'
], function (wreqr, $, _, Marionette, CustomElements, template, Plotly, Property, PropertyView, metacardDefinitions, Common, properties) {

    var zeroWidthSpace = "\u200B";

    function calculateAvailableAttributes(results){
        var availableAttributes = [];
        results.forEach(function(result){
            availableAttributes = _.union(availableAttributes, Object.keys(result.get('metacard').get('properties').toJSON()));
        });
        return availableAttributes.filter(function(attribute){
            return metacardDefinitions.metacardTypes[attribute] !== undefined;
        }).filter(function(attribute){
            return !metacardDefinitions.isHiddenType(attribute);
        }).filter(function(attribute){
            return !properties.isHidden(attribute);
        }).map(function(attribute){
            return {
                label: metacardDefinitions.metacardTypes[attribute].alias || attribute,
                value: attribute
            };
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
                case 'BOOLEAN':
                case 'STRING':
                return values.indexOf(value.toString() + zeroWidthSpace) >= 0;
                default:
                    return value >= values[0] && value <= values[1];
            }
        }
    }

    function addValueForAttributeToArray(valueArray, attribute, value){
        if (value !== undefined){
            switch(metacardDefinitions.metacardTypes[attribute].type){
                case 'DATE':
                    valueArray.push(Common.getHumanReadableDate(value));
                    break;
                case 'BOOLEAN':
                case 'STRING':
                    valueArray.push(value.toString() + zeroWidthSpace);
                    break;
                default:
                    valueArray.push(parseFloat(value));
                    break;
            }
        }
    }

    function getIndexClicked(data){
        return Math.max.apply(this, data.points.map(function(point){
            return point.pointNumber;
        }));
    }

    function getValueFromClick(data){
        if (data.points[0].x.constructor === Number){
            var spread = data.points[0].data.xbins.size*0.5;
            return [data.points[0].x - spread, data.points[0].x + spread];
        } else {
            return [data.points[0].x];
        }
    }

    function getLayout(plot){
        var baseLayout = {
            autosize: true,
            paper_bgcolor:'rgba(0,0,0,0)',
            plot_bgcolor: 'rgba(0,0,0,0)',
            font: {
                family: '"Open Sans Light","Helvetica Neue",Helvetica,Arial,sans-serif',
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
        if (plot){
            baseLayout.xaxis.autorange = false;
            baseLayout.xaxis.range = plot._fullLayout.xaxis.range;
            baseLayout.yaxis.range = plot._fullLayout.yaxis.range;
            baseLayout.yaxis.autorange = false;
        }
        return baseLayout;
    }

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
            if (this.histogramAttribute.currentView.getCurrentValue()[0] && this.options.selectionInterface.getActiveSearchResults().length !== 0){
                var histogramElement = this.el.querySelector('.histogram-container');
                //Plotly.purge(histogramElement);
                Plotly.newPlot(histogramElement, this.determineInitialData(), getLayout(), {
                    displayModeBar: false
                }).then(function(plot){
                    Plotly.newPlot(histogramElement, this.determineData(plot), getLayout(plot), {
                        displayModeBar: false
                    });
                    this.handleResize();
                    this.listenToHistogram();
                }.bind(this));
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
                    id: 'Group by'
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
        determineInitialData: function(){
            var activeResults = this.options.selectionInterface.getActiveSearchResults();
             return [
                {
                    x: calculateAttributeArray(activeResults, this.histogramAttribute.currentView.getCurrentValue()[0]),
                    opacity: 1,
                    type: 'histogram',
                    name: 'Hits        ',
                    marker: {
                        color: 'rgba(255, 255, 255, .05)',
                        line: {
                            color: 'rgba(255,255,255,.2)',
                            width: '2'
                        }
                    }
                }
             ];
        },
        determineData: function(plot){
            var activeResults = this.options.selectionInterface.getActiveSearchResults();
            var selectedResults = this.options.selectionInterface.getSelectedResults();
            var xbins = Common.duplicate(plot._fullData[0].xbins);
            xbins.end = xbins.end + xbins.size; //https://github.com/plotly/plotly.js/issues/1229
            return [
                {
                    x: calculateAttributeArray(activeResults, this.histogramAttribute.currentView.getCurrentValue()[0]),
                    opacity: 1,
                    type: 'histogram',
                    name: 'Hits        ',
                    marker: {
                        color: 'rgba(255, 255, 255, .05)',
                        line: {
                            color: 'rgba(255,255,255,.2)',
                            width: '2'
                        }
                    },
                    autobinx: false,
                    xbins: xbins
                }, {
                    x: calculateAttributeArray(selectedResults, this.histogramAttribute.currentView.getCurrentValue()[0]),
                    opacity: 1,
                    type: 'histogram',
                    name: 'Selected',
                    marker: {
                        color: 'rgba(255, 255, 255, .2)'
                    },
                    autobinx: false,
                    xbins: xbins
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
            var indexClicked = getIndexClicked(data);
            var alreadySelected = this.pointsSelected.indexOf(indexClicked) >= 0;
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
                    getValueFromClick(data)
                ));
                this.pointsSelected.splice(this.pointsSelected.indexOf(getIndexClicked(data)), 1);
            } else {
                this.options.selectionInterface.addSelectedResult(findMatchesForAttributeValues(
                    this.options.selectionInterface.getActiveSearchResults(),
                    attributeToCheck,
                    getValueFromClick(data)
                ));
                this.pointsSelected.push(getIndexClicked(data));
            }
        },
        handleShiftClick: function(data, alreadySelected){
            var indexClicked = getIndexClicked(data);
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
            for (var i = firstIndex; i<=lastIndex; i++){
                if (this.pointsSelected.indexOf(i) === -1){
                    this.pointsSelected.push(i);
                }
            }
            var attributeToCheck = this.histogramAttribute.currentView.getCurrentValue()[0];
            var categories = this.retrieveCategoriesFromPlotly();
            var validCategories = categories.slice(firstIndex, lastIndex);
            var activeSearchResults = this.options.selectionInterface.getActiveSearchResults();
            this.options.selectionInterface.addSelectedResult(validCategories.reduce(function(results, category){
                results = results.concat(findMatchesForAttributeValues(
                    activeSearchResults,
                    attributeToCheck,
                    category.constructor === Array ? category: [category]
                ));
                return results;
            }, []));
        },
        // This is an internal variable for Plotly, so it might break if we update Plotly in the future.
        // Regardless, there was no other way to reliably get the categories.
        retrieveCategoriesFromPlotly: function(){
            var histogramElement = this.el.querySelector('.histogram-container');
            var xaxis = histogramElement._fullLayout.xaxis;
            if (xaxis._categories.length > 0){
                return xaxis._categories;
            } else {
                var xbins = histogramElement._fullData[0].xbins;
                var min = xbins.start;
                var max = xbins.end;
                var binSize = xbins.size;
                var categories = [];
                var start = min;
                while(start < max) {
                    categories.push([start, start+binSize]);
                    start+=binSize;
                }
                return categories;
            }
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