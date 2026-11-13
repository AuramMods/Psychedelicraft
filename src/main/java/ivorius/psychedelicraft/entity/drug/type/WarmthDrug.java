package ivorius.psychedelicraft.entity.drug.type;

public final class WarmthDrug extends SimpleDrug {
    public WarmthDrug() {
        super(1, 0.004);
    }

    @Override
    public float bloomStrength() {
        return (float) getActiveValue() * 0.5F;
    }

    @Override
    public float superSaturationHallucinationStrength() {
        return (float) getActiveValue() * 0.1F;
    }
}
