package ivorius.psychedelicraft.entity.drug.type;

abstract class MushroomDrug extends SimpleDrug {
    MushroomDrug(double decreaseSpeed, double decreaseSpeedPlus) {
        super(decreaseSpeed, decreaseSpeedPlus);
    }

    @Override
    protected double maximumActiveValue() {
        return 2;
    }

    @Override
    public double getActiveValue() {
        return Math.min(1, super.getActiveValue());
    }

    @Override
    public float mobAvoidanceChance() {
        double intensity = super.getActiveValue();
        // One overlapping dose peaks near 0.38 after decay during absorption.
        double absorptionLoss = Math.min(0.121, intensity * (0.121 / 0.38));
        return (float) ((intensity + absorptionLoss) / 2);
    }
}
