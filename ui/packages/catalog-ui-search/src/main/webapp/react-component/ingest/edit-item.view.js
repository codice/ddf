import React from 'react'
import styled from '../../react-component/styles/styled-components'

const EditStyleComp = styled.div`
    display: 'flex';
    width: 40%;
    height: calc(100% - ${props => props.theme.minimumSpacing}px);
    background-color: ${props => props.theme.backgroundNavigation};
    margin: ${props => props.theme.minimumSpacing};
    padding: ${props => props.theme.minimumSpacing};
    margin-left: calc(4 * ${props => props.theme.minimumSpacing});
`

class EditItemView extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div style={{width: '1000px', height: '1000px', border: '1px solid black'}}>
                <EditStyleComp>a</EditStyleComp>
            </div>
        )
    }
}

export {EditItemView}