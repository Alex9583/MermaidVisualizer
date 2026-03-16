# Changelog

All notable changes to the Mermaid Visualizer plugin will be documented in this file.

## [1.4.0] - 2026-03-16

- Update Mermaid.js library to v11.13.0 and expand supported diagram types (#6)

## [1.3.0] - 2026-03-14

- Add export (SVG/PNG), zoom/pan, and configurable rendering settings (#5)

## [1.2.0] - 2026-03-08

- Add dedicated .mmd/.mermaid file support with split editor, live preview, and syntax highlighting (#4)

## [1.1.0] - 2026-03-03

- Add write permissions to auto-release workflow (#2)

## [1.0.0] - 2026-03-02

### Added
- Mermaid diagram rendering in the Markdown preview using Mermaid.js v11.12.x
- Support for all 22+ Mermaid diagram types (flowchart, sequence, class, ER, gantt, pie, state, etc.)
- Automatic dark/light theme detection with multi-signal cascade
- Shadow DOM isolation to prevent style leakage between diagrams and Markdown content
- IncrementalDOM hook for real-time rendering as the user types
- MutationObserver fallback for environments without IncrementalDOM
- Re-entrancy protection for concurrent rendering
- Bundled mermaid.min.js for offline support (no CDN dependency)
- Resource caching for improved performance
- Graceful error handling with user-visible error messages for invalid diagrams
- Mermaid load timeout with user feedback after 10 seconds
