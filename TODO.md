# Development TODOs

This document tracks known work that is not yet implemented. Keep completed work in the Git history or a future `CHANGELOG.md`.

## P0 — Window/HUD Horizontal Delay

### WX positioning

- [ ] Correct both `WX` calculations so the unsigned conversion occurs before subtracting `WIN_X_OFFSET`.
- [ ] Centralize the visible Window X calculation (`WX - 7`) to avoid duplicated logic.
- [ ] Verify positions for `WX = 7`, `8`, `14`, `15`, `80`, `87`, `166`, and `167`.
- [ ] Test every possible `WX % 8` remainder.

### Pixel coordinates and Window trigger

- [ ] Track the visible screen pixel independently from tile-oriented `fetchX` and FIFO-oriented `fifoX`.
- [ ] Trigger Window when the visible pixel reaches `WX - 7`, not only at an eight-pixel tile boundary.
- [ ] Track whether Window has already started on the current scanline.
- [ ] Reset that state at the beginning of every scanline.

### Background-to-Window transition

- [ ] Stop Background fetching when Window starts.
- [ ] Discard prefetched Background pixels that must not reach the LCD.
- [ ] Reset the fetcher to `OBTAIN_TILE` and start Window at tile X coordinate zero.
- [ ] Refill the shared Background/Window FIFO with Window pixels.
- [ ] Account for the Mode 3 timing penalty caused by starting Window.

## P1 — Window Vertical State

- [ ] Add an internal Window line counter.
- [ ] Increment it only on scanlines where Window actually outputs pixels.
- [ ] Reset it at frame start and when the LCD is disabled.
- [ ] Use it to select the Window tile row instead of relying only on `LY - WY`.
- [ ] Test mid-frame changes to `WY`, `WX`, and LCDC Window enable.

## P1 — Framebuffer Presentation

- [ ] Prevent `GameSurfaceView` from reading the framebuffer while the PPU writes it.
- [ ] Introduce front and back framebuffers and swap them during VBlank.
- [ ] Present only complete frames and limit the unbounded rendering loop.
- [ ] Consider rendering the framebuffer through a reusable `Bitmap` and `Paint`.

## P2 — Sprite FIFO and Pixel Mixing

These tasks improve sprite accuracy but do not cause the Window delay.

- [ ] Implement the existing `spriteFifo` as a real OBJ pixel queue.
- [ ] Store color, palette, priority, and source metadata in FIFO entries.
- [ ] Move BG/Window–OBJ priority resolution to the pixel output stage.
- [ ] Implement transparent OBJ color zero and DMG/CGB priority rules.
- [ ] Account for sprite-fetch Mode 3 penalties.

## Regression Tests

- [ ] Add unit tests for unsigned `WX - 7` conversion.
- [ ] Build a test scene with identical Background and Window tilemaps.
- [ ] Scroll `SCX` one pixel at a time in both directions and compare layer alignment.
- [ ] Repeat with vertical and diagonal movement.
- [ ] Confirm every scanline outputs exactly 160 visible pixels.
- [ ] Compare consecutive frame captures for tile jumps, lag, and tearing.
