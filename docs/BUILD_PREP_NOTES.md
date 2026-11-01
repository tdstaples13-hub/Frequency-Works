# Build-preparation checkpoint

- Corrected `BlockCapabilityCache.create` call from an undocumented five-argument overload to the documented four-argument form for NeoForge 1.21.1. (The documented optional invalidation listener overload has six arguments.)
- Updated GitHub Actions to run existing standalone tests and JSON validation before compilation.
- When an online build is attempted, a build log artifact is uploaded even if compilation fails, so errors can be diagnosed.
- Builds may not pass on the first attempt; no NeoForge compilation has been performed in this environment.
- This source is not a finished playable mod: Sequencer behavior, Frequency Monitor UI, and previous-tier Buffer upgrade recipe persistence are not implemented.
