const React = require('react');

const { Units } = require('./common');
const TextField = require('../text-field');

const Line = (props) => {
    const { line, lineWidth, lineUnits, cursor } = props;

    return (
        <div className="input-location">
            <TextField
                label="Line"
                value={JSON.stringify(line)}
                parse={JSON.parse}
                onChange={cursor('line')}
            />
            <Units value={lineUnits} onChange={cursor('lineUnits')}>
                <TextField
                    type="number"
                    label="Width"
                    min={0.000001}
                    value={lineWidth}
                    onChange={cursor('lineWidth')}
                />
            </Units>
        </div>
    );
};

module.exports = Line;
