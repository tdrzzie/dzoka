# 0060. Portable edge frame processing

- Date: 2026-11-26
- Status: Accepted for the bounded frame archive profile

Support indexed, hash-bound P6 frame exports at up to 320 by 240 pixels. Keep the archive at the edge node. Use an original fixed-grid reference appearance provider for detection and local tracking. This provider needs no downloaded model weights. Search coarse intervals first and refine candidate disappearance windows within the grant budget. Treat missing or ambiguous appearance as uncertain evidence.

Run appearance processing in the official Python image pinned to `python@sha256:d13fa0424035d290decef3d575cea23d1b7d5952cdf429df8f5542c71e961576`. The Python runtime uses PSF-2.0 and its historical license stack. The Debian base contains separately licensed system packages. The application imports standard-library modules only, mounts its own source read-only, disables networking, drops capabilities, uses an unprivileged UID, and imposes CPU, memory, PID and wall-clock limits. The archive and API credentials stay in the parent. No upstream application or model source is copied.

This deployment consumes the image by digest. Redistribution must preserve Python and Debian package notices. The image is an optional edge runtime and adds no Core JAR dependency. Compressed video ingestion can be implemented behind the frame archive contract using an independently reviewed decoder. [Python license](https://docs.python.org/3.14/license.html), [official image license guidance](https://hub.docker.com/_/python).
