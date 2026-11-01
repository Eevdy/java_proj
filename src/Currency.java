public enum Currency {
    RUB("RUB", "₽", 100),
    USD("USD", "$", 9_600),
    EUR("EUR", "€", 10_300),
    CNY("CNY", "¥", 1_320);

    private final String code;
    private final String symbol;
    private final long priceInRublesKopecks;

    Currency(
            String code,
            String symbol,
            long priceInRublesKopecks
    ) {
        this.code = code;
        this.symbol = symbol;
        this.priceInRublesKopecks = priceInRublesKopecks;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getPriceInRublesKopecks() {
        return priceInRublesKopecks;
    }
}
