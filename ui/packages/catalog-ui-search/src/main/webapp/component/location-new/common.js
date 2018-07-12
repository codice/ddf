const React = require('react');

const { Menu, MenuItem, Dropdown, Label, Group } = require('./inputs');

const Units = ({ value, onChange, children }) => (
    <Group>
        {children}
        <span className="input-group-btn">
            <Dropdown label={value}>
                <Menu value={value} onChange={onChange}>
                    <MenuItem value="meters" />
                    <MenuItem value="kilometers" />
                    <MenuItem value="feet" />
                    <MenuItem value="yards" />
                    <MenuItem value="miles" />
                    <MenuItem value="nautical miles" />
                </Menu>
            </Dropdown>
        </span>
    </Group>
);

module.exports = { Units };
