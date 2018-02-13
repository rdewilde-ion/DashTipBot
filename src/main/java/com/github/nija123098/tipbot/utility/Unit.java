package com.github.nija123098.tipbot.utility;

import com.google.gson.JsonParser;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

public enum Unit {
    BEER(() -> dollarToION(3.99F)),
    COFFEE(() -> dollarToION(2.99F)),
    TEA(() -> dollarToION(2.99F)),
    COOKIE(() -> dollarToION(.50F)),
    TOOTHPASTE(() -> dollarToION(1.99F)),
    ION(() -> 1D),
    USD(() -> dollarToION(1), "$"),
    EUR(() -> getUnitToION("EUR"), "€"),
    GBP(() -> getUnitToION("GBP"), "£"),
    RUB(() -> getUnitToION("RUB"), (amount) -> amount + "₽", "₽", "ruble"),
    CAD(() -> getUnitToION("CAD"), "C$", "Can$"),
    SGD(() -> getUnitToION("SGD"), "S$"),
    JPY(() -> getUnitToION("JPY")),
    AUD(() -> getUnitToION("AUD"), "A$", "AU$"),
    RON(() -> getUnitToION("RON")),
    CNY(() -> getUnitToION("CNY")),
    CZK(() -> getUnitToION("CZK"), "Kč"),
    CHF(() -> getUnitToION("CHF")),
    BGN(() -> getUnitToION("BGN")),
    PLN(() -> getUnitToION("PLN"), "zł", "zloty", "zlotys"),
    MYR(() -> getUnitToION("MYR")),
    ZAR(() -> getUnitToION("ZAR")),// 14.46
    SEK(() -> getUnitToION("SEL"), "kr"),
    INR(() -> getUnitToION("INR"), "₹"),
    HKD(() -> getUnitToION("HKD"), "HK$"),
    BRL(() -> getUnitToION("BRL"), "R$"),
    PKR(() -> getUnitToION("PKR")),
    MXN(() -> getUnitToION("MXN"), "Mex$"),;

    Unit(ToIONAmount ionAmount, String... names) {
        this(ionAmount, 2, names);
    }

    Unit(ToIONAmount ionAmount, int decimals, String... names) {
        this(ionAmount, null, names);
        this.display = (amount) -> {
            String decimal = Unit.displayAmount(amount, decimals);
            return this.names.size() > 1 ? this.names.get(1) + decimal : (decimal + " " + this.names.get(0));
        };
    }

    Unit(ToIONAmount ionAmount, Function<Double, String> display, String... names) {
        this.ionAmount = ionAmount;
        this.display = display;
        this.names.add(this.name());
        Collections.addAll(this.names, names);
    }

    private final ToIONAmount ionAmount;
    private Function<Double, String> display;
    private final List<String> names = new ArrayList<>(1);

    public String display(double amount) {
        return this.display.apply(amount);
    }

    public Double getIONAmount() {
        return this.ionAmount.getAmount();
    }

    private interface ToIONAmount {
        Double getAmount();
    }

    private static double dollarToION(float amount) {
        return amount / getIONValue("USD");
    }

    public static double getUnitToION(String currency) {
        return 1 / getIONValue(currency);
    }

    private static final AtomicReference<Float> ION_VALUE = new AtomicReference<>();
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

    private static float getIONValue(String currency) {
        if (ION_VALUE.get() == null) {
            try {
                EXECUTOR_SERVICE.schedule(() -> ION_VALUE.set(null), 1, TimeUnit.MINUTES);
                // TODO
                ION_VALUE.set(Float.parseFloat(String.valueOf(new JsonParser().parse(Unirest.get("https://min-api.cryptocompare.com/data/price?fsym=ION&tsyms=" + currency).asString().getBody()).getAsJsonObject().get(currency))));
            } catch (UnirestException e) {
                throw new WrappingException(e);
            }
        }
        return ION_VALUE.get();
    }

    private static final Set<String> NAMES = new HashSet<>();

    static {
        Stream.of(Unit.values()).forEach(unit -> NAMES.addAll(unit.names));
    }

    public static Set<String> getNames() {
        return NAMES;
    }

    private static final Map<String, Unit> NAME_MAP = new HashMap<>();

    static {
        Stream.of(Unit.values()).forEach(unit -> unit.names.forEach(name -> NAME_MAP.put(name.toLowerCase(), unit)));
    }

    public static Unit getUnitForName(String name) {
        return NAME_MAP.get(name.toLowerCase());
    }

    public static String displayAmount(double amount, int decimals) {
        String s = String.valueOf(amount);
        int index = s.indexOf("."), targetLength = index + decimals;
        if (decimals < 1) return s.substring(0, index);
        StringBuilder builder = new StringBuilder(s.substring(0, Math.min(s.length(), targetLength + 1)));
        for (int i = s.length() - 1; i < targetLength; i++) builder.append("0");
        return builder.toString();
    }
}
