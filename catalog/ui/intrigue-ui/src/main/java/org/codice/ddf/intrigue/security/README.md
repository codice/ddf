# Workspace Security

The following are invariants/conditions that the security code enforces:

- All workspaces must have an owner (identified by email)
- On create, an owner will be resolved using the following:
    - provided on initial create
    - provided by subject email
    - workspace fails creation!
- Users cannot change the owner of an existing workspace
- Users can only see workspaces which:
    - they are the owner of
    - have been shared with them
- Sharing permissions can only be altered by the owner
- Owners can share by:
    - a role
    - an email address
- Owners can share:
    - read access
    - update access
    - delete access

- None of these rules apply to admin, they can do anything
