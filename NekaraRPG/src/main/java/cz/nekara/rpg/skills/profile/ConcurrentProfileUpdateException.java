package cz.nekara.rpg.skills.profile;

public final class ConcurrentProfileUpdateException extends SkillStorageException {
    public ConcurrentProfileUpdateException(String playerKey, long expectedRevision, long actualRevision) {
        super("Concurrent skill profile update for " + playerKey
            + ": expected revision " + expectedRevision + " but found " + actualRevision);
    }
}
