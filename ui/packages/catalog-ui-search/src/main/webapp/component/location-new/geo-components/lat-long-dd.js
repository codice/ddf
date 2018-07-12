const React = require('react');

const { Radio, RadioItem, TextField, Group } = require('../inputs');
const { ListEditor, DdEntry } = require('../inputs/list-editor');
const { DdLatitude, DdLongitude } = require('./coordinates');
const { Units } = require('../common');
const { validateDdPoint } = require('../utils');
const errorMessages = require('../utils/errors');

const minimumDifference = 0.0001;

const Point = (props) => {
    const { dd, setState } = props;
    return (
        <Group>
            <DdLatitude
                value={dd.point.latitude}
                onChange={setState((draft, value) => draft.dd.point.latitude = value)}
            />
            <DdLongitude
                value={dd.point.longitude}
                onChange={setState((draft, value) => draft.dd.point.longitude = value)}
            />
        </Group>
    );
}

const Circle = (props) => {
    const { dd, setState } = props;
    return (
        <div>
            <Group>
                <DdLatitude
                    value={dd.circle.point.latitude}
                    onChange={setState((draft, value) => draft.dd.circle.point.latitude = value)}
                />
                <DdLongitude
                    value={dd.circle.point.longitude}
                    onChange={setState((draft, value) => draft.dd.circle.point.longitude = value)}
                />
            </Group>
            <Units
                value={dd.circle.units}
                onChange={setState((draft, value) => draft.dd.circle.units = value)}
                >
                <TextField
                    label="Radius"
                    type="number"
                    value={dd.circle.radius}
                    onChange={setState((draft, value) => draft.dd.circle.radius = value)}
                />
            </Units>
        </div>
    );
};

const Line = (props) => {
    const { dd, setState } = props;
    return (
        <ListEditor
            list={dd.line.list}
            EntryType={DdEntry}
            input={dd.line.point}
            validateInput={validateDdPoint}
            onError={setState((draft, value) => {
                draft.valid = false;
                draft.error = errorMessages.invalidCoordinates;
            })}
            onAdd={setState((draft, value) => {
                draft.dd.line.list = value;
                draft.dd.line.point.latitude = '';
                draft.dd.line.point.longitude = '';
            })}
            onRemove={setState((draft, value) => draft.dd.line.list = value)}
            >
            <Group>
                <DdLatitude
                    value={dd.line.point.latitude}
                    onChange={setState((draft, value) => draft.dd.line.point.latitude = value)}
                />
                <DdLongitude
                    value={dd.line.point.longitude}
                    onChange={setState((draft, value) => draft.dd.line.point.longitude = value)}
                />
            </Group>
        </ListEditor>
    );
}

const Polygon = (props) => {
    const { dd, setState } = props;
    return (
        <ListEditor
            list={dd.polygon.list}
            EntryType={DdEntry}
            input={dd.polygon.point}
            validateInput={validateDdPoint}
            onError={setState((draft, value) => {
                draft.valid = false;
                draft.error = errorMessages.invalidCoordinates;
            })}
            onAdd={setState((draft, value) => {
                draft.dd.polygon.list = value;
                draft.dd.polygon.point.latitude = '';
                draft.dd.polygon.point.longitude = '';
            })}
            onRemove={setState((draft, value) => draft.dd.polygon.list = value)}
            >
            <Group>
                <DdLatitude
                    value={dd.polygon.point.latitude}
                    onChange={setState((draft, value) => draft.dd.polygon.point.latitude = value)}
                />
                <DdLongitude
                    value={dd.polygon.point.longitude}
                    onChange={setState((draft, value) => draft.dd.polygon.point.longitude = value)}
                />
            </Group>
        </ListEditor>
    );
};

const BoundingBox = (props) => {
    const { dd, setState } = props;
    return (
        <div>
            <DdLatitude
                label="South"
                value={dd.boundingbox.south}
                onChange={setState((draft, value) => draft.dd.boundingbox.south = value)}
            />
            <DdLatitude
                label="North"
                value={dd.boundingbox.north}
                onChange={setState((draft, value) => draft.dd.boundingbox.north = value)}
            />
            <DdLongitude
                label="West"
                value={dd.boundingbox.west}
                onChange={setState((draft, value) => draft.dd.boundingbox.west = value)}
            />
            <DdLongitude
                label="East"
                value={dd.boundingbox.east}
                onChange={setState((draft, value) => draft.dd.boundingbox.east = value)}
            />
        </div>
    );
};

const LatLongDD = (props) => {
    const { dd, setState } = props;

    const inputs = {
        point: Point,
        line: Line,
        circle: Circle,
        polygon: Polygon,
        boundingbox: BoundingBox,
    };

    const Component = inputs[dd.shape] || null;

    return (
        <div>
            <Radio value={dd.shape} onChange={setState((draft, value) => draft.dd.shape = value)}>
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

module.exports = LatLongDD;
