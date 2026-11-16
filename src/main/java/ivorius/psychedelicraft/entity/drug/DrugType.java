package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.type.BrownShroomsDrug;
import ivorius.psychedelicraft.entity.drug.type.RedShroomsDrug;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class DrugType {
    private static final Map<String, DrugType> REGISTRY = new LinkedHashMap<>();
    public static final DrugType BROWN_SHROOMS = register("brown_shrooms", () -> new BrownShroomsDrug(1, 0.0002));
    public static final DrugType RED_SHROOMS = register("red_shrooms", () -> new RedShroomsDrug(1, 0.0002));

    private final String id;
    private final Supplier<Drug> factory;

    private DrugType(String id, Supplier<Drug> factory) {
        this.id = id;
        this.factory = factory;
    }

    private static DrugType register(String path, Supplier<Drug> factory) {
        DrugType type = new DrugType(Psychedelicraft.id(path).toString(), factory);
        REGISTRY.put(type.id, type);
        return type;
    }

    public static Optional<DrugType> byId(String id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static Collection<DrugType> values() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public String id() { return id; }
    public Drug create() { return factory.get(); }
}
