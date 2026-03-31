# Changes to apply to master after 2.29.x release

## Branch Protection: Migrate from legacy to ruleset

**Done on 2.29.x:**
- Created repo ruleset "2.29.x branch protection" (ID: 13192736)
  - Targets: `refs/heads/2.29.x`
  - Bypass actor: Codice Release Bot (app ID 2789451, Integration, always)
  - Rules: pull_request (2 approvals), required_status_checks (license/cla, WIP, incremental-build), required_linear_history, non_fast_forward, deletion
- Removed legacy branch protection on `2.29.x`

**TODO for master:**
- Create equivalent ruleset targeting `refs/heads/master`
  - Same rules but note: master does NOT have `required_linear_history` (2.29.x did)
  - Master has same status checks: incremental-build, WIP, license/cla
  - Add Codice Release Bot as bypass actor (not present in current master legacy protection)
- Remove legacy branch protection on `master`

## CI Workflow (`.github/workflows/ci.yml`)

**Done on gh-packages branch (commit 9f4ebf8e1d):**
- Added `packages: write` permission to the `deploy` job (top-level only has `packages: read`)

**TODO for master:**
- Merge `gh-packages` branch or cherry-pick the `packages: write` fix into master's `ci.yml`

## Release Workflow (`codice/release-pipelines`)

All changes pushed directly to `main` in `codice/release-pipelines` — these apply to all repos using the reusable workflow, no per-branch action needed.
