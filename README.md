# Mermaid Visualizer

IntelliJ plugin for [Mermaid](https://mermaid.js.org/) diagrams — live preview, code intelligence, and export, all offline.

## Features

### Rendering & Preview
- Renders ` ```mermaid ` code blocks in the built-in Markdown preview
- Dedicated split editor for `.mmd` and `.mermaid` files with live preview
- Zoom (Ctrl+wheel), pan (click & drag), fit-to-window & 1:1 controls
- Export diagrams as SVG or PNG — copy to clipboard or save to file
- Scroll synchronization between the text editor and the preview
- Automatic dark/light theme detection and switching
- Uses the official Mermaid.js library (v11.14.0) — supports all 27+ diagram types
- Works offline — Mermaid.js is bundled, no CDN required

### Code Intelligence
- **Syntax highlighting** — keywords, diagram types, arrows, strings, comments, punctuation
- **Customizable colors** via Settings > Editor > Color Scheme > Mermaid
- **Code completion** — 27 diagram types, context-sensitive keywords, node/participant names, arrows, directives
- **Go to Definition** (Ctrl+B) — navigate to node/participant declarations
- **Find Usages** (Alt+F7) — find all references to a node across the diagram
- **Rename** (Shift+F6) — rename nodes/participants with all references updated
- **Structure View** (Alt+7) — hierarchical view of diagrams, blocks, and nodes
- **Code Folding** — fold subgraphs, loop/alt/opt/par blocks, and consecutive comments
- **Inspections & quick-fixes** — unknown diagram types (with typo suggestions), invalid arrows per diagram context, unused node declarations
- **Render error annotations** — Mermaid.js parsing errors surfaced directly in the editor

### Settings
- Settings page (Settings > Tools > Mermaid) — theme, look (classic/hand-drawn), font family, max text size, live reload delay

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

## License

Apache 2.0
