import exception.InsufficientFundsException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

public final class Balance implements Comparable<Balance> {
    private final long amount;
    private final Currency currency;

    public Balance(long amount, Currency currency) {
        if (amount < 0) {
            throw new IllegalArgumentException("Сумма баланса не может быть отрицательной: " + amount);
        }
        this.currency = Objects.requireNonNull(currency, "Валюта не может быть null");
        this.amount = amount;
    }

    public Balance(long amountInKopecks) {
        this(amountInKopecks, Currency.RUB);
    }

    public static Balance zero(Currency currency) {
        return new Balance(0, currency);
    }

    public static Balance zero() {
        return new Balance(0, Currency.RUB);
    }

    public static Balance ofRubles(long rubles) {
        if (rubles < 0) {
            throw new IllegalArgumentException("Сумма рублей не может быть отрицательной: " + rubles);
        }
        return new Balance(Math.multiplyExact(rubles, 100L), Currency.RUB);
    }

    public long getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Balance add(Balance other) {
        Objects.requireNonNull(other, "Прибавляемый баланс не может быть null");
        Balance toAdd = (this.currency == other.currency) ? other : other.convertTo(this.currency);
        long newAmount = Math.addExact(this.amount, toAdd.amount);
        return new Balance(newAmount, this.currency);
    }

    public Balance add(long amountToAdd) {
        if (amountToAdd < 0) {
            throw new IllegalArgumentException("Нельзя прибавить отрицательную сумму: " + amountToAdd);
        }
        long newAmount = Math.addExact(this.amount, amountToAdd);
        return new Balance(newAmount, this.currency);
    }

    public Balance subtract(Balance other) {
        Objects.requireNonNull(other, "Вычитаемый баланс не может быть null");
        Balance toSubtract = (this.currency == other.currency) ? other : other.convertTo(this.currency);
        if (this.amount < toSubtract.amount) {
            throw new InsufficientFundsException(String.format(
                    "Недостаточно средств: на балансе %s, запрошено списание %s",
                    this, toSubtract
            ));
        }
        return new Balance(this.amount - toSubtract.amount, this.currency);
    }

    public Balance subtract(long amountToSubtract) {
        if (amountToSubtract < 0) {
            throw new IllegalArgumentException("Нельзя вычесть отрицательную сумму: " + amountToSubtract);
        }
        if (this.amount < amountToSubtract) {
            throw new InsufficientFundsException(String.format(
                    "Недостаточно средств: на балансе %s, запрошено списание %d.%02d %s",
                    this, amountToSubtract / 100, amountToSubtract % 100, currency.getSymbol()
            ));
        }
        return new Balance(this.amount - amountToSubtract, this.currency);
    }

    public Balance convertTo(Currency targetCurrency) {
        Objects.requireNonNull(targetCurrency, "Целевая валюта не может быть null");
        if (this.currency == targetCurrency) {
            return this;
        }

        BigDecimal sourceAmount = BigDecimal.valueOf(this.amount);
        BigDecimal sourcePrice = BigDecimal.valueOf(this.currency.getPriceInRublesKopecks());
        BigDecimal targetPrice = BigDecimal.valueOf(targetCurrency.getPriceInRublesKopecks());

        long convertedAmount = sourceAmount.multiply(sourcePrice)
                .divide(targetPrice, 0, RoundingMode.HALF_UP)
                .longValue();

        return new Balance(convertedAmount, targetCurrency);
    }

    public Balance convertTo(Currency targetCurrency, long customTargetPriceInRublesKopecks) {
        Objects.requireNonNull(targetCurrency, "Целевая валюта не может быть null");
        if (customTargetPriceInRublesKopecks <= 0) {
            throw new IllegalArgumentException("Курс валюты должен быть положительным: " + customTargetPriceInRublesKopecks);
        }

        BigDecimal sourceAmount = BigDecimal.valueOf(this.amount);
        BigDecimal sourcePrice = BigDecimal.valueOf(this.currency.getPriceInRublesKopecks());
        BigDecimal targetPrice = BigDecimal.valueOf(customTargetPriceInRublesKopecks);

        long convertedAmount = sourceAmount.multiply(sourcePrice)
                .divide(targetPrice, 0, RoundingMode.HALF_UP)
                .longValue();

        return new Balance(convertedAmount, targetCurrency);
    }

    @Override
    public int compareTo(Balance o) {
        Objects.requireNonNull(o, "Сравниваемый баланс не может быть null");
        if (this.currency == o.currency) {
            return Long.compare(this.amount, o.amount);
        }
        long thisInRubles = this.amount * this.currency.getPriceInRublesKopecks();
        long otherInRubles = o.amount * o.currency.getPriceInRublesKopecks();
        return Long.compare(thisInRubles, otherInRubles);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Balance balance)) return false;
        return amount == balance.amount && currency == balance.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%d.%02d %s", amount / 100, Math.abs(amount % 100), currency.getSymbol());
    }
}
