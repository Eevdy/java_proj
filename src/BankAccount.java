import exception.AccountBlockedException;
import exception.LimitExceededException;

import java.util.Objects;

public class BankAccount {
    private final String accountNumber;
    private final String ownerName;
    private Balance balance;
    private long withdrawalLimit;
    private AccountStatus status;

    public BankAccount(String accountNumber, String ownerName, Balance initialBalance, long withdrawalLimit) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Номер счета не может быть пустым");
        }
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("ФИО владельца счета не может быть пустым");
        }
        if (withdrawalLimit < 0) {
            throw new IllegalArgumentException("Лимит вывода не может быть отрицательным: " + withdrawalLimit);
        }

        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = Objects.requireNonNull(initialBalance, "Начальный баланс не может быть null");
        this.withdrawalLimit = withdrawalLimit;
        this.status = AccountStatus.ACTIVE;
    }

    public BankAccount(String accountNumber, String ownerName, long initialBalanceInKopecks) {
        this(accountNumber, ownerName, new Balance(initialBalanceInKopecks), 500_000_00L);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Balance getBalance() {
        return balance;
    }

    public long getWithdrawalLimit() {
        return withdrawalLimit;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isBlocked() {
        return status == AccountStatus.BLOCKED;
    }

    public void setWithdrawalLimit(long newLimit) {
        if (newLimit < 0) {
            throw new IllegalArgumentException("Лимит вывода не может быть отрицательным: " + newLimit);
        }
        this.withdrawalLimit = newLimit;
    }

    public void block(String reason) {
        this.status = AccountStatus.BLOCKED;
        System.out.printf("Счет %s (%s) заблокирован. Причина: %s%n", accountNumber, ownerName, reason);
    }

    public void unblock() {
        this.status = AccountStatus.ACTIVE;
        System.out.printf("Счет %s (%s) успешно разблокирован.%n", accountNumber, ownerName);
    }

    public void deposit(Balance amount) {
        Objects.requireNonNull(amount, "Сумма пополнения не может быть null");
        if (amount.getAmount() <= 0) {
            throw new IllegalArgumentException("Сумма пополнения должна быть строго больше нуля: " + amount);
        }
        ensureAccountActive("пополнить");
        this.balance = this.balance.add(amount);
    }

    public void deposit(long amountInKopecks) {
        if (amountInKopecks <= 0) {
            throw new IllegalArgumentException("Сумма пополнения должна быть строго больше нуля: " + amountInKopecks);
        }
        ensureAccountActive("пополнить");
        this.balance = this.balance.add(amountInKopecks);
    }

    public void withdraw(Balance amount) {
        Objects.requireNonNull(amount, "Сумма снятия не может быть null");
        if (amount.getAmount() <= 0) {
            throw new IllegalArgumentException("Сумма снятия должна быть строго больше нуля: " + amount);
        }
        ensureAccountActive("снять средства с");

        Balance inAccountCurrency = (this.balance.getCurrency() == amount.getCurrency())
                ? amount
                : amount.convertTo(this.balance.getCurrency());

        if (inAccountCurrency.getAmount() > withdrawalLimit) {
            throw new LimitExceededException(String.format(
                    "Превышен разовый лимит на вывод. Запрошено: %s, текущий лимит: %d.%02d %s",
                    inAccountCurrency, withdrawalLimit / 100, withdrawalLimit % 100, balance.getCurrency().getSymbol()
            ));
        }

        this.balance = this.balance.subtract(inAccountCurrency);
    }

    public void withdraw(long amountInKopecks) {
        withdraw(new Balance(amountInKopecks, this.balance.getCurrency()));
    }

    public void transfer(BankAccount targetAccount, Balance amount) {
        Objects.requireNonNull(targetAccount, "Целевой счет для перевода не может быть null");
        Objects.requireNonNull(amount, "Сумма перевода не может быть null");

        if (this == targetAccount || this.accountNumber.equals(targetAccount.accountNumber)) {
            throw new IllegalArgumentException("Нельзя перевести средства на тот же самый счет: " + accountNumber);
        }

        ensureAccountActive("совершить перевод со");

        if (targetAccount.isBlocked()) {
            throw new AccountBlockedException(String.format(
                    "Невозможно совершить перевод: счет получателя %s (%s) заблокирован",
                    targetAccount.getAccountNumber(), targetAccount.getOwnerName()
            ));
        }

        this.withdraw(amount);

        try {
            targetAccount.deposit(amount);
        } catch (RuntimeException e) {
            this.balance = this.balance.add(amount);
            throw e;
        }
    }

    public void transfer(BankAccount targetAccount, long amountInKopecks) {
        transfer(targetAccount, new Balance(amountInKopecks, this.balance.getCurrency()));
    }

    private void ensureAccountActive(String operationName) {
        if (isBlocked()) {
            throw new AccountBlockedException(String.format(
                    "Невозможно %s счета: счет %s (%s) заблокирован",
                    operationName, accountNumber, ownerName
            ));
        }
    }

    @Override
    public String toString() {
        return String.format(
                "BankAccount[№ %s | Владелец: %s | Баланс: %s | Лимит вывода: %d.%02d %s | Статус: %s]",
                accountNumber, ownerName, balance,
                withdrawalLimit / 100, withdrawalLimit % 100, balance.getCurrency().getSymbol(),
                status
        );
    }
}
