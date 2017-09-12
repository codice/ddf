# Workspace Security

The following are invariants/conditions that the workspace security code enforces:

- The system user has complete access to all workspaces
    - the system user can be configured

- All workspaces must have an owner
    - stored as the `metacard.owner` attribute on the workspace metacard
    - a workspace cannot lose the owner attribute, however it can be changed
    - owner always has complete access to a workspace

- On create, an owner will be resolved using the following:
    - provided on initial create in the metacard create request
        - this is how an external system can create metacards on behalf of other users
    - provided by current thread context subject
        - by default the `http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress`
          subject attribute is the owner identifier
        - the owner identifier can be configured to any subject attribute to help in scenarios
          where emails are not guaranteed to be available
    - workspace fails creation and the admin is alerted of the failure

- Users can only see workspaces which:
    - they are the owner of
    - have been shared with them through the following attributes:
        - `security.access-groups` allows for a role based sharing
        - `security.access-individuals` allows for direct user sharing
    - sharing permissions can only be altered by the system or the owner
