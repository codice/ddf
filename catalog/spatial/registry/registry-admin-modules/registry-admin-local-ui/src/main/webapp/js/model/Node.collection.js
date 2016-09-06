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
/*jshint -W024*/
define([
    'backbone',
    './Node'
], function (Backbone, Node) {

    return Backbone.Collection.extend({
        model: Node.Model,
        comparator: function(model){
            var fedNode = model.getObjectOfType('urn:registry:federation:node');
            if (fedNode !== null && fedNode.length > 0) {
                return fedNode[0].Name;
            }
        }
    });
});