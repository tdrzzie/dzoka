FROM python@sha256:593bd06efe90efa80dc4eee3948be7c0fde4134606dd40d8dd8dbcade98e669c AS builder
COPY debs /debs
RUN dpkg -i /debs/*.deb
COPY llama-source /llama-source
RUN cmake -S /llama-source -B /build -DCMAKE_BUILD_TYPE=Release -DLLAMA_BUILD_COMMON=OFF -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_TOOLS=OFF -DLLAMA_BUILD_EXAMPLES=OFF -DLLAMA_BUILD_SERVER=OFF -DLLAMA_BUILD_APP=OFF -DLLAMA_BUILD_UI=OFF -DLLAMA_BUILD_MTMD=OFF -DLLAMA_CONNECT=OFF -DLLAMA_OPENSSL=OFF -DLLAMA_SUBPROCESS=OFF -DLLAMA_LLGUIDANCE=OFF -DBUILD_SHARED_LIBS=ON -DGGML_BACKEND_DL=OFF -DGGML_NATIVE=OFF -DGGML_OPENMP=OFF -DGGML_OPENMP_FETCH=OFF -DGGML_CPU_KLEIDIAI=OFF -DGGML_CPU_HBM=OFF -DGGML_LLAMAFILE=OFF -DGGML_BLAS=OFF -DGGML_CUDA=OFF -DGGML_VULKAN=OFF -DGGML_METAL=OFF -DGGML_AVX=ON -DGGML_AVX2=ON -DGGML_FMA=ON -DGGML_F16C=ON -DGGML_AVX512=OFF -DGGML_AVX512_VBMI=OFF -DGGML_AVX512_VNNI=OFF -DGGML_AVX512_BF16=OFF -DGGML_AMX_TILE=OFF -DGGML_AMX_INT8=OFF -DGGML_AMX_BF16=OFF -DGGML_CCACHE=OFF -DLLAMA_USE_PREBUILT_UI=OFF -DGGML_BUILD_TESTS=OFF -DGGML_BUILD_EXAMPLES=OFF
RUN cmake --build /build --target llama -j 2
RUN mkdir -p /opt/llama && cp -P /build/bin/libllama.so* /build/bin/libggml.so* /build/bin/libggml-base.so* /build/bin/libggml-cpu.so* /opt/llama/
COPY afri-driver.cc /afri-driver.cc
RUN g++ -std=c++17 -O2 /afri-driver.cc -I/llama-source/include -I/llama-source/ggml/include -L/opt/llama -lllama -lggml -lggml-base -lggml-cpu -pthread -Wl,-rpath,/opt/llama -Wl,-rpath-link,/opt/llama -o /afri-driver
RUN ldd /afri-driver && ldd /opt/llama/libllama.so && ldd /opt/llama/libggml-cpu.so
RUN mkdir -p /opt/build-evidence && cp /build/CMakeCache.txt /build/compile_commands.json /opt/build-evidence/ && cp /build/src/CMakeFiles/llama.dir/link.txt /opt/build-evidence/llama-link.txt
FROM python@sha256:593bd06efe90efa80dc4eee3948be7c0fde4134606dd40d8dd8dbcade98e669c
COPY minimal-runtime-debs /debs
RUN dpkg -i /debs/*.deb && rm -r /debs
COPY --from=builder /opt/llama /opt/llama
COPY --from=builder /opt/build-evidence /opt/dzoka-build
COPY --from=builder /afri-driver /usr/local/bin/dzoka-afri-translate
COPY notices /opt/dzoka-notices
ENV LD_LIBRARY_PATH=/opt/llama
USER 65532:65532
ENTRYPOINT ["/usr/local/bin/dzoka-afri-translate"]
