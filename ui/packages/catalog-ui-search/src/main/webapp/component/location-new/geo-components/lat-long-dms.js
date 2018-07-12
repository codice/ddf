const React = require('react');

const { Radio, RadioItem, TextField, Group } = require('../inputs');
const { ListEditor, DmsEntry } = require('../inputs/list-editor');
const { DmsLatitude, DmsLongitude } = require('./coordinates');
const { Units } = require('../common');
const { validateDmsPoint } = require('../utils');
const Direction = require('./direction');
const errorMessages = require('../utils/errors');

const latitudeDirections = ['N', 'S'];
const longitudeDirections = ['E', 'W'];

const Point = (props) => {
    const { dms, setState } = props;
    return (
        <Group>
            <DmsLatitude
                value={dms.point.latitude.coordinate}
                onChange={setState((draft, value) => draft.dms.point.latitude.coordinate = value)}
                >
                <Direction
                    options={latitudeDirections}
                    value={dms.point.latitude.direction}
                    onChange={setState((draft, value) => draft.dms.point.latitude.direction = value)}
                />
            </DmsLatitude>
            <DmsLongitude
                value={dms.point.longitude.coordinate}
                onChange={setState((draft, value) => draft.dms.point.longitude.coordinate = value)}
                >
                <Direction
                    options={longitudeDirections}
                    value={dms.point.longitude.direction}
                    onChange={setState((draft, value) => draft.dms.point.longitude.direction = value)}
                />
            </DmsLongitude>
        </Group>
    );
}

const Circle = (props) => {
    const { dms, setState } = props;
    return (
        <div>
            <Group>
                <DmsLatitude
                    value={dms.circle.point.latitude.coordinate}
                    onChange={setState((draft, value) => draft.dms.circle.point.latitude.coordinate = value)}
                    >
                    <Direction
                        options={latitudeDirections}
                        value={dms.circle.point.latitude.direction}
                        onChange={setState((draft, value) => draft.dms.circle.point.latitude.direction = value)}
                    />
                </DmsLatitude>
                <DmsLongitude
                    value={dms.circle.point.longitude.coordinate}
                    onChange={setState((draft, value) => draft.dms.circle.point.longitude.coordinate = value)}
                    >
                    <Direction
                        options={longitudeDirections}
                        value={dms.circle.point.longitude.direction}
                        onChange={setState((draft, value) => draft.dms.circle.point.longitude.direction = value)}
                    />
                </DmsLongitude>
            </Group>
            <Units
                value={dms.circle.units}
                onChange={setState((draft, value) => draft.dms.circle.units = value)}
                >
                <TextField
                    label="Radius"
                    type="number"
                    value={dms.circle.radius}
                    onChange={setState((draft, value) => draft.dms.circle.radius = value)}
                />
            </Units>
        </div>
    );
};

const Line = (props) => {
    const { dms, setState } = props;
    return (
        <ListEditor
            list={dms.line.list}
            EntryType={DmsEntry}
            input={dms.line.point}
            validateInput={validateDmsPoint}
            onError={setState((draft, value) => {
                draft.valid = false;
                draft.error = errorMessages.invalidCoordinates;
            })}
            onAdd={setState((draft, value) => {
                draft.dms.line.list = value;
                draft.dms.line.point.latitude.coordinate = '';
                draft.dms.line.point.longitude.coordinate = '';
            })}
            onRemove={setState((draft, value) => draft.dms.line.list = value)}
            >
            <Group>
                <DmsLatitude
                    value={dms.line.point.latitude.coordinate}
                    onChange={setState((draft, value) => draft.dms.line.point.latitude.coordinate = value)}
                    >
                    <Direction
                        options={latitudeDirections}
                        value={dms.line.point.latitude.direction}
                        onChange={setState((draft, value) => draft.dms.line.point.latitude.direction = value)}
                    />
                </DmsLatitude>
                <DmsLongitude
                    value={dms.line.point.longitude.coordinate}
                    onChange={setState((draft, value) => draft.dms.line.point.longitude.coordinate = value)}
                    >
                    <Direction
                        options={longitudeDirections}
                        value={dms.line.point.longitude.direction}
                        onChange={setState((draft, value) => draft.dms.line.point.longitude.direction = value)}
                    />
                </DmsLongitude>
            </Group>
        </ListEditor>
    );
};

