package utils;

public class InvestorSettings {
    // Investor profiles
    public static final int N_PROFILES = 3;
    public static final int TRADER = 0;
    public static final int SWING = 1;
    public static final int INVESTOR = 2;

    public static final int HOUR_PRICE = 0;
    public static final int DAY_PRICE = 1;
    public static final int WEEK_PRICE = 2;
    public static final int MONTH_PRICE = 3;

    // Prices multipliers that represent the different importance given to prices at a certain time
    // [0][x] - trader, [1][x] - swing, [2][x] - investor
    // [y][0] - hourPrice, [y][1] - dayPrice, [y][2] - weekPrice, [y][3] - monthPrice
    public static final float[][] PROFILE = {{0.8f, 0.2f, 0, 0}, {0.2f, 0.4f, 0.4f, 0.2f}, {0.0f, 0.1f, 0.2f, 0.7f}};

    public static final int INVESTOR_MAX_SKILL = 11;
    public static final int PORTFOLIO_SIZE = 10;
}
