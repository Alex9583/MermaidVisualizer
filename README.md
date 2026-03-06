# Mermaid Visualizer

IntelliJ plugin that renders [Mermaid](https://mermaid.js.org/) diagrams directly in the Markdown preview.

## Features

- Renders ` ```mermaid ` code blocks in the built-in Markdown preview
- Uses the official Mermaid.js library (v11.12.x) — supports all 22+ diagram types
- Automatic dark/light theme detection and switching
- Works offline — Mermaid.js is bundled, no CDN required
- Runs in the IDE's embedded JCEF browser with `securityLevel: 'strict'`

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
