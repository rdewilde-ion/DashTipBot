package com.github.nija123098.tipbot.commands;

import com.github.nija123098.tipbot.command.AbstractCommand;
import com.github.nija123098.tipbot.command.Command;
import com.github.nija123098.tipbot.utility.Database;
import com.github.nija123098.tipbot.utility.TransactionLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.github.nija123098.tipbot.utility.Database.BALANCES;

public class WithdrawCommand extends AbstractCommand {
    @Override
    public String getHelp() {
        return "Withdraws an amount of ION with a specified address in the form ~withdraw <amount> <address>";
    }

    @Override
    public Command getCommand() {
        return (invoker, arguments, channel) -> {
            if (arguments.length == 0)
                return "Please specify an amount to withdraw amount in ION and the address to send it to.";
            BalanceCommand.update(invoker);
            double currentWallet = Double.parseDouble(Database.getValue(BALANCES, invoker, "0"));
            double withdrawAmount;
            boolean firstAmount = true;
            try {
                withdrawAmount = Double.parseDouble(arguments[0]);
            } catch (NumberFormatException ignored) {
                try {
                    withdrawAmount = Double.parseDouble(arguments[1]);
                    firstAmount = false;
                } catch (NumberFormatException ignoredAgain) {
                    return "Please specify an amount to withdraw and the address to send it to.";
                }
            }
            if (withdrawAmount < .0001D) return "Your withdraw amount must be greater than .0001 ION";
            if (currentWallet < withdrawAmount) return "Your balance is not that high.";
            TransactionLog.log("withdrawing " + withdrawAmount + " for user " + invoker.getStringID());
            if ((arguments[firstAmount ? 1 : 0]).length() == 3) return "You should leave out the unit type";
            String ret = sendMoney(arguments[firstAmount ? 1 : 0], withdrawAmount, invoker.getStringID()).trim();
            if (ret.matches("([0-9a-f])+")) {
                Database.setValue(BALANCES, invoker, String.valueOf((float)(currentWallet - withdrawAmount)));
            }
            return ret;
        };
    }

    private static String sendMoney(String address, Double amount, String userID) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("ion-cli", "sendtoaddress", address, String.valueOf(amount), "\"Standard Withdraw\"", "\"" + userID + "\"").start();
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        process.waitFor();
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = inputReader.readLine()) != null) builder.append(line).append("\n");
        builder.append("\n\n");
        while ((line = errorReader.readLine()) != null) builder.append(line).append("\n");
        return builder.toString();
    }
}
