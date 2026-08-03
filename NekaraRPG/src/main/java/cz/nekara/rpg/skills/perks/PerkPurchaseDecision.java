package cz.nekara.rpg.skills.perks;

public record PerkPurchaseDecision(PerkPurchaseStatus status, int availablePoints) {
    public boolean allowed() {
        return status == PerkPurchaseStatus.PURCHASED;
    }
}
