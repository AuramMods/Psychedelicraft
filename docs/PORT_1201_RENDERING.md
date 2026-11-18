# Audiovisual implementation and evidence

Use this with the [registration matrix](PORT_1201_REGISTRY.md), [settings matrix](PORT_1201_SETTINGS.md), [native mappings](PORT_1201_NATIVE.md), and detailed [cycle report](PORT_1201.md). The accepted `51286f8f3e6866e8b952175c9c60c7f6e63bef8d` supplies the behavior contract; original `319a4ea0160d6600cffcd5f5cc9e363ceaab44a1` supplies target-native programs/models. C19–C24 record the LTS/latest comparisons and the individual implementations. C29 adds text effects; C36 adds the combined-material and Reggie/text observations and corrects pulse-distance parity.

“Controlled input” below means a QA sidecar supplies renderer channels or a scripted voice input while the production jar performs the actual native draw, shader execution or delayed dialogue callback. It is not proof of natural dose onset. Ordinary consumable/command/packet checks are identified separately. All installed clients use owned, muted Linux/Mesa/Xvfb instances.

| Requirement | Chosen native implementation | Evidence | Remaining scope |
| --- | --- | --- | --- |
| Weighted effect selection/onset/decay | [HallucinationManager](../src/main/java/ivorius/psychedelicraft/client/render/HallucinationManager.java), accepted weights/timing | C19 calculation regressions; three actual full-hunger LSD pills produce native owner post output while the observer stays sober | Broader per-benefit multiplayer/natural-expiry checks |
| Color, contrast, saturation, ordinary/colored bloom, blur, noise and digital/binary output | [PostEffects](../src/main/java/ivorius/psychedelicraft/client/render/shader/PostEffects.java), original program resources and native pass/target lifecycle | C19 individual GPU fixtures, real zero/one glyphs, matched-surface colored bloom; C36 combined scene | Renderer-replacement compatibility deferred |
| World depth and near/far focus | Native depth capture and independent native passes in PostEffects | C19 near-only/far-only image ROIs, depth samples, resize, stencil and resource reload | Broader lifecycle matrix |
| Double vision, heat/water distortion and wet overlay | PostEffects + [EnvironmentEffects](../src/main/java/ivorius/psychedelicraft/client/render/EnvironmentEffects.java) | C19 controlled and actual water immersion/exit; C34 water-like brew contact; C36 immersion composition | Per-benefit lifecycle matrix |
| Temporal history/motion | Native scaled history targets in PostEffects | C19 moving-camera history, actual GUI scale, pause and exact neutral recovery; independent motion toggle | Broader death/dimension matrix |
| Small/big/wiggle waves, distant deformation and direct LSD fracture | [GeometryEffects](../src/main/java/ivorius/psychedelicraft/client/render/shader/GeometryEffects.java), composed native core stages | C20 individual native frames and coordinate/clip tests; C21/C22 stacked composition; C36 special materials | External renderer integration deferred |
| Surface fractals and material exclusions | [GeometrySource](../src/main/java/ivorius/psychedelicraft/client/render/shader/GeometrySource.java), current native atlas sprite, [GeometryMaterial](../src/main/java/ivorius/psychedelicraft/client/render/shader/GeometryMaterial.java) | C20 native color/texture/fog/alpha preservation; C36 portals, stars, border, glint and GUI/post exclusions | Shader-pack replacements deferred |
| Pulse distance and fog policy | [GeometryMaterial](../src/main/java/ivorius/psychedelicraft/client/render/shader/GeometryMaterial.java), [vertex stage](../src/main/resources/assets/psychedelicraft/shaders/geometry/basic.gvsh), inverse native view | C36 compiled-stage transform-feedback arithmetic and real fog-dependent material uniforms; details below | No compatibility claim for alternate renderers |
| World/model/cloud/hand scopes and GUI separation | [RenderPhase](../src/main/java/ivorius/psychedelicraft/client/render/RenderPhase.java), mapped client-only mixins | C20 native moving models/clouds/glint; C36 combined draws and whole-frame NORMAL restoration | Alternate renderer lifecycles deferred |
| Camera drift, wobble/tremor, mouse inertia and local hand/body poses | [CameraEffects](../src/main/java/ivorius/psychedelicraft/client/render/CameraEffects.java), native camera/mouse/player scopes | C21 owner/observer matrices, first/rear/front views, actual hand draws, out-of-body cleanup and combined frame | Broader death/respawn/dimension checks |
| Screen overlays, perception and heartbeat | [ScreenOverlays](../src/main/java/ivorius/psychedelicraft/client/render/ScreenOverlays.java), native overlay mesh/shader and local sound | C22 individual/combined rendered overlays and heartbeat; C19/C22 post composition | Broader natural-expiry checks |
| Smoking/bubble particles | Registered mod particles and native particle engine | C5 smoking; C13 ambient liquid/projectile particles; C36 actual native smoke/flame particle material in the combined scene | C36 does not claim a new per-particle species audit |
| Detached entities and identity swaps | [ContextualHallucinations](../src/main/java/ivorius/psychedelicraft/client/render/ContextualHallucinations.java), native entity models, detached substitutes | C23 22 solo species + four fish groups, owner-only forcing packets, swaps, faint/occluded/F1/Fabulous/reload/expiry/dimension/reconnect output | Current native species mapping bounds remain in native report |
| Reggie model/orbit/local dialogue | [RastaHeadHallucination](../src/main/java/ivorius/psychedelicraft/client/render/RastaHeadHallucination.java), [RastaHeadChat](../src/main/java/ivorius/psychedelicraft/client/render/RastaHeadChat.java) | C23 actual model, delayed voices, real player-chat response, non-damaging nudge and observer privacy; C36 normal/distorted/restored and composed output | Broader natural drug expiry |
| Incoming/outgoing drug text | [ClientChatEffects](../src/main/java/ivorius/psychedelicraft/client/ClientChatEffects.java), preserved native components/signing boundaries | C29 actual owner/observer chat, command protection and Unicode/style handling; C36 local Reggie route | No shader-pack dependency introduced |
| Music, breathing and perceived listener volume | [ClientDrugMusic](../src/main/java/ivorius/psychedelicraft/client/sound/ClientDrugMusic.java), [ClientSoundEffects](../src/main/java/ivorius/psychedelicraft/client/sound/ClientSoundEffects.java) | C24 actual native playback/source lifecycle, settings, breathing, volume and death cleanup; master remains muted in QA | Broader per-benefit lifecycle checks |
| Rift rays, zero-matter center, jar interior and glitch | [ZeroMatter](../src/main/java/ivorius/psychedelicraft/client/render/ZeroMatter.java), native entity/block-entity renderers | C14 implementation/resources; C36 real combined scene with core/post effects and reload/resize | External renderer compatibility deferred |
| Native mob models under effects | Native drowned, wandering trader and trader llama renderers | C36 actual scene models with terrain/hand/particle/special-material/post effects | Native behavior/acquisition and custom-benefit scopes remain in native report |

