# Auram shader compatibility

## Renderer setup and first-person boot fix

The isolated client uses Minecraft 1.20.1, Java 17, Forge 47.4.28, Oculus 1.8.0, Embeddium 0.3.31, Distant Horizons 2.4.5-b and the reobfuscated Psychedelicraft jar. The renderer mods, AuramBSL directory, adjacent `AuramBSL.txt`, and relevant configs came from the Auram Prism instance. The shader option values were preserved. The copied settings file has SHA-256 `72acb13cef1eecfe78d589c28e3178da95b2647567c59c3f56a15cc9f63e64ab`. Oculus read the custom profile with 61 user overrides.

The original production jar crashed during construction. Its redirect of `ItemInHandRenderer.renderHandsWithItems` in `GameRenderer` prevented Oculus's required redirect of the same call from applying. Psychedelicraft now applies its pose, render scope and out-of-body hand suppression inside `ItemInHandRenderer`. The native call remains available to Oculus, whose shader hand passes also call the shared method. Oculus batches some hand draws outside that method. Hand-specific shader program and buffer behavior still need verification.

The separate exact-loader run reached Forge's dependency resolver and rejected the jar on 47.4.10, the version pinned in Auram. The jar still declares 47.4.28 as its minimum. The reduced boot setup uses the four mod jars above. The renderer setup, logs, and disposable world are under the ignored `run/production/auram-shader-qa` directory. All tests ran in an isolated windowed, muted Linux/Xvfb client. The original Prism instance supplied files through read-only access.

The Java 17 build passed 92 unit tests and reobfuscated the jar. The headless Forge run passed all 303 required GameTests. The packaged renderer result is recorded below.

### Packaged boot result

The patched jar reached the seed-12345 Overworld with AuramBSL active. The visible framebuffer contained terrain, the first-person hand, and the shaded sky. The F3 capture identifies Forge 47.4.28, Embeddium 0.3.31 and Oculus shadow draws. Oculus wrote a fresh timestamp to the local shader settings file, and all 61 option values still match Auram. Distant Horizons initialized its LOD renderer, compiled its setup shaders, and wrote more than 200 records to the Overworld LOD database. It reports partial Oculus support. LOD draw correctness, Psychedelicraft effect parity, and hardware performance remain to be verified.

The candidate jar used for this test has SHA-256 `475877b9b2d1a99303983e571c2a037a362556bef903c0113ed1773d2278f6d9`. The original crash, loader rejection, and in-world framebuffer are retained in `run/production/auram-shader-qa`. The successful test uses Linux ARM64, Mesa llvmpipe and Xvfb. The first frames took tens of seconds on this software renderer.

## Auram's Forge 47.4.10 loader

The published minimum initially came from the MDK used for the base port. Auram's Prism profile pins 47.4.10. Compare the official 47.4.10 and 47.4.28 source archives. Their 666 Forge Java source paths match. Among the 80 directly imported Forge types found in the archives, only `RegistryObject` changes. Its implementation updates registry validation, reference handling, and equality. The port compiles and reobfuscates against 47.4.10 using the same Minecraft 1.20.1 mappings.

For the installed-runtime trial, use the new build with a local candidate manifest declaring `[47.4.10,48)`. The candidate and the preceding 47.4.28 build have the same extracted contents apart from that manifest. Install the same candidate on separate Forge 47.4.10 and 47.4.28 dedicated servers. Both enable the Psychedelicraft datapack, finish creating a fresh flat world, save all dimensions, and exit zero. Then set the declared minimum in source and rebuild. All 1,574 extracted entries of the final build match the candidate.

The test inputs and runtime results are retained under ignored `run/production/auram-shader-qa/01-forge-floor`. The renderer clone uses all 283 unchanged AuramBSL files and all 61 matching shader option values. Its canonical sorted relative-path and SHA-256 inventory digest is `65f59aa266c8e04093f9f925ce3a558e3ec7c21eebe36f3bbd0e5c7d054dc1a5`. The source settings file is hashed separately because Oculus rewrites its timestamp comment. The owned full Auram client snapshot stages 541 top-level mod jars and a manifest for later integration.

The 47.4.10 client reads the copied 61-option AuramBSL profile and joins the isolated Overworld with the complete renderer stack. Its save was copied from a 47.4.28 run, so Forge logs the saved loader-version difference before proceeding. This joined-world checkpoint verifies that the new dependency floor permits the target client to run. It does not establish effect or LOD parity. The earlier 47.4.28 renderer smoke uses the same code and resources.

