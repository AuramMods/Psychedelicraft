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

## First-person sword visibility with Oculus

The bound Oculus `hand_cutout_diffuse` program draws to framebuffer 77 and links `PS_ApparitionTint` at location 55 in this run. The original jar leaves it at `(0, 0, 0, 0)` in the sober phase. Psychedelicraft's core fragment extension multiplies its output by this tint, and Oculus overrides the native shader apply method that normally uploads it. Oculus's named uniform accessor also applies its own uniform naming convention.

The optional client mixin uploads the current tint directly to each linked Oculus `ExtendedShader` or `FallbackShader` program after it binds and sets its own uniforms. The full-stack replay with unchanged AuramBSL and all 61 custom option values reads `(1, 1, 1, 1)` in the recorded hand draw. The diamond sword is visible in the sober and cleared captures, and Zero's digital effect remains visible in the active capture. The same candidate completes the three-phase stock Forge client trial and retains the sword there. Both clients save their isolated worlds and exit normally. This establishes baseline hand visibility. Hand geometry and apparition color still need individual effect checks.

The reobfuscated candidate has SHA-256 `6c8990690c720fe4551e90bf5b80e712ba33dbe263211dcd61cd426ccd4d657b`. The Java 17 build passes 92 unit tests and all 303 required Forge 47.4.10 GameTests. Traces and screenshots are retained in ignored `run/production/auram-shader-qa/03-oculus-hand`.

## Embeddium terrain with shader packs disabled

Embeddium 0.3.31 loads its own chunk stages from `sodium:blocks/block_layer_opaque`. Apply the shared geometry equations to the decoded chunk position and model-view matrix. The vertex stage uses the existing block atlas for surface color. The fragment stage retains Embeddium's material cutoff and fog. The chunk program uploads the shared effect state after Embeddium binds its program and textures. Uniform locations live with that program's interface.

The installed 47.4.10 bare Embeddium and Oculus-with-shaders-off trials each submit six terrain batches through two linked programs in every sampled phase. The previous jar reports no `PS_Enabled` uniform on either program. The candidate reads `0`, `1`, then `0` across sober, active and cleared phases on both programs. In the active phase it reads small and wiggle waves `(0.325, 0, 0.28)`, surface strength `0.65`, and pulse alpha `0.3`. The sidecar sets those inputs deterministically and turns off Psychedelicraft's 2D shader for these trials. The checkerboard shape visibly bends with the candidate and returns to its regular layout after clearing. The previous jar leaves the board straight while the mobs and sword still deform.

The stock Forge client and the separate full AuramBSL and Distant Horizons profile both complete their captures and exit zero with the same jar. The full profile reads all 61 custom shader options and still uses Oculus's own terrain programs. The Java 17 build passes 92 unit tests and reobfuscates the candidate with SHA-256 `7661eb36a2e4bbdeab5f431fea8199428899335289a3dbe150dc5330d6136771`. The five controlled runs and comparison captures are in ignored `run/production/auram-shader-qa/04-embeddium-terrain`.

## Oculus terrain displacement and terrain shadows

Oculus's own debug output identifies separate transformed vertex stages for solid terrain, cutout terrain, water, and the shadow map. They all take the decoded position through Iris's `getVertexPosition`. The compatibility mixin composes the shared deformation there after Iris finishes transforming the stages. It projects the decoded position with the viewer's world matrix, applies the accepted waves and fracture, and transforms the result back into camera-relative world space. AuramBSL then runs its own projection, lighting, water, shadow distortion, and jitter on that position. Terrain shadow draws get the same displaced world position as the visible terrain. The change leaves the shader files and settings outside the mod untouched.

The matched 47.4.10 captures disable Psychedelicraft's 2D passes and inject the same fixed geometry inputs in both jars. Each sampled frame submits six terrain shadow batches and six world batches. The previous jar has no effect locations on those three bound programs. The candidate reports `PS_Enabled` values `0`, `1`, and `0` on all three. The active checkerboard, glass and ground visibly bend. The cleared scene restores the straight silhouettes. A separate native `/drug` run with the full effect chain still shows Zero glyphs, and the stock client still deforms its checkerboard. All four processes save and exit zero. These trials measure terrain displacement and the participating shadow draws. Far LOD displacement, isolated shadow alignment, and Oculus material color remain open.

The reobfuscated jar has SHA-256 `79a3393f4f1a74d401cdabb141a7ad4185efbaf0ba1d752dc499b2d9367c49a2`. The final Java 17 build passes 92 unit tests and all 303 required Forge GameTests. The transformed GLSL dumps, draw traces, and captures are in ignored `run/production/auram-shader-qa/05-oculus-terrain`.

## Distant Horizons terrain and water displacement

The native Distant Horizons 2.4.5-b terrain shader decodes camera-relative vertices and applies its curvature before projecting them. The compatibility stage applies the same displacement used by the near terrain before the native LOD projection. With Oculus active, the transformed `dh_terrain` and `dh_water` shaders instead take the displaced position through Iris's decoded position helper. Both pipelines upload the current frame's shared geometry state after their own program setup.

Four installed Forge 47.4.10 replays use a spectator camera at `(0.5, 148, -30.5)`, five native render chunks, a fixed test landscape, and matched warmed LOD databases. In the active AuramBSL replay six opaque draws bind the Iris LOD program at ID 215 and 38 translucent draws bind the separate Iris LOD program at ID 3. Both programs read `PS_Enabled` values `0`, `1`, and `0` across the three phases. With shader packs off, four opaque and 18 translucent draws use the same linked native Distant Horizons program with the same values. Neither previous-jar control links the geometry uniforms in its actual LOD draws. The far red column, water, and towers visibly change silhouette with the candidate and return after clearing. The full-stack run preserves all 61 shader options. All four processes save and exit zero.

The Java 17 build passes 92 unit tests and reobfuscates the jar with SHA-256 `afc4453873e4caabfe047994f18be731a7f709e828ac7214ef23957c148cd2df`. Near/far transitions in motion, LOD color effects, and depth-specific post passes need separate verification. Captures and actual draw traces are in ignored `run/production/auram-shader-qa/06-dh-terrain`.

### Full Auram clone first comparison

The isolated clone contains the 541 original top-level jars and reports 629 loaded mods before adding the port and the audit sidecar. Both the port trial and the separate unmodified control fail Forge's construct phase in Evolved Mekanism 1.2.1-fix3. JEI initializes `EMItems` before its custom QIO drive tiers are initialized. Both launches also report a GLSL 4.60 compiler message on Mesa llvmpipe before that failure. Auram's original macOS log records a world login using the same Forge loader and AuramBSL. The isolated trial needs to reproduce the successful construction order before it can verify a world on the full pack. The original profile remains untouched. Logs are in ignored `run/production/auram-shader-qa/full-auram`.
