# Toast Lab manual test plan

Run these checks on physical Android and iOS devices after `native:plugin:validate`, `native:install --force`, and successful platform compilation.

- Basic: neutral, success, error, warning, and info.
- Content: message-only, title/message, long wrapping text, icon, no icon, action, and close control.
- Position: top, center, and bottom with cutouts, status bars, navigation bars, and home indicators.
- Duration: one second, three seconds, and persistent.
- Animation: fade, slide, scale, spring, and Reduce Motion enabled.
- Gesture: successful directional swipe and a failed swipe that springs back.
- Style: all four colors, corner radius, padding, and shadow off/on.
- Control: update visible/queued toasts, dismiss visible/queued toasts, and dismissAll.
- Queue: three FIFO toasts; confirm each timer starts only on visibility.
- Stack: three visible, fourth overflow queued, then promoted after a dismissal.
- Events: shown; timeout, swipe, programmatic, and action dismissal; action pressed before action dismissal.
- Races: dismiss just before timeout, timing update while visible, dismissAll with timers, and action tap near timeout. Confirm one dismissal per ID.
- Lifecycle: rotate/background Android, reconnect scenes on iOS, and confirm no stale overlay, duplicate host, or lost active state.
