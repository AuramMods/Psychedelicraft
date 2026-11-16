/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.config.PSConfig;

import java.util.function.Predicate;

final class BrewProfiles {
    private static final Predicate<BrewProfile.State> ANY = s -> true;
    private static final Predicate<BrewProfile.State> BASE = s -> !s.vinegar
            && s.fermentation == 0 && s.distillation == 0 && s.maturation == 0;
    private static final Predicate<BrewProfile.State> VINEGAR = s -> s.vinegar;
    private static final Predicate<BrewProfile.State> DISTILLED = s -> !s.vinegar && s.distillation > 0;
    private static final Predicate<BrewProfile.State> MATURED = s -> !s.vinegar && s.maturation > 0;
    private static final Predicate<BrewProfile.State> PARTIAL = s -> !s.vinegar && s.fermentation == 1;
    private static final Predicate<BrewProfile.State> FERMENTED = s -> !s.vinegar && s.fermentation == 2;
    private static final int DISTILLED_COLOR = 0x33ffffff;
    private static final int MATURE_COLOR = 0xcc592518;

    private BrewProfiles() {
    }

    static BrewProfile wheatHop() {
        return new BrewProfile("wheat_hop", "beer", 0xaafeaa08, 1,
                () -> PSConfig.FLUID_TICKS.get("wheat_hop").snapshot(), DISTILLED_COLOR, MATURE_COLOR,
                0.25, 1.7, 0.1,
                style("wort", "bitter", "beer", "wort", BASE),
                style("beer_vinegar", "bitter", "clear", "vinegar", VINEGAR),
                style("beer", null, "clear", "beer", DISTILLED),
                style("beer", null, "beer", "beer", MATURED),
                style("half_wash", "bitter", "rum_semi_mature", "half_wash", PARTIAL),
                style("beer", "green", "rum_mature", "beer", FERMENTED));
    }

    static BrewProfile wheat() {
        return new BrewProfile("wheat", "beer", 0xaafeaa08, 1,
                () -> PSConfig.FLUID_TICKS.get("wheat").snapshot(), DISTILLED_COLOR, MATURE_COLOR,
                0.25, 1.7, 0.1,
                style("wort", null, "beer", "wort", BASE),
                style("beer_vinegar", null, "clear", "vinegar", VINEGAR),
                style("wheat_whiskey", null, "clear", "whiskey", s -> !s.vinegar && s.maturation > 0 && s.distillation > 0),
                style("vodka", null, "clear", "vodka", DISTILLED),
                style("wash", null, "rum_mature", "wash", MATURED),
                style("half_wash", null, "rum_semi_mature", "half_wash", PARTIAL),
                style("wash", null, "rum_mature", "wash", FERMENTED));
    }

    static BrewProfile potato() {
        return new BrewProfile("potato", "beer", 0xaafeaa08, 2,
                () -> PSConfig.FLUID_TICKS.get("potato").snapshot(), DISTILLED_COLOR, MATURE_COLOR,
                0.45, 1.9, 0.15,
                style("wort", null, "beer", "wort", BASE),
                style("vinegar", null, "clear", "vinegar", VINEGAR),
                style("poteen", null, "rum_semi_mature", "poteen", s -> !s.vinegar && s.maturation > 0 && s.distillation > 0),
                style("vodka", null, "clear", "vodka", DISTILLED),
                style("beer", null, "beer", "beer", MATURED),
                style("half_wash", null, "rum_semi_mature", "half_wash", PARTIAL),
                style("beer", null, "rum_mature", "beer", FERMENTED));
    }

    static BrewProfile tomato() {
        return new BrewProfile("tomato", "tomato_juice", 0xaaffaa08, 1,
                () -> PSConfig.FLUID_TICKS.get("tomato").snapshot(), 0xaaff0000, 0xaaff3300,
                0.15, 0.9, 0.05,
                style("ketchup", null, "ketchup", "ketchup", s -> s.vinegar && s.fermentation == 0 && s.maturation >= 1 && s.maturation <= 4),
                style("whiskey", null, "rum_semi_mature", "whiskey", s -> !s.vinegar && s.fermentation > 0 && s.maturation >= 6 && s.distillation >= 3),
                style("mead", null, "mead", "mead", s -> !s.vinegar && s.fermentation > 0 && s.maturation >= 5 && s.distillation == 0),
                style("beer", null, "beer", "beer", s -> !s.vinegar && s.fermentation > 0 && s.distillation == 1),
                style("vinegar", null, "clear", "vinegar", VINEGAR),
                style("vodka", null, "clear", "vodka", DISTILLED),
                style("juice", null, "tomato_juice", "juice", ANY));
    }

