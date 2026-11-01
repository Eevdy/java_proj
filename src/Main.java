import exception.AccountBlockedException;
import exception.InsufficientFundsException;
import exception.LimitExceededException;

public class Main {
    public static void main(String[] args) {
        System.out.println(">>> 1. Неизменяемый класс Balance:");
        Balance initialRub = Balance.ofRubles(10_000);
        System.out.println("Начальный баланс: " + initialRub);

        Balance added = initialRub.add(Balance.ofRubles(2_500));
        System.out.println("После add(2 500 ₽): " + added);
        System.out.println("Исходный объект не изменился (immutable): " + initialRub);

        Balance subtracted = added.subtract(Balance.ofRubles(1_200));
        System.out.println("После subtract(1 200 ₽): " + subtracted);

        System.out.println("\n>>> 2. Конвертация валют (по курсам enum Currency):");
        Balance rubBalance = Balance.ofRubles(96_000);
        System.out.println("Исходная сумма в рублях: " + rubBalance);
        System.out.println("В долларах США (USD):     " + rubBalance.convertTo(Currency.USD));
        System.out.println("В евро (EUR):             " + rubBalance.convertTo(Currency.EUR));
        System.out.println("В китайских юанях (CNY):  " + rubBalance.convertTo(Currency.CNY));

        Balance customUsd = rubBalance.convertTo(Currency.USD, 9_000);
        System.out.println("Конвертация в USD по спецкурсу (1$ = 90.00 ₽): " + customUsd);

        System.out.println("\n>>> 3. Создание банковских счетов:");
        BankAccount account1 = new BankAccount(
                "40817810000000000001",
                "Родионычев Анатолий Александрович",
                Balance.ofRubles(150_000),
                50_000_00L
        );

        BankAccount account2 = new BankAccount(
                "40817810000000000002",
                "Иванов Иван Иванович",
                Balance.ofRubles(20_000),
                30_000_00L
        );

        System.out.println("Счет 1: " + account1);
        System.out.println("Счет 2: " + account2);

        System.out.println("\n>>> 4. Пополнение и снятие со счета:");
        account1.deposit(10_000_00L);
        System.out.println("Счет 1 после пополнения на 10 000 ₽: " + account1.getBalance());

        account1.withdraw(5_000_00L);
        System.out.println("Счет 1 после снятия 5 000 ₽: " + account1.getBalance());

        System.out.println("\n>>> 5. Перевод между счетами (Анатолий -> Иван):");
        System.out.println("Баланс отправителя до перевода: " + account1.getBalance());
        System.out.println("Баланс получателя до перевода:  " + account2.getBalance());

        account1.transfer(account2, Balance.ofRubles(25_000));
        System.out.println("Перевод 25 000 ₽ успешно выполнен!");
        System.out.println("Баланс отправителя после перевода: " + account1.getBalance());
        System.out.println("Баланс получателя после перевода:  " + account2.getBalance());

        System.out.println("\n>>> 6. Проверка корнер-кейсов и валидации:");

        testCase("1. Попытка добавить отрицательную сумму к балансу", () -> {
            account1.deposit(-500);
        });

        testCase("2. Попытка перевода сверх установленного лимита (лимит 50 000 ₽, перевод 60 000 ₽)", () -> {
            account1.transfer(account2, Balance.ofRubles(60_000));
        });

        testCase("3. Попытка снять больше, чем есть на балансе (баланс счёта 2: 45 000 ₽, снятие: 50 000 ₽)", () -> {
            account2.setWithdrawalLimit(100_000_00L);
            account2.withdraw(Balance.ofRubles(50_000));
        });

        testCase("4. Попытка перевода средств на свой же счет", () -> {
            account1.transfer(account1, Balance.ofRubles(1_000));
        });

        testCase("5. Передача null в целевой аккаунт при переводе", () -> {
            account1.transfer(null, Balance.ofRubles(1_000));
        });

        testCase("6. Перевод средств на заблокированный счет", () -> {
            account2.block("Подозрительная активность в системе безопасности");
            System.out.println("Статус счета 2: " + account2.getStatus());
            account1.transfer(account2, Balance.ofRubles(5_000));
        });

        System.out.println("\n>>> Восстановление: разблокируем счет получателя и повторяем операцию:");
        account2.unblock();
        account1.transfer(account2, Balance.ofRubles(5_000));
        System.out.println("Перевод 5 000 ₽ успешно завершен после разблокировки!");
        System.out.println("Итоговый баланс счета 1: " + account1.getBalance());
        System.out.println("Итоговый баланс счета 2: " + account2.getBalance());
    }

    private static void testCase(String title, Runnable action) {
        System.out.println("\n[ТЕСТ] " + title);
        try {
            action.run();
            System.out.println("ОШИБКА: Исключение НЕ было выброшено!");
        } catch (IllegalArgumentException | InsufficientFundsException |
                 LimitExceededException | AccountBlockedException | NullPointerException e) {
            System.out.printf("  Перехвачено ожидаемое исключение [%s]: %s%n",
                    e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            System.out.printf("  Перехвачено непредвиденное исключение [%s]: %s%n",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
