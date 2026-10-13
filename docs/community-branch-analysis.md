# Community branch analysis

This fork enables analysis of multiple long-lived branches in SonarQube Community Build.

## Analyze a branch

Analyze the project's main branch at least once, then pass `sonar.branch.name` for each additional branch:

```shell
sonar-scanner -Dsonar.projectKey=my-project -Dsonar.branch.name=feature/my-change
```

Each branch has its own measures, issues, quality-gate result, and analysis history. Branches can be listed, renamed,
deleted, and protected from automatic deletion through the existing `api/project_branches` web service and user
interface.

The scanner treats an existing branch as its own reference branch. A new branch uses the project's main branch as its
reference, which lets SonarQube carry issue history into the new branch.

## Scope

This initial implementation supports explicit long-lived branch analysis only. It deliberately rejects
`sonar.pullrequest.*` properties. Pull-request analysis, pull-request decoration, and CI-provider auto-detection are not
included.

The implementation is built into the fork:

- a bundled scanner extension converts `sonar.branch.name` into scanner-report branch metadata;
- the web server creates and resolves branch components when reports are submitted;
- the compute engine loads branch and reference-branch metadata while processing the report;
- the existing branch APIs and branch-aware UI are enabled.

No Java agent or replacement web archive is required.

## Compatibility

Branch support relies on internal scanner and server extension points. Rebase upgrades should run the focused branch
tests and an end-to-end scan before release. The implementation was initially ported to scanner engine 13.15 and the
SonarQube 26.11 development tree (immediately after the 26.10 tag).

The branch implementation was derived from the LGPL-3.0
[SonarQube Community Branch Plugin](https://github.com/mc1arke/sonarqube-community-branch-plugin), with its original
copyright and licensing notice recorded in `NOTICE.txt`.
