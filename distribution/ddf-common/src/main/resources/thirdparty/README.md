# DDF Third-party Resources
This directory is intended as common storage for third-party projects.
The DDF default security policy allows global read access to this directory
for its running Java process, thereby providing a centralized location custom
bundles can look to for resource files.

Depending on the needs of the third-party project, the permissions on this
directory can be expanded or restricted as needed by managing the
[`default.policy`](https://github.com/codice/ddf/blob/master/distribution/ddf-common/src/main/resources/security/default.policy).

