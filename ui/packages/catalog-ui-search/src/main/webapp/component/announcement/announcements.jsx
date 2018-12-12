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

var React = require('react');
var actions = require('./actions');
var $ = require('jquery');
var connect = require('react-redux').connect;

var dim = function (Component) {
    return class extends React.Component {
         constructor(props) {
             super(props)
             this.state = { rect: {} }
         }
        componentWillReceiveProps() {
            this.setState({ rect: this.ref.getBoundingClientRect() });
        }
        render() {
            return (
                <div ref={(ref) => { this.ref = ref }}>
                    <Component {...this.props} boundingRect={this.state.rect} />
                </div>
            );
        }
    };
};

var Announcment = dim(function (props) {
    var classes = props.removing ? 'announcement is-dismissed' : 'announcement';
    var height = props.boundingRect.height || 'auto';

    return (
        <div style={{ height: height }} className={classes + ' ' + props.type}>
            <div className='announcement-title'>{props.title}</div>
            <div className='announcement-message'>{Array.isArray(props.message) ? props.message.map((submessage) => <div key={submessage}>{submessage}</div>) : props.message}</div>
            <div className='announcement-action is-button' onClick={props.onDismiss}>
                <span className='fa fa-times dismiss'></span>
            </div>
        </div>
    );
});

var Announcments = function (props) {
    var dismiss = function (id) {
        return function () { props.onDismiss(id) };
    };

    var list = props.list.map(function (announcment) {
        return (
            <Announcment
                {...announcment}
                key={announcment.id}
                onDismiss={dismiss(announcment.id)} />
        );
    });

    return <div>{list}</div>;
};

module.exports = connect(function (state) {
    return {
        list: state
    }
}, {
    onDismiss: actions.remove
})(Announcments)
