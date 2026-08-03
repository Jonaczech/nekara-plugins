package cz.nekara.rpg.skills.profile;

public class SkillStorageException extends RuntimeException {
    public SkillStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public SkillStorageException(String message) {
        super(message);
    }
}
