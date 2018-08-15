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
    'js/CustomElements',
    'js/store',
    'component/unsaved-indicator/workspace/workspace-unsaved-indicator.view',
    'react',
    'behaviors/region.behavior'
], function (Marionette, _, CustomElements, store, UnsavedIndicatorView, React) { 

    return Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.get('content');
        },
        behaviors() {
            const view = this;
            return {
                region: {
                    regions: [
                        {
                            selector: '.title-saved',
                            view() {
                                if (view.model.get('currentWorkspace')) {
                                    return UnsavedIndicatorView;
                                }   
                                return undefined;
                            },
                            viewOptions() {
                                return {
                                    model: view.model.get('currentWorkspace')
                                };
                            },
                            shouldRegionUpdate(currentView) {
                                if (currentView && currentView.model !== view.model.get('currentWorkspace')) {
                                    return true;
                                }
                                return false;
                            }
                        }
                    ]
                }
            }
        },
        template(data) {
            return (
                <React.Fragment key={data.currentWorkspace ? data.currentWorkspace.id : 0}>
                    <input placeholder="Workspace Title" value={data.currentWorkspace ? data.currentWorkspace.title : ''}
                     data-help="This is the title of the workspace you are currently in.
                    If you have permission, you can click here to start editing the title." onChange={this.updateWorkspaceName.bind(this)}/>
                    <pre className="title-display">{data.currentWorkspace ? data.currentWorkspace.title : ''}</pre>
                    <div className="title-saved"></div>
                </React.Fragment>
            )
        },
        tagName: CustomElements.register('content-title'),
        initialize: function (options) {
            if (options.model === undefined){
                this.setDefaultModel();
            }
            this.listenTo(this.model, 'change:currentWorkspace', this.render);
        },
        updateWorkspaceName: function(e){
            this.model.get('currentWorkspace').set('title', e.currentTarget.value);
        }
    });
});
