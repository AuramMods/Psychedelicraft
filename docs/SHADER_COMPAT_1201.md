# Auram shader compatibility

## Renderer setup and first-person boot fix

The isolated client uses Minecraft 1.20.1, Java 17, Forge 47.4.28, Oculus 1.8.0, Embeddium 0.3.31, Distant Horizons 2.4.5-b and the reobfuscated Psychedelicraft jar. The renderer mods, AuramBSL directory, adjacent `AuramBSL.txt`, and relevant configs came from the Auram Prism instance. The shader option values were preserved. The copied settings file has SHA-256 `72acb13cef1eecfe78d589c28e3178da95b2647567c59c3f56a15cc9f63e64ab`. Oculus read the custom profile with 61 user overrides.

The original production jar crashed during construction. Its redirect of `ItemInHandRenderer.renderHandsWithItems` in `GameRenderer` prevented Oculus's required redirect of the same call from applying. Psychedelicraft now applies its pose, render scope and out-of-body hand suppression inside `ItemInHandRenderer`. The native call remains available to Oculus, whose shader hand passes also call the shared method. Oculus batches some hand draws outside that method. Hand-specific shader program and buffer behavior still need verification.

The separate exact-loader run reached Forge's dependency resolver and rejected the jar on 47.4.10, the version pinned in Auram. The jar still declares 47.4.28 as its minimum. The reduced boot setup uses the four mod jars above. The renderer setup, logs, and disposable world are under the ignored `run/production/auram-shader-qa` directory. All tests ran in an isolated windowed, muted Linux/Xvfb client. The original Prism instance supplied files through read-only access.

The Java 17 build passed 92 unit tests and reobfuscated the jar. The headless Forge run passed all 303 required GameTests. The packaged renderer result is recorded below.

### Packaged boot result

The patched jar reached the seed-12345 Overworld with AuramBSL active. The visible framebuffer contained terrain, the first-person hand, and the shaded sky. The F3 capture identifies Forge 47.4.28, Embeddium 0.3.31 and Oculus shadow draws. Oculus wrote a fresh timestamp to the local shader settings file, and all 61 option values still match Auram. Distant Horizons initialized its LOD renderer, compiled its setup shaders, and wrote more than 200 records to the Overworld LOD database. It reports partial Oculus support. LOD draw correctness, Psychedelicraft effect parity, and hardware performance remain to be verified.

The candidate jar used for this test has SHA-256 `475877b9b2d1a99303983e571c2a037a362556bef903c0113ed1773d2278f6d9`. The original crash, loader rejection, and in-world framebuffer are retained in `run/production/auram-shader-qa`. The successful test uses Linux ARM64, Mesa llvmpipe and Xvfb. The first frames took tens of seconds on this software renderer.
