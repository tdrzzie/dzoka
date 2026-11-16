# 0044. Evaluate MADLAD with a separately reviewed INT8 CPU backend

- Status: Accepted diagnostic; executed, quality screen failed
- Date: 2026-11-16

## Context and decision

Accepted for bounded diagnostic execution on November 16, 2026. The mandatory source gate passes. The existing model providers, deployment profiles and immutable releases remain governing. The MADLAD Apache-2.0 candidate and exact artifact identities are reviewed in the [model record](../dependencies/madlad-cpu-review.md).

The first native compatibility probe returned unavailable before translation. Independently inspected CTranslate2 `src/cpu/backend.cc` lines 77–86 show that OpenBLAS serves float32 while Ruy can serve INT8. Preserve the initial failure. Evaluate a separate Linux amd64 image with INT8_FLOAT32, six GiB container memory, two CPUs, no network, a read-only filesystem, dropped capabilities and individually mounted verified model files. Source and output token ceilings remain explicit. No new Python dependency is introduced.

The existing CTranslate2 revision `5de572ef60b67a0276c5989ee73d0072808ff4c3` pins `google/ruy` at `363f252289fb7a1fba1703d99196524698cb884d`. Its Apache-2.0 license has SHA-256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`. Inspected root CMake lines 31–96 govern minimal builds, cpuinfo and test-only googletest. Library and test helpers were inspected, including the early return when minimal builds are enabled. [Pinned Ruy source](https://github.com/google/ruy/tree/363f252289fb7a1fba1703d99196524698cb884d)

Ruy pins `pytorch/cpuinfo` at `082deffc80ce517f81dc2f3aebe6ba671fcd09c9`. Its 27-line BSD license has SHA-256 `8e7e60636c3aa0cb03571a1a841ce5697f9551ff92b3c426c2561613d15ade70`. Its bundled clog has a separate 26-line BSD license with SHA-256 `68a14c3e79add023eecd83d611e73f05b8bf1d16852efeb7eb4033a96736da9e`. CMake lines 95–115 and 252–264 identify optional test downloads and bundled clog. Disable all tests/benchmarks, set Ruy minimal mode, and build with network disabled from verified source archives. googletest is excluded. Preserve all notices in the image. [Pinned cpuinfo source](https://github.com/pytorch/cpuinfo/tree/082deffc80ce517f81dc2f3aebe6ba671fcd09c9)

Static review covers these dependency/license/build boundaries. It does not establish executed upstream test results or model quality. Record actual archive hashes, image identity, driver source, failed probes and runtime outputs. Provider adoption requires separate versioned identities and acceptance evidence after the candidate demonstrates useful quality and reliable bounded execution.

## Alternatives and consequences

The existing OpenBLAS float32 image remains suitable for its pinned Marian providers. Dequantizing the three-billion-parameter candidate would require a materially larger memory allowance and separate acceptance. Ruy provides a permissive INT8 backend without adopting a vendor Python wheel. Its CPU performance, exact effective compute type and resource bounds must be observed on the current host. The diagnostic does not activate profiles or change core authorization, ownership or recovery decisions.

## Execution disposition

The full model/tokenizer/vocabulary/configuration hashes were verified. The separately built Ruy image required effective CPU INT8_FLOAT32 and completed twenty frozen public-reference calls with confirmed container cleanup and no truncation. The same twenty-case existing isolated baseline returned nine truncated outputs. Both eight-case MADLAD Shona→English slices exceeded the predeclared median/worst CER ceilings; four English→Shona probes were also poor. The [execution record](../testing/m4-language-alternatives-2026-11-16.md) retains exact image/result identities, counts, scores and failed probes. Do not adopt or activate this candidate as an accepted Shona quality improvement. Native compatibility is established within this diagnostic; representative semantic quality and production provider integration are not.
