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
    '../dropdown.view',
    './dropdown.query-src.hbs',
    'component/query-src/query-src.view',
    'component/singletons/sources-instance'
], function (Marionette, _, $, DropdownView, template, ComponentView, sources) {

    return DropdownView.extend({
        template: template,
        className: 'is-querySrc',
        componentToShow: ComponentView,
        initializeComponentModel: function(){
            //override if you need more functionality
            this.modelForComponent = this.model;
        },
        isCentered: true,
        getCenteringElement: function(){
            return this.el.querySelector('.dropdown-text');
        },
        hasTail: true,
        serializeData: function(){
            var srcs = this.model.get('value');
            return sources.toJSON().filter(function(src){
                return srcs.indexOf(src.id) !== -1;
            });
        }
    });
});
