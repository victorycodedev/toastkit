# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in ToastKit, please report it privately to the maintainer rather than opening a public issue.

Open an issue at <https://github.com/victorycodedev/toastkit/issues> and request private disclosure, or contact the maintainer directly.

## Scope

ToastKit is a presentation utility for NativePHP Mobile apps. It introduces no network endpoints, no persistence, and no Android or iOS permissions. Reports are most useful when they concern:

- Malformed bridge payloads that could crash or misbehave the native overlay.
- Unsafe handling of user-supplied toast content (messages, titles, colors, IDs).
- Memory leaks or lifecycle issues in the native overlays.

## Supported versions

Only the latest release receives security fixes. Please upgrade before reporting.