## Pulse-distance parity correction

The initial C36 program fixtures verify the uploaded material selectors but expose a limitation of that observation: a correct selector does not prove the selected shader equation. The accepted `WorldGeometryShader` and `world_geometry.vsh` use cylindrical world-up distance for ordinary terrain/models, switching to spherical distance for immersion, impairment or thick fog. The original 1.20.1 parent terrain shader also obtains its ordinary cylindrical distance from the native fog path.

The target was calculating terrain/particle selector `3` with `length(view)`, while model selector `2` used the cylinder. Copy the production native `position` program's already-compiled stages into a diagnostic program, register its actual `PS_PulseDistance` varying for transform feedback, and submit the point `(3,4,0)`. This does not relink or replace the active native program. The diagnostic restores its program, buffers, VAO, feedback binding and caller GL/cache state.

| Compiled GPU path | Before | After | Reference |
| --- | --- | --- | --- |
| Uniform | 0 | 0 | 0 |
| Spherical | 5 | 5 | 5 |
| Model cylinder | 4 | 4 | 4 |
| Terrain/particle cylinder | **5** | **4** | **4** |

Both identity view and a 60-degree tilted camera produce those values. Thus the mismatch is observed from compiled GPU execution, not inferred only from shader text or uniform names. The pre-fix diagnostic records an actual failed assertion; the fixed diagnostic completes and exits zero.

Ordinary terrain and models now share `max(length(relative.xz), abs(relative.y))`, with recovered world up. Spherical distance remains `length(view)`. At the native world-view capture, cache the actual camera/dimension/boss fog conditions once for that view. Native water/lava/powder-snow immersion, blindness/darkness, dimension fog and boss fog select the spherical policy for non-uniform materials. Portals, borders and other uniform materials retain zero; zero matter retains its explicit spherical policy. This changes no native fog rendering, texture, alpha or depth equation.

## Combined installed-runtime observations

The final corrected-jar client completes **all 20 cases**, writes `done=true` with no failure, saves all 20 native screenshots, and exits zero. Inspect every captured image. It runs installed Forge 47.4.28 under Linux/Mesa/Xvfb, with master volume zero and no host game window.