    static BrewProfile redGrapes() {
        return new BrewProfile("red_grapes", "wine", 0xaafeaa08, 1,
                () -> PSConfig.FLUID_TICKS.get("red_grapes").snapshot(), 0x993f0822, 0xee3f0822,
                0.55, 1.7, 0.2,
                style("juice", null, "wine", "juice", BASE),
                style("vinegar", null, "clear", "vinegar", VINEGAR),
                style("brandy", null, "rum_semi_mature", "brandy", DISTILLED),
                style("vinegar", null, "clear", "vinegar", s -> !s.vinegar && s.maturation >= 16),
                style("wine", "well_aged", "wine", "wine", s -> !s.vinegar && s.maturation >= 14),
                style("wine", "aged", "wine", "wine", s -> !s.vinegar && s.maturation >= 8),
                style("wine", "slightly_aged", "wine", "wine", s -> !s.vinegar && s.maturation >= 4),
                style("wine", "young", "wine", "wine", MATURED),
                style("half_wash", "wine", "wine", "half_wash", PARTIAL),
                style("wash", "wine", "wine", "wash", FERMENTED));
    }

    static BrewProfile rice() {
        return new BrewProfile("rice", "rice_wine", 0xeecac4b2, 1,
                () -> PSConfig.FLUID_TICKS.get("rice").snapshot(), DISTILLED_COLOR, 0x88d6bc90,
                0.25, 1.7, 0.1,
                style("wort", null, "rice_wine", "wort", BASE),
                style("vinegar", null, "clear", "vinegar", VINEGAR),
                style("brandy", null, "clear", "brandy", DISTILLED),
                style("wine", null, "clear", "wine", MATURED),
                style("half_wash", null, "clear", "half_wash", PARTIAL),
                style("wine", "young", "clear", "wine", FERMENTED));
    }

    static BrewProfile juniper() {
        return new BrewProfile("juniper", "slurry", 0xcc704e21, 1,
                () -> PSConfig.FLUID_TICKS.get("juniper").snapshot(), DISTILLED_COLOR, MATURE_COLOR,
                0.4, 1.7, 0.1,
                style("wort", null, "slurry", "wort", BASE),
                style("vinegar", null, "clear", "vinegar", VINEGAR),
                style("gin", null, "clear", "gin", DISTILLED),
                style("wash", null, "rum_mature", "wash", MATURED),
                style("half_wash", null, "rum_semi_mature", "half_wash", PARTIAL),
                style("wash", "young", "rum_mature", "wash", FERMENTED));
    }

    static BrewProfile honey() {
        return new BrewProfile("honey", "mead", 0xbbe9ae3b, 5,
                () -> PSConfig.FLUID_TICKS.get("honey").snapshot(), 0x99e9ae3b, 0xaad1984d,
                0.35, 1.7, 0.1,
                style("wort", null, "mead", "wort", BASE),
                style("vinegar", null, "mead", "vinegar", VINEGAR),
                style("brandy", null, "mead", "brandy", DISTILLED),
                style("mead", null, "mead", "mead", MATURED),
                style("half_wash", null, "mead", "half_wash", PARTIAL),
                style("mead", "young", "mead", "mead", FERMENTED));
    }

    static BrewProfile sugarCane() {
        return new BrewProfile("sugar_cane", "clear", 0xaafeaa08, 1,
                () -> PSConfig.FLUID_TICKS.get("sugar_cane").snapshot(), DISTILLED_COLOR, MATURE_COLOR,
                0.35, 1.7, 0.1,
                style("wort", null, "clear", "wort", BASE),
                style("vinegar", null, "clear", "vinegar", VINEGAR),
                style("rum", "young", "rum_semi_mature", "rum", s -> !s.vinegar && s.distillation > 0 && s.maturation == 0),
                style("rum", null, "rum_semi_mature", "rum", DISTILLED),
                style("basi", null, "clear", "basi", MATURED),
                style("half_wash", null, "rum_semi_mature", "half_wash", PARTIAL),
                style("basi", "young", "clear", "basi", FERMENTED));
    }

