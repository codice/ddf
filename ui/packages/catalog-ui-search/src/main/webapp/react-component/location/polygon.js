const React = require('react');

const TextField = require('../text-field');

const Polygon = (props) => {
    const { polygon, cursor } = props;

    return (
        <div className="input-location">
            <TextField
                label="Polygon"
                value={JSON.stringify(polygon)}
                parse={JSON.parse}
                onChange={cursor('polygon')}
            />
        </div>
    );
};

module.exports = Polygon;
