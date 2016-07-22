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

var Announcment = function (props) {
    return (
        <div className={"announcement " + props.type}>
            <div className="announcement-title">{props.title}</div>
            <div className="announcement-message">{props.message}</div>
            <div className="announcement-action is-button" onClick={props.onDismiss}>
                <span className="fa fa-times dismiss"></span>
            </div>
        </div>
    );
};

var Announcments = React.createClass({
    propTypes: {
        list: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
        dispatch: React.PropTypes.func.isRequired
    },
    render: function () {
        var dispatch = this.props.dispatch;

        var list = this.props.list.map(function (announcment) {
            return <Announcment {...announcment}
                                key={announcment.id}
                                onDismiss={function (e) {
                                    var announcementElement = $(e.currentTarget).parent();
                                    announcementElement.css('height', announcementElement[0].getBoundingClientRect().height);
                                    window.requestAnimationFrame(function(){
                                        announcementElement.addClass('is-dismissed');
                                    });
                                    setTimeout(function(){
                                        dispatch(actions.remove(announcment.id));
                                    }, 250);
                                }} />;
        });

        return <div>{list}</div>;
    }
});

module.exports = Announcments;
