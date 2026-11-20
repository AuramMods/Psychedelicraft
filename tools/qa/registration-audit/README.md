# Registration and asset audit

This separate Forge 47.4.28 / Java 17 QA mod observes the installed production jar. It is not included in the production source set or production jar. Install it only in an owned QA instance.

The native server export includes every nonempty mod registry exposed by `ForgeRegistries`, fluid types, dynamic registry IDs, loaded recipes, advancements, drug profiles and the three operator command trees. The client adds every Forge config leaf, native advancement criteria, default item model/name observations, every block state with `MODEL` render shape, and all four mod entity-renderer bindings. Native GUI rendering produces 60-item contact sheets. A real Polish language/resource reload repeats the client inventory and contact sheets.

The audit records observations after attempting the native operations. It does not gate launching or building the mod. Its successful result means the audit finished; inspect the generated report's findings and screenshots for coverage and actual errors. Missing assets are reported rather than hidden behind an overall success flag.

## Run in an isolated client

Prerequisites are Java 17, Docker, an installed Forge 47.4.28 client with its libraries/version metadata, and the Minecraft assets used by `tools/qa/run-client.sh`. The existing [background workflow](../../../docs/PORT_1201.md#background-runtime-workflow) describes the Linux/Mesa/Xvfb environment. The host receives no game window or audio.

From the repository root, build the real production mod and generate an owned native world:

```sh
./gradlew --no-daemon build runGameTestServer
```

Then build the sidecar from its own directory:

```sh
cd tools/qa/registration-audit
../../../gradlew --no-daemon build
cd ../../..
```

Use a fresh QA directory. The example below copies the completed GameTest world, so no playable Modrinth instance or previous private fixture is required:

```sh
audit_game=run/production/registration-audit
mkdir -p "$audit_game/saves" "$audit_game/mods"
cp -R run/gametest/world "$audit_game/saves/AuditQA"
cp tools/qa/registration-audit/build/libs/psychedelicraft-registration-audit.jar "$audit_game/mods/"
printf 'AuditQA\n' > "$audit_game/audit-world.txt"
docker build --platform linux/amd64 -t psychedelicraft-forge-qa:java17 -f tools/qa/Dockerfile .
sh tools/qa/run-client.sh run/production/cycle1/client "$audit_game" RegistryAudit owner
```

Replace `run/production/cycle1/client` with the installed Forge client directory if different. The launcher copies the current production jar, sets master volume zero, and keeps rendering on an isolated display. The sidecar opens the exact world named in `audit-world.txt`, clears the owned player's drug/potion state, disables ambient rift spawning in that QA config, and sets GUI scale one. It exits after both languages finish, or after recording an exception. An orderly process exit alone is not a passing audit: inspect `registration-audit/result.json` and the manifests.

Artifacts under the QA game directory:

- `registration-audit/manifest-en_us.json` and `manifest-pl_pl.json`: actual runtime observations.
- `registration-audit/screenshots/`: native default item renders in both languages.
- `registration-audit/result.json`: completion or the actual exception.
- `saves/AuditQA/registration-audit/server.json`: the server-side subset, also exported when this QA mod is used on an owned dedicated server.

Generate the current matrix after inspecting those artifacts:

```sh
python3 tools/qa/registration-audit/report.py \
  --english "$audit_game/registration-audit/manifest-en_us.json" \
  --polish "$audit_game/registration-audit/manifest-pl_pl.json" \
  --jar build/libs/psychedelicraft-1.20.1-port.2.jar \
  --output docs/PORT_1201_REGISTRY.md
```

The report reconciles the pinned accepted/original registry sources, including their known dynamic helpers. It is deliberately not a generic Java evaluator. Review source-mapping changes when updating the pinned revisions or adding new registration factories.

## Coverage limits

Default item rendering does not cover every NBT variant. Inspecting every baked block state does not prove every state's final world pixels. Quad/sprite sampling uses seed zero for all directions and the unculled face; it does not enumerate every weighted-model outcome. A renderer binding does not prove every entity animation, and a loaded recipe does not prove every ingredient combination. The report keeps earlier behavior evidence and unfinished combined-render/multiplayer/native integration work separate from these asset observations. It makes no Oculus or shader-pack compatibility claim.
