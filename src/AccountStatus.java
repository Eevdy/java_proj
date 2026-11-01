public enum AccountStatus {
    ACTIVE("Активен"),
    BLOCKED("Заблокирован");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
