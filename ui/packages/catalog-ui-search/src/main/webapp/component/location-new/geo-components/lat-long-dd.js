const React = require('react');

const Group = require('react-component/group');
const { Radio, RadioItem } = require('react-component/radio');
const TextField = require('react-component/text-field');
const { Units } = require('react-component/location/common');
const ListEditor = require('../inputs/list-editor');
const { DdLatitude, DdLongitude } = require('./coordinates');
const { validateDdPoint } = require('../utils');
const { ddPoint } = require('../models');
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
};

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
    const points = dd.line.list.map((entry, index) =>
        <Group key={index}>
            <DdLatitude
                value={dd.line.list[index].latitude}
                onChange={setState((draft, value) => draft.dd.line.list[index].latitude = value)}
            />
            <DdLongitude
                value={dd.line.list[index].longitude}
                onChange={setState((draft, value) => draft.dd.line.list[index].longitude = value)}
            />
        </Group>
    );

    return (
        <ListEditor
            list={dd.line.list}
            defaultItem={ddPoint}
            onChange={setState((draft, value) => draft.dd.line.list = value)}
            >
            {points}
        </ListEditor>
    );
};

const Polygon = (props) => {
    const { dd, setState } = props;
    const points = dd.polygon.list.map((entry, index) =>
        <Group key={index}>
            <DdLatitude
                value={dd.polygon.list[index].latitude}
                onChange={setState((draft, value) => draft.dd.polygon.list[index].latitude = value)}
            />
            <DdLongitude
                value={dd.polygon.list[index].longitude}
                onChange={setState((draft, value) => draft.dd.polygon.list[index].longitude = value)}
            />
        </Group>
    );

    return (
        <ListEditor
            list={dd.polygon.list}
            defaultItem={ddPoint}
            onChange={setState((draft, value) => draft.dd.polygon.list = value)}
            >
            {points}
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
