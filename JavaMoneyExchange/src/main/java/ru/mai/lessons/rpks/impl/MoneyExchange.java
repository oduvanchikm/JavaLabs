package ru.mai.lessons.rpks.impl;

import ru.mai.lessons.rpks.IMoneyExchange;
import ru.mai.lessons.rpks.exception.ExchangeIsImpossibleException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MoneyExchange implements IMoneyExchange {

    static class CoinCount {
        int denomination;
        int count;

        CoinCount(int denomination, int count) {
            this.count = count;
            this.denomination = denomination;
        }

        public String ResultToString() {
            return denomination + "[" + count + "]";
        }
    }

    private List<Integer> ParseCoin(String coinDenomination) {
        List<Integer> result = new ArrayList<>();

        List<String> resultInString = Arrays.stream(coinDenomination.split(",")).map(String::trim).toList();

        for (String string : resultInString) {
            try {
                int digit = Integer.parseInt(string);

                if (digit > 0) {
                    result.add(digit);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid coin denomination format: input contains non-numeric");
            }
        }

        return result;
    }

    private boolean FindExchange(int sum, List<Integer> coins, int index, List<CoinCount> result) {
        if (sum == 0) {
            return true;
        }

        if (sum < 0 || index >= coins.size()) {
            return false;
        }

        int coin = coins.get(index);
        for (int count = sum / coin; count >= 0; --count) {
            result.add(new CoinCount(coin, count));
            if (FindExchange(sum - coin * count, coins, index + 1, result)) {
                return true;
            }
            result.remove(result.size() - 1);
        }

        return false;
    }

    public String exchange(Integer sum, String coinDenomination) throws ExchangeIsImpossibleException {
        if (coinDenomination.isEmpty() || sum == 0) {
            throw new ExchangeIsImpossibleException("Exchange not possible: empty coin denomination or sum is zero ");
        }

        List<Integer> availableCoinForSum = ParseCoin(coinDenomination);
        if (availableCoinForSum.isEmpty()) {
            throw new ExchangeIsImpossibleException("Exchange not possible: no valid coin denominations provided");
        }

        availableCoinForSum.sort(Collections.reverseOrder());

        List<CoinCount> resultList = new ArrayList<>();
        boolean possible = FindExchange(sum, availableCoinForSum, 0, resultList);

        if (!possible) {
            throw new ExchangeIsImpossibleException("Exchange not possible: no valid combination found");
        }

        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < resultList.size(); i++) {
            if (resultList.get(i).count > 0) {
                resultBuilder.append(resultList.get(i).ResultToString());
                if (i < resultList.size() - 1) {
                    resultBuilder.append(", ");
                }
            }
        }

        return resultBuilder.toString();
    }
}
