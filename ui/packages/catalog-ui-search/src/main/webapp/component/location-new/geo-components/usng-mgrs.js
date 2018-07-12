const React = require('react');

const { Radio, RadioItem, TextField, TextArea } = require('../inputs');
const { ListEditor, UsngEntry } = require('../inputs/list-editor');
const { UsngCoordinate } = require('./coordinates');
const { Units } = require('../common');
const { validateUsngGrid } = require('../utils');
const errorMessages = require('../utils/errors');

const Point = (props) => {
    const { usng, setState } = props;
    return (
        <UsngCoordinate
            value={usng.point}
            onChange={setState((draft, value) => draft.usng.point = value)}
        />
    );
};

const Circle = (props) => {
    const { usng, setState } = props;
    return (
        <div>
            <UsngCoordinate
                value={usng.circle.point}
                onChange={setState((draft, value) => draft.usng.circle.point = value)}
            />
            <Units
                value={usng.circle.units}
                onChange={setState((draft, value) => draft.usng.circle.units = value)}
                >
                <TextField label="Radius"
                    type="number"
                    value={usng.circle.radius}
                    onChange={setState((draft, value) => draft.usng.circle.radius = value)}
                />
            </Units>
        </div>
    )
}

const Line = (props) => {
    const { usng, setState } = props;
    return (
        <ListEditor
            list={usng.line.list}
            EntryType={UsngEntry}
            input={usng.line.point}
            validateInput={validateUsngGrid}
            onError={setState((draft, value) => {
                draft.valid = false;
                draft.error = errorMessages.invalidUsngGrid;
            })}
            onAdd={setState((draft, value) => {
                draft.usng.line.list = value;
                draft.usng.line.point = '';
            })}
            onRemove={setState((draft, value) => draft.usng.line.list = value)}
            >
            <UsngCoordinate
                value={usng.line.point}
                onChange={setState((draft, value) => draft.usng.line.point = value)}
            />
        </ListEditor>
    );
};

const Polygon = (props) => {
    const { usng, setState } = props;
    return (
        <ListEditor
            list={usng.polygon.list}
            EntryType={UsngEntry}
            input={usng.polygon.point}
            validateInput={validateUsngGrid}
            onError={setState((draft, value) => {
                draft.valid = false;
                draft.error = errorMessages.invalidUsngGrid;
            })}
            onAdd={setState((draft, value) => {
                draft.usng.polygon.list = value;
                draft.usng.polygon.point = '';
            })}
            onRemove={setState((draft, value) => draft.usng.polygon.list = value)}
            >
            <UsngCoordinate
                value={usng.polygon.point}
                onChange={setState((draft, value) => draft.usng.polygon.point = value)}
            />
        </ListEditor>
    );
};

const BoundingBox = (props) => {
    const { usng, setState } = props;
    return (
        <UsngCoordinate
            value={usng.boundingbox}
            onChange={setState((draft, value) => draft.usng.boundingbox = value)}
        />
    );
};

const USNG = (props) => {
    const { usng, setState } = props;

    const inputs = {
        point: Point,
        circle: Circle,
        line: Line,
        polygon: Polygon,
        boundingbox: BoundingBox,
    };

    const Component = inputs[usng.shape] || null;

    return (
        <div>
            <Radio value={usng.shape} onChange={setState((draft, value) => draft.usng.shape = value)}>
                <RadioItem value="point">Point</RadioItem>
                <RadioItem value="circle">Circle</RadioItem>
                <RadioItem value="line">Line</RadioItem>
                <RadioItem value="polygon">Polygon</RadioItem>
                <RadioItem value="boundingbox">Bounding Box</RadioItem>
            </Radio>
            <div className="input-location">
                {Component !== null ? <Component {...props} /> : null}
            </div>
        </div>
    );
};

module.exports = USNG;
