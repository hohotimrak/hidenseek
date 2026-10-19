# Still Hunt — independent implementation

The complete user requirements are preserved in REQUEST.ru.txt. They are requirements, not claims about the reference. No original source, binaries, textures or text were copied. No decompilation was performed. This is independent development, not a claimed strict clean-room process.

## Evidence and stack

- https://docs.fabricmc.net/1.21.8/develop/getting-started/setting-up — JDK 21.
- https://github.com/FabricMC/fabric-docs/blob/main/reference/1.21.8/build.gradle — Minecraft 1.21.8 and Fabric API 0.134.0+1.21.8.
- https://maven.fabricmc.net/docs/yarn-1.21.8+build.1/ — exact mapping/API reference.
- Loader 0.17.3, Loom 1.11.8, Gradle 8.14.3 are pinned; actual compatibility must be established by compilation and dedicated-server startup.
- https://modrinth.com/mod/hide-plusseek was accessible. No original gameplay was exercised.
- https://www.youtube.com/watch?v=e1bbYM9d_QA could not be fetched. No claim of watching it.

## Design decisions

One match per server, one dimension, exactly two seekers and one hider. Server tick time drives durations. LOBBY → HIDING → SEEKING → FINISHED; PAUSED retains previous phase. A restart loads active matches paused. No offline time is added to game time.

Mathematical map cells are closed spatial boxes, not representative point samples. A cell is excluded only when an entire box contradicts a truthful constraint. Surface tiles are presentation only and never prove underground absence. Unknown or mutable world data cannot exclude a location. Block answers are timestamped historical observations; this first implementation conservatively does not derive geometry from them.

Public and private network views are built separately. A seeker's view never serializes the hider position, hand, offers or reward counters. Vanilla entity tracking remains a separate anti-cheat limitation.

Content handlers are explicitly registered. JSON contains numeric parameters, never executable code. Commands and custom packets both enter the same server authority checks. Purchases use match-scoped request IDs and commit only after successful evaluation.

## Acceptance gates

1. Pure Java randomized map tests: true hide box survives truthful constraints, boundaries and vertical projection are conservative.
2. Economy: concurrent/repeated IDs, cooldowns, failed evaluations and reward tiers.
3. Compile both environment source sets and remap distributable JAR.
4. Dedicated server startup, including mixin application, without client classes.
5. Real clients: body lock, remote camera, capture, screenshot, flight, LAN and restart. These are not substitutes for unit tests and cannot be claimed without clients.
