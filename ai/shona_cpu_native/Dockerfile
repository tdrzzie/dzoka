FROM python@sha256:593bd06efe90efa80dc4eee3948be7c0fde4134606dd40d8dd8dbcade98e669c AS builder
COPY debs /debs
RUN dpkg -i /debs/*.deb
COPY source /source
COPY driver.cc /driver.cc
RUN cmake -S /source -B /build -DCMAKE_BUILD_TYPE=Release -DWITH_MKL=OFF -DWITH_DNNL=OFF -DWITH_CUDA=OFF -DWITH_CUDNN=OFF -DWITH_HIP=OFF -DWITH_RUY=OFF -DOPENMP_RUNTIME=NONE -DWITH_OPENBLAS=ON -DBUILD_CLI=OFF -DBUILD_TESTS=OFF -DBUILD_TESTING=OFF -DCMAKE_INSTALL_PREFIX=/opt/ct2 && cmake --build /build -j 4 && cmake --install /build
RUN g++ -std=c++17 -O2 /driver.cc -I/opt/ct2/include -L/opt/ct2/lib -lctranslate2 -Wl,-rpath,/opt/ct2/lib -o /driver
FROM python@sha256:593bd06efe90efa80dc4eee3948be7c0fde4134606dd40d8dd8dbcade98e669c
COPY runtime-debs /debs
RUN dpkg -i /debs/*.deb && rm -r /debs
COPY --from=builder /opt/ct2 /opt/ct2
COPY --from=builder /driver /usr/local/bin/dzoka-translate
COPY notices /opt/dzoka-notices
ENV OPENBLAS_NUM_THREADS=2 CT2_VERBOSE=0
USER 65532:65532
ENTRYPOINT ["/usr/local/bin/dzoka-translate"]
