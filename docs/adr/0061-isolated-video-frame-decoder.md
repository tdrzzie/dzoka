# 0061. Optional isolated compressed-video frame decoder

- Date: 2026-11-26
- Status: Accepted for the edge process boundary

Use Debian's FFmpeg package in an independently built, digest-pinned optional container for compressed camera clips. FFmpeg has LGPL and optional GPL components. Debian's configured binary includes GPL components. Keep the binary and its dependencies inside that optional process, preserve package license documents, and record installed versions and the resulting image identity. No FFmpeg source or library is linked or copied into the permissive Core. [FFmpeg legal terms](https://ffmpeg.org/legal.html), [Debian package](https://packages.debian.org/bookworm/ffmpeg).

Only locally indexed and hash-bound clips up to 64 MiB are accepted. The manifest supplies a UTC timestamp and explicit nonnegative clip offset for every indexed sample. The decoder seeks to one authorized sample and emits one scaled P6 frame, with networking disabled, an unprivileged user, read-only input, dropped capabilities, bounded memory/CPU/PIDs and a wall-clock deadline. Whole clips stay on the edge host. Subsequent appearance processing and evidence approval use the same reference and grant boundaries.
