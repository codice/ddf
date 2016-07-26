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
    './help.hbs',
    'js/CustomElements',
    'component/dropdown/dropdown',
    'component/dropdown/hint/dropdown.hint.view',
    'component/hint/hint'
], function (Marionette, _, $, template, CustomElements, Dropdown,
             DropdownHintView, Hint) {

    function findHighestAncestorTop(element) {
        var parent = element.parentNode;
        var maxTop = parent.getBoundingClientRect().top;
        try {
            while (parent !== null) {
                maxTop = Math.max(maxTop, parent.getBoundingClientRect().top);
                parent = parent.parentNode;
            }
        }
        catch (err) {
        }
        return maxTop;
    }

    function findHighestAncestorLeft(element) {
        var parent = element.parentNode;
        var maxTop = parent.getBoundingClientRect().left;
        try {
            while (parent !== null) {
                maxTop = Math.max(maxTop, parent.getBoundingClientRect().left);
                parent = parent.parentNode;
            }
        }
        catch (err) {
        }
        return maxTop;
    }

    function findLowestAncestorBottom(element) {
        var parent = element.parentNode;
        var maxTop = parent.getBoundingClientRect().bottom;
        try {
            while (parent !== null) {
                maxTop = Math.min(maxTop, parent.getBoundingClientRect().bottom);
                parent = parent.parentNode;
            }
        }
        catch (err) {
        }
        return maxTop;
    }

    function findLowestAncestorRight(element) {
        var parent = element.parentNode;
        var maxTop = parent.getBoundingClientRect().right;
        try {
            while (parent !== null) {
                maxTop = Math.min(maxTop, parent.getBoundingClientRect().right);
                parent = parent.parentNode;
            }
        }
        catch (err) {
        }
        return maxTop;
    }

    function findBlockers() {
        var blockingElements = $(CustomElements.getNamespace() + 'dropdown-companion.is-open')
            .add(CustomElements.getNamespace() + 'menu-vertical.is-open');
        return _.map(blockingElements, function (blockingElement) {
            return {
                boundingRect: blockingElement.getBoundingClientRect(),
                element: blockingElement
            };
        });
    }

    function hasNotScrolledPastVertically(element, boundingRect) {
        return (boundingRect.top + 1) >= findHighestAncestorTop(element);
    }

    function hasScrolledToVertically(element, boundingRect) {
        return (boundingRect.bottom - 1) <= findLowestAncestorBottom(element);
    }

    function hasNotScrolledPastHorizontally(element, boundingRect) {
        return (boundingRect.left + 1) >= findHighestAncestorLeft(element);
    }

    function hasScrolledToHorizontally(element, boundingRect) {
        return (boundingRect.right - 1) <= findLowestAncestorRight(element);
    }

    function withinScrollViewport(element, boundingRect) {
        return hasNotScrolledPastVertically(element, boundingRect) && hasScrolledToVertically(element, boundingRect)
            && hasNotScrolledPastHorizontally(element, boundingRect) && hasScrolledToHorizontally(element, boundingRect);
    }

    function isBlocked(element, boundingRect) {
        var blocked = false;
        _.forEach(findBlockers(), function (blocker) {
            if ($(blocker.element).find(element).length === 0) {
                if (((boundingRect.left > blocker.boundingRect.left)
                    && (boundingRect.left < blocker.boundingRect.right))
                    || ((boundingRect.right > blocker.boundingRect.left)
                    && (boundingRect.right < blocker.boundingRect.right))
                    || ((boundingRect.left < blocker.boundingRect.left)
                    && (boundingRect.right > blocker.boundingRect.right))) {
                    if ((boundingRect.top > blocker.boundingRect.top)
                        && (boundingRect.top < blocker.boundingRect.bottom)) {
                        blocked = true;
                    }
                    if ((boundingRect.bottom > blocker.boundingRect.top)
                        && (boundingRect.bottom < blocker.boundingRect.bottom)) {
                        blocked = true;
                    }
                }
            }
        });
        return blocked;
    }

    return new (Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('help'),
        events: {
            'mousedown': 'preventPropagation'
        },
        regions: {
            hints: '.help-hints'
        },
        initialize: function () {
            $('body').append(this.el);
        },
        onRender: function () {
        },
        hintOn: false,
        toggleHints: function () {
            if (this.hintOn) {
                this.hideHints();
                this.stopListeningForResize();
                this.stopListeningForTyping();
                this.stopListeningForClick();
            } else {
                this.showHints();
                this.listenForResize();
                this.listenForTyping();
                this.listenForClick();
            }
        },
        removeOldHints: function () {
            this.el.innerHTML = '';
            $(CustomElements.getNamespace() + 'dropdown-companion.is-hint').remove();
        },
        showHints: function () {
            this.removeOldHints();
            this.hintOn = true;
            this.$el.addClass('is-shown');
            var $elementsWithHints = $('[data-help]');
            _.each($elementsWithHints, function (element) {
                var dropdownHintView = new DropdownHintView({
                    model: new Dropdown(),
                    modelForComponent: new Hint({
                        hint: element.getAttribute('data-help')
                    })
                });
                var boundingRect = element.getBoundingClientRect();
                var top = Math.max(findHighestAncestorTop(element), boundingRect.top);
                var bottom = Math.min(findLowestAncestorBottom(element), boundingRect.bottom);
                var left = Math.max(findHighestAncestorLeft(element), boundingRect.left);
                var right = Math.min(findLowestAncestorRight(element), boundingRect.right);
                var height = bottom - top;
                var width = right - left;
                if (boundingRect.width > 0 && height > 0 && width > 0 && !isBlocked(element, boundingRect)) {
                    dropdownHintView.render();
                    this.$el.append(dropdownHintView.$el);
                    dropdownHintView.$el.css('height', height).css('width', width)
                        .css('top', top).css('left', left);
                    this.listenTo(dropdownHintView.model, 'change:isOpen', function () {
                        dropdownHintView.dropdownCompanion.componentToShow.currentView.render();
                    });
                }
            }.bind(this));
            this.addUntoggleElement();
        },
        hideHints: function () {
            this.hintOn = false;
            this.$el.removeClass('is-shown');
        },
        addUntoggleElement: function () {
            var $untoggleElement = $('.navigation-right > .item-help');
            _.forEach($untoggleElement, function (element) {
                var $untoggleElementClone = $(element).clone(true);
                this.$el.append($untoggleElementClone);
                var boundingRect = element.getBoundingClientRect();
                $untoggleElementClone.css('height', boundingRect.height).css('width', boundingRect.width)
                    .css('top', boundingRect.top).css('left', boundingRect.left)
                    .css('position', 'absolute').css('line-height', '60px')
                    .css('text-align', 'center').css('font-size', '2rem')
                    .css('overflow', 'hidden')
                    .on('click', this.toggleHints.bind(this));
            }.bind(this));
        },
        preventPropagation: function (e) {
            e.stopPropagation();
        },
        listenForResize: function () {
            $(window).on('resize.' + this.cid, _.throttle(function (event) {
                this.showHints();
            }.bind(this), 16));
        },
        stopListeningForResize: function () {
            $(window).off('resize.' + this.cid);
        },
        listenForTyping: function () {
            $(window).on('keydown.' + this.cid, function (event) {
                var code = event.keyCode;
                if (event.charCode && code == 0)
                    code = event.charCode;
                switch (code) {
                    case 27:
                        // Escape
                        this.toggleHints();
                        break;
                    default:
                        break;
                }
            }.bind(this));
        },
        stopListeningForTyping: function () {
            $(window).off('keydown.' + this.cid);
        },
        listenForClick: function () {
            this.$el.on('click.' + this.cid, function () {
                this.toggleHints();
            }.bind(this));
        },
        stopListeningForClick: function () {
            this.$el.off('click.' + this.cid);
        }
    }))();
});