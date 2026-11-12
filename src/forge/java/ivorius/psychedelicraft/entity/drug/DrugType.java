/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.type.AlcoholDrug;
import ivorius.psychedelicraft.entity.drug.type.BathSaltsDrug;
import ivorius.psychedelicraft.entity.drug.type.CannabisDrug;
import ivorius.psychedelicraft.entity.drug.type.CocaineDrug;
import ivorius.psychedelicraft.entity.drug.type.SleepDeprivationDrug;
import ivorius.psychedelicraft.entity.drug.type.TobaccoDrug;
import ivorius.psychedelicraft.entity.drug.type.BrownShroomsDrug;
import ivorius.psychedelicraft.entity.drug.type.CaffeineDrug;
import ivorius.psychedelicraft.entity.drug.type.LsdDrug;
import ivorius.psychedelicraft.entity.drug.type.PeyoteDrug;
import ivorius.psychedelicraft.entity.drug.type.RedShroomsDrug;
import ivorius.psychedelicraft.entity.drug.type.WarmthDrug;

public final class DrugType {
    private static final Map<String, DrugType> REGISTRY = new LinkedHashMap<>();

    public static final DrugType BROWN_SHROOMS = register("brown_shrooms", () -> new BrownShroomsDrug(1, 0.0002));
    public static final DrugType RED_SHROOMS = register("red_shrooms", () -> new RedShroomsDrug(1, 0.0002));
    public static final DrugType PEYOTE = register("peyote", () -> new PeyoteDrug(1, 0.0002));
    public static final DrugType ATROPINE = register("atropine", () -> new LsdDrug(1, 0.0003, true));
    public static final DrugType CAFFEINE = register("caffeine", () -> new CaffeineDrug(1, 0.0002));
    public static final DrugType WARMTH = register("warmth", WarmthDrug::new);
    public static final DrugType ALCOHOL = register("alcohol", AlcoholDrug::new);
    public static final DrugType KAVA = register("kava", AlcoholDrug::new);
    public static final DrugType CANNABIS = register("cannabis", CannabisDrug::new);
    public static final DrugType TOBACCO = register("tobacco", TobaccoDrug::new);
    public static final DrugType COCAINE = register("coccaine", CocaineDrug::new);
    public static final DrugType BATH_SALTS = register("bath_salts", BathSaltsDrug::new);
    public static final DrugType LSD = register("lsd", () -> new LsdDrug(1, 0.0003, false));
    public static final DrugType SUGAR = register("sugar", () -> new CaffeineDrug(1, 0.0002));
    public static final DrugType SLEEP_DEPRIVATION = register("sleep_deprivation", SleepDeprivationDrug::new);

    private final String id;
    private final Supplier<Drug> factory;

    private DrugType(String id, Supplier<Drug> factory) {
        this.id = id;
        this.factory = factory;
    }

    private static DrugType register(String path, Supplier<Drug> factory) {
        String id = Psychedelicraft.id(path).toString();
        DrugType type = new DrugType(id, factory);
        REGISTRY.put(id, type);
        return type;
    }

    public static Optional<DrugType> byId(String id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public String id() {
        return id;
    }

    public Drug create() {
        return factory.get();
    }
}
