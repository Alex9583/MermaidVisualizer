<div align="center">

<img src="src/main/resources/META-INF/pluginIcon.svg" alt="Mermaid Visualizer logo" width="96" height="96" />

# Mermaid Visualizer

**Mermaid diagrams in your JetBrains IDE — render, edit, and export without leaving the editor.**

IntelliJ plugin for [Mermaid](https://mermaid.js.org/) diagrams — live preview, code intelligence, and export, all offline.

[![JetBrains Marketplace Version](https://img.shields.io/jetbrains/plugin/v/30432.svg?label=Marketplace)](https://plugins.jetbrains.com/plugin/30432-mermaid-visualizer)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/30432.svg)](https://plugins.jetbrains.com/plugin/30432-mermaid-visualizer)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/30432.svg)](https://plugins.jetbrains.com/plugin/30432-mermaid-visualizer/reviews)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![IntelliJ Platform](https://img.shields.io/badge/IntelliJ%20Platform-2025.3+-000000?logo=intellijidea&logoColor=white)](https://plugins.jetbrains.com/docs/intellij/welcome.html)
[![JDK](https://img.shields.io/badge/JDK-21-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Build & Test](https://img.shields.io/github/actions/workflow/status/Alex9583/MermaidVisualizer/build.yml?branch=master&label=Build)](https://github.com/Alex9583/MermaidVisualizer/actions/workflows/build.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-FFDD00?logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/alextdev)

</div>

---

## Features

### Rendering & Preview
- Renders ` ```mermaid ` code blocks in the built-in Markdown preview
- Dedicated split editor for `.mmd` and `.mermaid` files with live preview
- Zoom (Ctrl+wheel), pan (click & drag), fit-to-window & 1:1 controls
- Export diagrams as SVG or PNG — copy to clipboard or save to file
- Scroll synchronization between the text editor and the preview
- Automatic dark/light theme detection and switching
- Uses the official Mermaid.js library (v11.15.0) — supports all 29+ diagram types
- Works offline — Mermaid.js is bundled, no CDN required

### Code Intelligence
- **Syntax highlighting** — keywords, diagram types, arrows, strings, comments, punctuation
- **Customizable colors** via Settings > Editor > Color Scheme > Mermaid
- **Code completion** — 29 diagram types, context-sensitive keywords, node/participant names, arrows, directives
- **Go to Definition** (Ctrl+B) — navigate to node/participant declarations
- **Find Usages** (Alt+F7) — find all references to a node across the diagram
- **Rename** (Shift+F6) — rename nodes/participants with all references updated
- **Structure View** (Alt+7) — hierarchical view of diagrams, blocks, and nodes
- **Code Folding** — fold subgraphs, loop/alt/opt/par blocks, and consecutive comments
- **Inspections & quick-fixes** — unknown diagram types (with typo suggestions), invalid arrows per diagram context, unused node declarations
- **Render error annotations** — Mermaid.js parsing errors surfaced directly in the editor

### Settings
- Settings page (Settings > Tools > Mermaid) — theme, look (classic/hand-drawn), font family, max text size, live reload delay, max preview height
- **Custom rendering colors** — optionally override the visualizer background color and the line/edge color; disabled by default, so the rendering keeps following the IDE theme unless you opt in

## Requirements

- IntelliJ IDEA 2025.3+ (or any JetBrains IDE based on build 253+)
- The bundled Markdown plugin must be enabled

## Installation

Install from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/30432-mermaid-visualizer) (or search for "Mermaid Visualizer"), or build from source.

## Development

```bash
./gradlew build              # Build the plugin
./gradlew runIde             # Launch a sandbox IDE with the plugin
./gradlew test               # Run tests
./gradlew verifyPlugin       # Verify plugin compatibility
```

## Support

If Mermaid Visualizer saves you time, consider supporting its development:

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20me%20a%20coffee-Support%20Mermaid%20Visualizer-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/alextdev)

## License

Apache 2.0 — see [LICENSE](LICENSE).