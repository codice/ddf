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
    '../menu-vertical.view',
    '../workspace/menu-vertical.workspace'
], function (Marionette, _, $, MenuVerticalView, WorkspaceMenu) {

    function hasBottomRoom(top, element){
        return ((top + element.clientHeight) < window.innerHeight);
    }

    return MenuVerticalView.extend({
        className: 'is-popout',
        updatePosition: function () {
            var clientRect = this.options.getTargetElement().getBoundingClientRect();
            var menuWidth = this.el.clientWidth;
            var necessaryLeft = Math.floor(clientRect.left + clientRect.width / 2 - menuWidth / 2);
            var necessaryTop = Math.floor(clientRect.top + clientRect.height);
            if (hasBottomRoom(necessaryTop, this.el)){
                this.$el.addClass('is-bottom').removeClass('is-top');
                this.$el.css('left', necessaryLeft).css('top', necessaryTop);
            } else {
                this.$el.addClass('is-top').removeClass('is-bottom');
                this.$el.css('left', necessaryLeft).css('top', clientRect.top);
            }
        }
    }, {
        getNewWorkspaceMenu: function (linkedModel, targetElement, name, workspaceModel) {
            return new this({
                collection: WorkspaceMenu.getNew(workspaceModel),
                linkedModel: linkedModel,
                getTargetElement: targetElement,
                name: name
            });
        }
    });
});
