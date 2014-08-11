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
        '/applications/js/view/features/FeatureRow.view',
        'text!featureTemplate',
        'datatables',
        'icanhaz'
],
    function(Marionette, _, $, FeatureRowView, FeaturesTemplate, ich){
        'use strict';

        if(!ich.featureTemplate) {
            ich.addTemplate('featureTemplate', FeaturesTemplate);
        }

        var FeaturesView = Marionette.CompositeView.extend({
            template: FeaturesTemplate,
            itemViewContainer: 'tbody',
            itemView: FeatureRowView,

            events: {
                'click .sort li a': 'onSort',
                'click .filter-container': 'onShowFilter',
                'click .sort-container': 'onShowSort'
            },

            initialize: function (options) {
                _.bindAll(this, 'onCompositeCollectionRendered', 'onShowFilter', 'onShowSort', 'onSort', 'onFilter');

                // If a selected model is passed in as an option, cache it for use on render.
                if (options.selectedModel) {
                    this.selectedModel = options.selectedModel;
                }
            },

            onRender: function () {
                /*
                // remove any listeners that may or may not yet exist
                this.$('#filter-features').off('keyup', this.onFilter);

                // Begin listening to the filter input
                this.$('#filter-features').on('keyup', this.onFilter);

                // Check to see if we need to select a specific row in the table
                if (!(_.isUndefined(this.selectedModel))) {
                    var selectedView = this.children.findByModel(this.selectedModel);
                    if (selectedView) {
                        selectedView.trigger('selected', selectedView.model);
                    }

                    // Reset the selectedModel attribute so future renders do not utilize it un-expectedly
                    this.selectedModel = undefined;
                }
                */
            },

            onCompositeCollectionRendered: function () {
                this.$('#features-table').dataTable({
                    bLengthChange: false,
                    bSortClasses: false,
                    sDom: 't<"table-footer"ip>',
                    oLanguage: {
                        oPaginate: {
                            sFirst: '',
                            sLast: '',
                            sNext: '',
                            sPrevious: ''
                        },
                        sSearch: 'Filter:',
                        sInfo: '_START_ - _END_ of _TOTAL_'
                    },
                    bSort: true,
                    iDisplayLength: 10
                }).fnSort([[0, 'asc'], [1, 'asc']]);
            },

            onSort: function (event) {
                var target = $(event.currentTarget);
                var table = this.$('#features-table').dataTable();
                var caret = this.$('.btn-dropdown span.glyphicon-chevron-down');

                event.stopPropagation();

                this.$('.btn-dropdown').text(target.text()).append(caret);
                this.$('.dropdown-toggle').dropdown('toggle');

                switch (target.attr('data-id')) {
                    case 'ln_ph_asc':
                        table.fnSort([[0, 'asc'], [3, 'asc']]);
                        break;
                    case 'ln_ph_desc':
                        table.fnSort([[0, 'desc'], [3, 'desc']]);
                        break;
                    case 'ln_asc':
                        table.fnSort([[0, 'asc']]);
                        break;
                    case 'ln_desc':
                        table.fnSort([[0, 'desc']]);
                        break;
                    default:
                        table.fnSort([[0, 'asc'], [1, 'asc']]);
                        break;
                }
            },

            onShowFilter: function (event) {
                var target = $(event.currentTarget);

                if (target.hasClass('inactive')) {
                    event.stopPropagation();
                    target.removeClass('inactive');

                    this.$('.sort-container').addClass('inactive');
                }
            },

            onShowSort: function (event) {
                var target = $(event.currentTarget);

                if (target.hasClass('inactive')) {
                    event.stopPropagation();
                    target.removeClass('inactive');

                    this.$('.filter-container').addClass('inactive');
                }
            },

            onFilter: function (event) {
                var table = this.$('#features-table').dataTable();
                var filterText = this.$('#filter-features').val();

                event.stopPropagation();

                table.fnFilter(filterText, null);
            }

        });

        return FeaturesView;
    }
);