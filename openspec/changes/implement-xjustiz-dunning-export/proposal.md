# Proposal: Implement XJustiz Dunning Export

**Change ID:** `implement-xjustiz-dunning-export`
**Status:** Proposed
**Created:** 2025-11-11

## Summary

Implement electronic court dunning procedure (elektronisches Mahnverfahren) by adding XJustiz EDA file export capability for ClaimLedger (Forderungskonto). This enables users to generate court dunning applications in standardized XML format according to the XJustiz 3.5.1 "Mahn" schema.

## Motivation

German courts require dunning applications (Mahnanträge) to be submitted electronically in XJustiz format. The existing ClaimLedger feature tracks claims and payments but cannot export data in the required format for court submission. This creates manual work and potential errors when lawyers need to file court dunning procedures.

## Goals

- Generate XJustiz-compliant EDA (Elektronischer Dokument-Austausch) files from ClaimLedger data
- Map ClaimLedger components (Hauptforderung, Kosten, Zinsen) to XJustiz dunning schema elements
- Provide export functionality in both desktop client and REST API
- Support XJustiz 3.5.1 "Mahn" schema (xjustiz_0600_mahn_3_3.xsd)
- Enable validation of generated XML against XJustiz schema
- Provide human-readable viewer for EDA files in document viewer with dual display (formatted view + raw XML)

## Non-Goals

- Automated reminder generation workflow (existing invoice reminder statuses handle this)
- Direct court system integration (file submission remains manual)
- Support for other XJustiz schemas beyond dunning
- beA (besonderes elektronisches Anwaltspostfach) integration

## Scope

This change introduces one new capability:

1. **XJustiz EDA Export** - Generate court dunning application XML files from ClaimLedger data and provide integrated viewer for EDA files in the document panel

## Success Criteria

- ClaimLedger export produces valid XJustiz XML that passes schema validation
- Desktop client provides UI action to export ClaimLedger as XJustiz file
- REST API endpoint enables programmatic export
- Generated files are accepted by German court dunning systems
- Export includes all required elements: claimant, debtor, claim amounts, interest calculations
- EDA file viewer displays formatted, human-readable content in ArchiveFilePanel document viewer
- Viewer provides tabbed interface with formatted view and raw XML view
- Viewer integrates seamlessly with existing document viewing infrastructure

## Dependencies

- Existing XJustiz 3.5.1 XSD schemas in `j-lawyer-server-common/xjustiz/`
- Existing ClaimLedger domain model
- JAXB code generation via xjc (Ant-based, uses JDK tools)
- Existing document viewer framework in ArchiveFilePanel
- AppUserBean extension to store lawyer identification number (requires DB migration)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Complex XJustiz schema mapping | High | Start with minimal required fields, iterate based on validation feedback |
| Missing ClaimLedger data for required XJustiz elements | Medium | Provide clear validation messages, document required ClaimLedger setup |
| XJustiz schema version updates | Low | Design modular XML generation to support future schema versions |

## Related Changes

None - this is an independent new feature.

## Alternatives Considered

1. **Manual XML creation** - Too error-prone and time-consuming for users
2. **Third-party service integration** - Adds external dependency and potential data privacy concerns
3. **PDF form generation** - Courts increasingly require structured XML, not PDF forms
