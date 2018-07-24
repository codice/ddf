const React = require('react');

const { Units } = require('./common');
const TextField = require('../text-field');

const Polygon = (props) => {
    const { polygon, polygonBufferWidth, polygonBufferUnits, cursor } = props;

    return (
        <div className="input-location">
            <TextField
                label="Polygon"
                value={JSON.stringify(polygon)}
                parse={JSON.parse}
                onChange={cursor('polygon')}
            />
            <Units value={polygonBufferUnits} onChange={cursor('polygonBufferUnits')}>
                <TextField 
                    type="number" 
                    label="Buffer width" 
                    min={0.000001} 
                    value={polygonBufferWidth} 
                    onChange={cursor('polygonBufferWidth')}/>
            </Units>
        </div>
    );
};

module.exports = Polygon;
