# Changelog

## [08-08-2026]

### Fixed
- **PPU Renderer**: Resolved a horizontal delay/jitter between the Window and Background layers when scrolling (`SCX`).
- **PPU Renderer**: Decoupled horizontal scroll logic from the Window layer to prevent pixel discarding on Window pixels.
- **PPU Renderer**: Fixed sprite visibility and positioning by migrating object fetching to screen-space coordinates.
- **PPU Renderer**: Corrected a tile detection offset in `FifoFetcher` that caused sprites to disappear or be misaligned based on the current scroll value.

### Added
- **FifoFetcher**: Introduced `windowPixelsInFifo` to track precise window rendering progress within the FIFO pipeline.
