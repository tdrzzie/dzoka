FROM python@sha256:d13fa0424035d290decef3d575cea23d1b7d5952cdf429df8f5542c71e961576
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg=7:5.1.9-0+deb12u1 && dpkg-query -W > /edge-packages.txt && rm -rf /var/lib/apt/lists/*
USER 65534:65534
ENTRYPOINT ["/usr/bin/ffmpeg"]