| Cases | Actual observation |
| --- | --- |
| `neutral`, `combined` | Real checker terrain, enchanted held sword, native drowned/trader/llama, particles, Nether/End portals, gateway, rift, jar and glitch. Combined waves, surface coloration, pulses and post output visibly compose. |
| `stars`, `border` | Native star and border draws use their material exclusions: stars have no surface/waves; border has no surface and uses uniform pulse. |
| `blind_fog`, `darkness_fog` | Actual native impairment is present; compiled terrain/held uniforms select spherical bands. |
| `water_fog`, `snow_fog` | Actual camera fluid is `WATER` / `POWDER_SNOW`; native underwater/frozen overlays and spherical selection compose with mod output. |
| `boss_fog`, `nether_fog` | Actual native boss fog and Nether dimension fog are true. Terrain/held programs select sphere; returning to the Overworld restores ordinary cylinder selectors. |
| `fabulous`, `reload`, `resize`, `hud_hidden` | Native Fabulous targets, real resource reload, 854×480 → 960×540 and F1 render correctly in this scene. Reload replaces the zero-matter shader object. |
| `reggie_plain`, `reggie_distorted`, `reggie_composed`, `reggie_restored` | A fresh operator command/owner packet starts each comparison. The actual delayed voice queue emits through production local chat. Plain/restored output equals the canonical line; active alcohol/kava/Zero/cannabis changes it. The composed frame includes the resulting native post/glyph layer, geometry and distorted text together. Health remains 20. |
| `cleared`, `native_sober` | Normal checker terrain/color/hand return. All sampled geometry programs are disabled. The final period runs over 100 ticks without renderer/drug overrides; clearing forced channels alone is not its passing condition. |

The final result records **885 cumulative contextual caller-state checks** by the last case's verification. These include empty contextual passes as well as active Reggie rendering; this is not a count of 885 hallucination models. They compare native GL program, draw/read framebuffer, active texture, blend/depth/cull/scissor/stencil state, native shader/cache/color, render phase and caller pose. Apparition tint/render flag restore correctly. Actual NORMAL/POST program uniforms stay disabled, every complete frame returns to NORMAL, and sampled shader applications report no GL errors. Controlled fixture inputs do not establish natural dose onset or prove compatibility with other renderers.

Preserve the unsuccessful attempts separately:

1. The first composition run reaches twelve captures, then fails a shutdown-time sober assertion. It left category strengths positive while forcing only visible channels to zero. Clear both categories and selected effects, then add the native sober period with no visual overrides.
2. A second pre-distance-fix run passes fourteen composition/Reggie/cleanup cases. Its jar still has the pulse-distance mismatch; it is not the final corrected-jar result.
3. The first corrected-jar environmental run passes five fog cases, then hits a NightConfig `ConcurrentModificationException` during the Nether case. The stack is the QA sidecar's repeated `ConfigValue.set` autosave racing the file watcher. Move settings writes into case setup and remove watches only for this owned QA process's mod configs. Production settings UI behavior is unchanged.
4. A fourth run completes fifteen captures, then presents canonical text in the distorted Reggie case. The client-only drug override could be replaced by authoritative snapshots before the production dialogue tick. Set controlled state on both sides at entry and log actual emit-time values. The final callback records incoming distortion enabled and alcohol/kava/Zero/cannabis **0.6/0.2/0.8/0.6**, then **0.4/0.2/0.35/0.6** for composition, producing distorted text. Plain/restored callbacks record the toggle off and those drugs zero. The exact snapshot timing in the earlier failed run was not instrumented and is not retrospectively established.

Local evidence is retained under ignored `run/production/cycle36`: final `evidence/results.json` and `evidence/screenshots`, `client-attempt5.log` and `game5`; failed `evidence-attempt1`, `evidence-attempt3-fog-config-race` and `evidence-attempt4-client-dose-race`; separate passing old-jar `evidence-composition-before-fix`; and `pulse-before` / `pulse-after` GPU diagnostics. The separate sidecar is never packaged with production classes.

Java 17 `build runGameTestServer` exits zero: **92 unit tests**, zero failures/errors/skips, and **all 297 required GameTests pass**. Production SHA-256 is **`90af81aa4fb4cb6a0269fa9241ed1af67fabdf385c761458c48188b21cc040a0`**. Compared with C35, exactly two client class entries and the geometry vertex resource change; common/server classes are byte-identical. The C35 registration snapshot retains its original jar provenance, while its curated evidence links include this subsequent rendering work.

## Remaining work

Complete the broader two-player death/respawn/dimension/custom-benefit/natural-expiry matrix and remaining explicit native handoffs, then perform the full delivery audit. Oculus/modified BSL compatibility remains deferred at the user's request. Neither a valid model nor a successful controlled render completes the full port.