    static BrewProfile corn() {
        return new BrewProfile("corn", "beer", 0xaafeaa08, 1,
                () -> PSConfig.FLUID_TICKS.get("corn").snapshot(), DISTILLED_COLOR, MATURE_COLOR,
                0.25, 1.7, 0.1,
                style("wort", null, "beer", "wort", BASE),
                style("vinegar", null, "clear", "vinegar", VINEGAR),
                style("vodka", null, "clear", "vodka", s -> !s.vinegar && s.maturation == 0 && s.distillation > 0),
                style("whiskey", null, "clear", "whiskey", s -> !s.vinegar && s.maturation > 0 && s.distillation > 0),
                style("beer", null, "beer", "beer", MATURED),
                style("half_wash", null, "rum_semi_mature", "half_wash", PARTIAL),
                style("beer", "green", "beer", "beer", FERMENTED));
    }

    static BrewProfile apple() {
        return new BrewProfile("apple", "cider", 0x99edc13b, 1,
                () -> PSConfig.FLUID_TICKS.get("apple").snapshot(), 0x66edc13b, 0x88edc13b,
                0.35, 1.7, 0.1,
                style("cider", "sweet", "cider", "cider", BASE),
                style("vinegar", null, "clear", "vinegar", VINEGAR),
                style("brandy", null, "cider", "brandy", DISTILLED),
                style("cider", null, "cider", "cider", MATURED),
                style("cider", "half_sweet", "cider", "cider", PARTIAL),
                style("cider", "hard", "cider", "cider", FERMENTED));
    }

    static BrewProfile pineapple() {
        return new BrewProfile("pineapple", "cider", 0x99edc13b, 2,
                () -> PSConfig.FLUID_TICKS.get("pineapple").snapshot(), 0x66edc13b, 0x88edc13b,
                0.35, 1.7, 0.1,
                style("juice", null, "cider", "juice", BASE),
                style("vinegar", null, "clear", "vinegar", VINEGAR),
                style("brandy", null, "cider", "brandy", DISTILLED),
                style("wine", null, "cider", "wine", MATURED),
                style("half_wash", null, "cider", "half_wash", PARTIAL),
                style("wine", "young", "cider", "wine", FERMENTED));
    }

    static BrewProfile banana() {
        return new BrewProfile("banana", "mead", 0xbbe9ae3b, 3,
                () -> PSConfig.FLUID_TICKS.get("banana").snapshot(), 0x99e9ae3b, 0xaad1984d,
                0.35, 1.7, 0.1,
                style("juice", null, "mead", "juice", BASE),
                style("vinegar", null, "clear", "vinegar", VINEGAR),
                style("brandy", null, "mead", "brandy", DISTILLED),
                style("beer", null, "mead", "beer", MATURED),
                style("half_wash", null, "mead", "half_wash", PARTIAL),
                style("beer", null, "mead", "beer", FERMENTED));
    }

    static BrewProfile milk() {
        return new BrewProfile("milk", "rice_wine", 0x88ffffff, 2,
                () -> PSConfig.FLUID_TICKS.get("milk").snapshot(), 0x77cac4b2, 0x88d6bc90,
                0.35, 1.7, 0.1,
                style("wort", null, "rice_wine", "wort", BASE),
                style("vinegar", null, "rice_wine", "vinegar", VINEGAR),
                style("arkhi", null, "rice_wine", "arkhi", DISTILLED),
                style("blaand", null, "rice_wine", "blaand", MATURED),
                style("half_wash", null, "rice_wine", "half_wash", PARTIAL),
                style("blaand", null, "rice_wine", "blaand", FERMENTED));
    }

    static BrewProfile agave() {
        return new BrewProfile("agave", "tea", 0x779bab62, 1,
                () -> PSConfig.FLUID_TICKS.get("agave").snapshot(), 0x779beb62, 0x779beb62,
                0.15, 1.5, 0.05,
                style("mezcal", null, "rum_semi_mature", "mezcal", s -> !s.vinegar && s.fermentation > 0 && s.maturation == 0 && s.distillation == 1),
                style("tequila", "blanco", "clear", "tequila", s -> !s.vinegar && s.fermentation > 0 && s.maturation == 0 && s.distillation >= 2),
                style("tequila", "reposado", "clear", "tequila", s -> !s.vinegar && s.fermentation > 0 && s.maturation > 0 && s.distillation >= 2),
                style("juice", null, "clear", "juice", ANY));
    }

    private static BrewProfile.Style style(String name, String variant, String appearance, String symbol,
                                         Predicate<BrewProfile.State> condition) {
        return new BrewProfile.Style(name, variant, appearance, symbol, condition);
    }
}
