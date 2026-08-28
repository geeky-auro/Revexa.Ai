package ai.revexa.identity.domain;

import ai.revexa.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_account")
public class UserAccount extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    /** UI preference: {@code system}, {@code light} or {@code dark}. */
    @Column(name = "theme", nullable = false, length = 16)
    private String theme = "system";

    @Column(name = "preferred_language", nullable = false, length = 32)
    private String preferredLanguage = "python";

    /**
     * How eagerly the mentor should reveal. {@code guided} keeps hints progressive (the default and
     * the product's whole point); {@code direct} lets power users jump further ahead.
     */
    @Column(name = "mentor_mode", nullable = false, length = 16)
    private String mentorMode = "guided";

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getMentorMode() {
        return mentorMode;
    }

    public void setMentorMode(String mentorMode) {
        this.mentorMode = mentorMode;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
