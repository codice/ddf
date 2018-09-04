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
import * as React from "react";
import styled from "../../styles/styled-components";
import { ChangeBackground } from '../../styles/mixins/change-background';
import MarionetteRegionContainer from '../../container/marionette-region-container';
const SourceItemCollectionView = require('component/source-item/source-item.collection.view');
const sources = require('component/singletons/sources-instance');
const SourcesSummaryView = require('component/sources-summary/sources-summary.view');

const Root = styled<{}, 'div'>('div')`
    display: block;
    height: 100%;
    width: 100%;
    overflow: hidden;
    ${props => {
        return ChangeBackground(props.theme.backgroundContent);
    }}
`

const SourcesCenter = styled<{}, 'div'>('div')`
    margin: auto;
    max-width: ${props => {
        return props.theme.screenBelow(props.theme.mediumScreenSize) ? '100%' : '1200px';
    }};
    padding: 0px ${props => props.theme.screenBelow(props.theme.mediumScreenSize) ? '20px' : '100px'};
    overflow: auto;
    height: 100%;
`

export default ({}: {}) => {
    return (
        <Root>
            <SourcesCenter>
                <div>
                    <MarionetteRegionContainer 
                        view={SourcesSummaryView}
                        replaceElement
                    />
                </div>
                <div>
                    <MarionetteRegionContainer 
                        view={SourceItemCollectionView}
                        viewOptions={() => {
                            return {
                                collection: sources
                            }
                        }}
                        replaceElement
                    />
                </div>
            </SourcesCenter>
        </Root>
    )
}