const Polygon = (props) => {
    const { dms, setState } = props;
    return (
        <ListEditor
            list={dms.polygon.list}
            EntryType={DmsEntry}
            input={dms.polygon.point}
            validateInput={validateDmsPoint}
            onError={setState((draft, value) => {
                draft.valid = false;
                draft.error = errorMessages.invalidCoordinates;
            })}
            onAdd={setState((draft, value) => {
                draft.dms.polygon.list = value;
                draft.dms.polygon.point.latitude.coordinate = '';
                draft.dms.polygon.point.longitude.coordinate = '';
            })}
            onRemove={setState((draft, value) => draft.dms.polygon.list = value)}
            >
            <Group>
                <DmsLatitude
                    value={dms.polygon.point.latitude.coordinate}
                    onChange={setState((draft, value) => draft.dms.polygon.point.latitude.coordinate = value)}
                    >
                    <Direction
                        options={latitudeDirections}
                        value={dms.polygon.point.latitude.direction}
                        onChange={setState((draft, value) => draft.dms.polygon.point.latitude.direction = value)}
                    />
                </DmsLatitude>
                <DmsLongitude
                    value={dms.polygon.point.longitude.coordinate}
                    onChange={setState((draft, value) => draft.dms.polygon.point.longitude.coordinate = value)}
                    >
                    <Direction
                        options={longitudeDirections}
                        value={dms.polygon.point.longitude.direction}
                        onChange={setState((draft, value) => draft.dms.polygon.point.longitude.direction = value)}
                    />
                </DmsLongitude>
            </Group>
        </ListEditor>
    );
};

const BoundingBox = (props) => {
    const { dms, setState } = props;
    return (
        <div>
            <DmsLatitude
                label='South'
                value={dms.boundingbox.south.coordinate}
                onChange={setState((draft, value) => draft.dms.boundingbox.south.coordinate = value)}
                >
                <Direction
                    options={latitudeDirections}
                    value={dms.boundingbox.south.direction}
                    onChange={setState((draft, value) => draft.dms.boundingbox.south.direction = value)}
                />
            </DmsLatitude>
            <DmsLatitude
                label='North'
                value={dms.boundingbox.north.coordinate}
                onChange={setState((draft, value) => draft.dms.boundingbox.north.coordinate = value)}
                >
                <Direction
                    options={latitudeDirections}
                    value={dms.boundingbox.north.direction}
                    onChange={setState((draft, value) => draft.dms.boundingbox.north.direction = value)}
                />
            </DmsLatitude>
            <DmsLongitude
                label='West'
                value={dms.boundingbox.west.coordinate}
                onChange={setState((draft, value) => draft.dms.boundingbox.west.coordinate = value)}
                >
                <Direction
                    options={longitudeDirections}
                    value={dms.boundingbox.west.direction}
                    onChange={setState((draft, value) => draft.dms.boundingbox.west.direction = value)}
                />
            </DmsLongitude>
            <DmsLongitude
                label='East'
                value={dms.boundingbox.east.coordinate}
                onChange={setState((draft, value) => draft.dms.boundingbox.east.coordinate = value)}
                >
                <Direction
                    options={longitudeDirections}
                    value={dms.boundingbox.east.direction}
                    onChange={setState((draft, value) => draft.dms.boundingbox.east.direction = value)}
                />
            </DmsLongitude>
        </div>
    );
};

const LatLongDMS = (props) => {
    const { dms, setState } = props;

    const inputs = {
        point: Point,
        line: Line,
        circle: Circle,
        polygon: Polygon,
        boundingbox: BoundingBox,
    };

    const Component = inputs[dms.shape] || null;

    return (
        <div>
            <Radio value={dms.shape} onChange={setState((draft, value) => draft.dms.shape = value)}>
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

module.exports = LatLongDMS;
