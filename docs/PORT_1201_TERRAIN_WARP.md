# Forge 1.20.1 terrain warp with AuramBSL

Brown mushrooms reserve the original small waves after the active value reaches 0.15. A single mushroom reaches about 0.38 at peak. The reserved wave still uses the original category and effect ramps. The remaining slots keep the weighted rotation. Removing the unused shattering selection also frees a slot for an effect with a renderer.

Peyote moves world vertices vertically with two slow waves centered on the player. The waves use the active intensity that flight reads. The Visuals and Shaders settings page has a separate Peyote Screen Distortion control, enabled by default.

When Distant Horizons is present, nearby geometry and the LOD shaders evaluate the same blend using camera distance, absolute world position, and time. With Auram's five-chunk distance, the original waves start blending to the first broader band at 16 horizontal blocks and reach it fully at 64 blocks. Two further bands use four and sixteen times its wavelength. The higher wavelengths reduce undersampling on coarse meshes. The shared functions apply the displacement before AuramBSL projects the terrain or draws its shadows. The shader pack files and options come from Auram.

## Evidence

- The initial brown-mushroom run on the released jar selected the unused shattering effect while every terrain warp input stayed zero. A single mushroom in the terrain patch selected visible movement. The reservation regression covers 64 random seeds.
- The small-wave calculations from the original and current shader agree at 855 sampled vertices and times inside the original range. The comparison uses the same camera, strength, and timing read from a live AuramBSL frame.
- The first peyote run reached 0.3782 after eating dried peyote. Nearby terrain, shadows, and both DH programs used the same intensity. At the fixed camera, the foreground walkway rose while the nearby objects and sky provided points of reference.
- The brown-mushroom route captures 82 positions in each state from Z −720.5 to −12.5. Both DH layers drew at every position. All 25 active frames that also drew nearby terrain used matching camera, waveform, and time inputs across all three paths. The opaque render list included DH section levels 6 and 7. The replay shows the same structures bending before and after entering the native chunk range.
- One peyote dose completed the same route with both DH layers and the nearby draw present in the same sampled ranges. With both drugs stacked and screen effects enabled, AuramBSL reloaded mid-dose. Brown mushrooms reached 0.8476 and peyote reached 0.8626 afterward, and all three render paths continued drawing for the replay.
- A second peyote replay steps through the near boundary in two-block increments and samples the actual depth textures on a 13 × 9 grid. Eight active samples contained both near and LOD geometry. All ten LOD readbacks differed between the opaque snapshot and the depth with translucent terrain. The shadow draw used the same warp inputs as the world draw.
- The tested shader-pack copies matched 283 shader files and all 61 option values from Auram. The runtime checks used Forge 47.4.10, Embeddium 0.3.31, Oculus 1.8.0, and Distant Horizons 2.4.5-b on Apple M4 Max OpenGL.
- With Oculus shaders off, one dose of each still moved the native Embeddium and DH meshes. A separate run left both drugs to wear off naturally. The terrain programs returned to zero warp after both active values reached zero.
- The Java 17 build reobfuscated the test jar and passed 94 unit tests and all 303 required Forge GameTests.
- The same 640 × 360 camera and active wave inputs were measured for 360 frames before and after the LOD blend. Median time inside the render event went from 8.32 to 6.72 ms. The wall-frame median went from 55.08 to 54.01 ms. These are spot checks on the local renderer, with substantial variation in the slowest frames.

Distant Horizons merges terrain into larger faces as detail falls. Vertices at the same world position get the same displacement, but a sparse face interpolates only between its existing vertices. Broad waves preserve visible motion across this change while native chunks retain the shorter waves near the player. DH still controls when it replaces a mesh, and AuramBSL still controls fog and visibility.

The reobfuscated jar, paired replays, fixed-camera captures, and draw logs are in the local `run/production/auram-shader-qa/29-terrain-continuity` directory. Each paired replay shows the sober route on the left and the active route on the right.
