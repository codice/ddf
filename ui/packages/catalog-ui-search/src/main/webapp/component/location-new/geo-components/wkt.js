const React = require('react');
const { Units } = require('../common');
const { TextField } = require('../inputs');

const WKT = (props) => {
    const { wkt, setState } = props;

    return (
        <div className="input-location">
            <TextField
                value={wkt}
                onChange={setState((draft, value) => draft.wkt = value)}
            />
        </div>
    );
};

module.exports = WKT;