The final Java 17 `build runGameTestServer` on Forge 47.4.10 reobfuscates the jar and passes 92 unit tests and all 303 required GameTests. The built jar has SHA-256 `937b28a4d50e4ccdd1cfa65e2d2c765d0700159a778e8e96623fdedee6a3d9b7`. The next cycle will identify which shader programs draw each material, locate the completed color and near/far depth at each frame phase, and capture comparable sober, active and cleared frames. [Official Forge release archive](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)

## First measured compositor and terrain phases

Run the same installed Forge 47.4.10 jar in two isolated Xvfb clients on a headless ARM guest. One uses stock Forge and the other uses Embeddium, Oculus, unchanged AuramBSL with all 61 options, and Distant Horizons. The QA sidecar records actual GL bindings and sampled pixels at each render boundary. Its optional draw hooks count submissions immediately after Embeddium's multi-draw and Distant Horizons' indexed draws. The sidecar lives outside the production jar.

Both clients reach the Overworld and yield 12 rendered frames. On the full stack, Oculus's final pass runs ahead of Forge `AFTER_LEVEL` and Psychedelicraft's `completeWorld`. For example, at the sampled center pixel in frame 12, the main target changes from RGBA `176,205,255,0` before Oculus compositing to `49,72,43,255` at its final pass. The later Psychedelicraft pre/post samples and Oculus's sRGB conversion sample all read the latter value during this sober frame. The final sampled depth `0.9666811` is also present at the pre-hand boundary. The same frame records 33 Embeddium batches across five linked program IDs and 18 Distant Horizons draws across two linked program IDs. This orders the actual calls and confirms both terrain renderers submit work. The one depth sample and combined LOD counts leave near/far projection and opaque/translucent coverage open.

The first client in the shared Docker environment exited with code 137 after joining and setting up the LOD renderer. The isolated replay produces an inspected scene with near tree leaves, shaded sky, distant terrain, and the held hand. No source shader or dependency jar changes were needed to obtain these frames. Keep profiles, captures, and structured logs under `run/production/auram-shader-qa/02-render-matrix`. The fixed seed-12345 matrix scene is now generated through an installed Forge server without the mod and independently checks its near glass and mob, far land, and far water. Next compare the seven configurations using that same scene and controlled states. [Forge 47.4.10 CI run](https://github.com/AuramMods/Psychedelicraft/actions/runs/44702741673)

## Seven renderer configurations in one scene

Copy the same seed-12345 scene for each configuration. The fixture places the camera at `(0.5, 105, -12.5)` and uses the normal server inventory and `/drug` command paths. The active samples use LSD 0.9, cannabis 0.7 and Zero 0.35. The next phase clears and unlocks the profiles. The no-mod control keeps the same camera and held sword across the three captures. Each client saves four sampled frames per phase and one image per phase, then closes its isolated world normally. All seven processes exit zero and produce all 21 phase images.

| Configuration | Active terrain observation | Active Psychedelicraft post observation |
| --- | --- | --- |
| Stock Forge | Native world stages reach the stock shader path | Center RGB changes and Zero glyphs appear |
| Embeddium | Six terrain batches across two programs. Both report `PS_Enabled` location −1 | Center RGB changes |
| Embeddium and Distant Horizons | Six near batches, two opaque and nine translucent LOD draws. All three programs report −1 | Center RGB changes |
| Embeddium and Oculus with shaders disabled | Six near batches on the same two bare terrain programs. Both report −1 | Center RGB changes |
| AuramBSL and Embeddium without Distant Horizons | Twelve near batches on three shader-pack programs. All report −1 | Center RGB changes |
| AuramBSL, Embeddium and Distant Horizons | Twelve near batches, six opaque and twenty translucent LOD draws at frame eight. All five active programs report −1 | Center RGB changes from `208,219,228` to `89,90,93` across Psychedelicraft's post boundary. The active image visibly contains the digital glyphs. |
| Complete renderer stack without Psychedelicraft | Twelve near batches, six opaque and eighteen translucent LOD draws at frame eight | The mod's post boundary is absent. The control image retains the held diamond sword. |

The replacement terrain and LOD programs bypass the mod's native core uniform path. The full renderer stack retains some post effects at the existing compositing boundary. The center sample in this fixed view is at far-clear depth, so it cannot verify either near/far depth combination or depth-specific effects. Fixed-scene differences also include temporal shader history, visual overlays and camera effects. Keep the full frames and raw `QA_EVT` traces for later focused comparisons.

The full-stack image with Psychedelicraft loses the diamond sword in all three phases, although its inventory icon is present. The same shader pack without Psychedelicraft visibly draws it. The stock case draws the sword with the mod. Trace Oculus's actual hand program and uniform uploads before patching the affected path. The matrix inputs, 21 captures and results are in ignored `run/production/auram-shader-qa/02-render-matrix`.
