/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import * as React from 'react';
import WorkspacesTemplates from '../../presentation/workspaces-templates'

const store = require('js/store');
const LoadingView = require('component/loading/loading.view');
const wreqr = require('wreqr')
const Property = require('component/property/property');
const properties = require('properties');

interface Props {
    closeTemplates: () => void;
    hasUnsaved: boolean;
    hasTemplatesExpanded: boolean;
    toggleExpansion: () => void;
}

interface State {
    adhocModel: any
}

class WorkspacesTemplatesContainer extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = {
            adhocModel: new Property({
                value: [''],
                label: '',
                type: 'STRING',
                showValidationIssues: false,
                showLabel: false,
                placeholder: 'Search ' + properties.branding + ' ' + properties.product,
                isEditing: true
            })
        }
    }
    startAdhocSearch() {
        this.prepForCreateNewWorkspace();
        store.get('workspaces').createAdhocWorkspace(this.state.adhocModel.getValue()[0]);
    }
    prepForCreateNewWorkspace() {
        var loadingview = new LoadingView();
        store.get('workspaces').once('sync', function(workspace: any){
            loadingview.remove();
            wreqr.vent.trigger('router:navigate', {
                fragment: 'workspaces/'+workspace.id,
                options: {
                    trigger: true
                }
            });
        });
        this.props.closeTemplates();
    }
    createWorkspace() {
        this.prepForCreateNewWorkspace();
        store.get('workspaces').createWorkspace();
    }
    createLocalWorkspace() {
        this.prepForCreateNewWorkspace();
        store.get('workspaces').createLocalWorkspace();
    }
    createAllWorkspace() {
        this.prepForCreateNewWorkspace();
        store.get('workspaces').createAllWorkspace();
    }
    createGeoWorkspace() {
        this.prepForCreateNewWorkspace();
        store.get('workspaces').createGeoWorkspace();
    }
    createLatestWorkspace() {
        this.prepForCreateNewWorkspace();
        store.get('workspaces').createLatestWorkspace();
    }
    render() {
        return (
            <WorkspacesTemplates 
                createWorkspace={this.createWorkspace.bind(this)}
                createLocalWorkspace={this.createLocalWorkspace.bind(this)}
                createAllWorkspace={this.createAllWorkspace.bind(this)}
                createGeoWorkspace={this.createGeoWorkspace.bind(this)}
                createLatestWorkspace={this.createLatestWorkspace.bind(this)}
                hasTemplatesExpanded={this.props.hasTemplatesExpanded}
                toggleExpansion={this.props.toggleExpansion}
                startAdhocSearch={this.startAdhocSearch.bind(this)}
                adhocModel={this.state.adhocModel}
            />
        )
    }
}

export default WorkspacesTemplatesContainer