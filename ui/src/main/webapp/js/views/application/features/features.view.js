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
        './featureRow.view',
        'text!featureTemplate',
        'icanhaz',
        'datatables'
],
    function(Marionette, _, $, FeatureRowView, FeaturesTemplate, ich){
        'use strict';

        if(!ich.featureTemplate) {
            ich.addTemplate('featureTemplate', FeaturesTemplate);
        }

        var FeaturesView = Marionette.CompositeView.extend({
            template: 'featureTemplate',
            itemViewContainer: 'tbody',
            itemView: FeatureRowView,

            onCompositeCollectionRendered: function () {
                this.$('#features-table').dataTable({
                    sDom: 't<"table-footer"ip>', // not sure why but this get rid the search box up top.
                    bSort: true,
                    bPaginate: false,
                    aoColumnDefs: [
                        {
                            bSortable: false,
                            aTargets: [ 2 ]
                        }
                    ]
                }).fnSort([[0, 'asc'], [1, 'asc']]);
            }



        });

        return FeaturesView;
    }
);