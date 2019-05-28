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

const _ = require('underscore')
const Marionette = require('marionette')

_.extend(Marionette.Region.prototype, {
  // Displays a backbone view instance inside of the region.
  // Handles calling the `render` method for you. Reads content
  // directly from the `el` attribute. Also calls an optional
  // `onShow` and `onDestroy` method on your view, just after showing
  // or just before destroying the view, respectively.
  // The `preventDestroy` option can be used to prevent a view from
  // the old view being destroyed on show.
  // The `forceShow` option can be used to force a view to be
  // re-rendered if it's already shown in the region.
  show(view, options) {
    if (!this._ensureElement()) {
      return
    }

    this._ensureViewIsIntact(view)
    Marionette.MonitorDOMRefresh(view)

    const showOptions = options || {}
    const isDifferentView = view !== this.currentView
    const preventDestroy = !!showOptions.preventDestroy
    const forceShow = !!showOptions.forceShow
    const replaceElement = !!showOptions.replaceElement

    // We are only changing the view if there is a current view to change to begin with
    const isChangingView = !!this.currentView

    // Only destroy the current view if we don't want to `preventDestroy` and if
    // the view given in the first argument is different than `currentView`
    const _shouldDestroyView = isDifferentView && !preventDestroy

    // Only show the view given in the first argument if it is different than
    // the current view or if we want to re-show the view. Note that if
    // `_shouldDestroyView` is true, then `_shouldShowView` is also necessarily true.
    const _shouldShowView = isDifferentView || forceShow

    const _shouldReplaceElement = replaceElement

    if (isChangingView) {
      this.triggerMethod('before:swapOut', this.currentView, this, options)
    }

    if (this.currentView && isDifferentView) {
      delete this.currentView._parent
    }

    if (_shouldDestroyView) {
      this.empty()

      // A `destroy` event is attached to the clean up manually removed views.
      // We need to detach this event when a new view is going to be shown as it
      // is no longer relevant.
    } else if (isChangingView && _shouldShowView) {
      this.currentView.off('destroy', this.empty, this)
    }

    if (_shouldShowView) {
      // We need to listen for if a view is destroyed
      // in a way other than through the region.
      // If this happens we need to remove the reference
      // to the currentView since once a view has been destroyed
      // we can not reuse it.
      view.once('destroy', this.empty, this)

      // make this region the view's parent,
      // It's important that this parent binding happens before rendering
      // so that any events the child may trigger during render can also be
      // triggered on the child's ancestor views
      view._parent = this
      this._renderView(view)

      if (isChangingView) {
        this.triggerMethod('before:swap', view, this, options)
      }

      this.triggerMethod('before:show', view, this, options)
      Marionette.triggerMethodOn(view, 'before:show', view, this, options)

      if (isChangingView) {
        this.triggerMethod('swapOut', this.currentView, this, options)
      }

      // An array of views that we're about to display
      const attachedRegion = Marionette.isNodeAttached(
        this.replaced ? this.currentView.el : this.el
      )

      // The views that we're about to attach to the document
      // It's important that we prevent _getNestedViews from being executed unnecessarily
      // as it's a potentially-slow method
      let displayedViews = []

      const attachOptions = _.extend(
        {
          triggerBeforeAttach: this.triggerBeforeAttach,
          triggerAttach: this.triggerAttach,
        },
        showOptions
      )

      if (attachedRegion && attachOptions.triggerBeforeAttach) {
        displayedViews = this._displayedViews(view)
        this._triggerAttach(displayedViews, 'before:')
      }

      this.attachHtml(view, _shouldReplaceElement)
      this.currentView = view

      if (attachedRegion && attachOptions.triggerAttach) {
        displayedViews = this._displayedViews(view)
        this._triggerAttach(displayedViews)
      }

      if (isChangingView) {
        this.triggerMethod('swap', view, this, options)
      }

      this.triggerMethod('show', view, this, options)
      Marionette.triggerMethodOn(view, 'show', view, this, options)

      return this
    }

    return this
  },

  // Replace the region's DOM element with the view's DOM element.
  _replaceEl(view) {
    // empty el so we don't save any non-destroyed views
    this.$el.contents().detach()

    // always restore the el to ensure the regions el is
    // present before replacing
    this._restoreEl()

    const parent = this.el.parentNode

    parent.replaceChild(view.el, this.el)
    this.replaced = true
  },

  // Restore the region's element in the DOM.
  _restoreEl() {
    if (!this.currentView) {
      return
    }

    const view = this.currentView
    const parent = view.el.parentNode

    if (!parent) {
      return
    }

    parent.replaceChild(this.el, view.el)
    this.replaced = false
  },

  // Override this method to change how the new view is
  // appended to the `$el` that the region is managing
  attachHtml(view, shouldReplace) {
    if (shouldReplace) {
      // replace the region's node with the view's node
      this._replaceEl(view)
    } else {
      // empty the node and append new view
      this.$el.contents().detach()

      this.el.appendChild(view.el)
    }
  },

  // Destroy the current view, if there is one. If there is no
  // current view, it does nothing and returns immediately.
  empty(options) {
    const view = this.currentView

    const emptyOptions = options || {}
    const preventDestroy = !!emptyOptions.preventDestroy
    // If there is no view in the region
    // we should not remove anything
    if (!view) {
      return this
    }

    view.off('destroy', this.empty, this)
    this.triggerMethod('before:empty', view)

    if (this.replaced) {
      this._restoreEl()
    }

    if (!preventDestroy) {
      this._destroyView()
    }
    this.triggerMethod('empty', view)

    // Remove region pointer to the currentView
    delete this.currentView

    if (preventDestroy) {
      this.$el.contents().detach()
    }

    return this
  },
})
