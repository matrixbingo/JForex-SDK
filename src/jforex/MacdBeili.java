package jforex;


import com.dukascopy.api.*;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IIndicators.AppliedPrice;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.ISignalUpChartObject;
import com.dukascopy.api.indicators.IIndicator;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@RequiresFullAccess
@Library("C:\\Work\\.m2\\repository\\com\\google\\guava\\guava\\21.0\\guava-21.0.jar;C:\\Work\\.m2\\repository\\org\\apache\\commons\\commons-lang3\\3.4\\commons-lang3-3.4.jar")
public class MacdBeili implements IStrategy {

    private int counter = 0;
    final private int capacity = 1000000;
    public static final int HIST = 2;
    private static final String DATE_FORMAT_NOW = "yyyyMMdd_HHmmss";

    private IOrder order;
    private IChart chart;
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IBar currBar;



    @Configurable("Instrument")
    public Instrument instrument = Instrument.EURUSD;
    @Configurable("defaultPeriod")
    public Period defaultPeriod = Period.THIRTY_MINS;
    @Configurable("Offer side")
    public OfferSide offerSide = OfferSide.ASK;
    @Configurable("Slippage")
    public double slippage = 1;
    @Configurable("Amount")
    public double amount = 0.02;
    @Configurable("Take profit pips")
    public int takeProfitPips = 50;
    @Configurable("Stop loss in pips")
    public int stopLossPips = 10;
    @Configurable("Applied price")
    public AppliedPrice appliedPrice = AppliedPrice.CLOSE;
    @Configurable("Fast period")
    public int fastMACDPeriod = 12;
    @Configurable("Slow period")
    public int slowMACDPeriod = 26;
    @Configurable("Signal period")
    public int signalMACDPeriod = 9;
    @SuppressWarnings("serial")
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") {
        {
            setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    };


    @Override
    public void onStart(IContext context) throws JFException {
        this.context = context;
        this.console = context.getConsole();
        this.indicators = context.getIndicators();
        this.history = context.getHistory();
        this.engine = context.getEngine();

        this.chart = context.getChart(instrument);
        if (this.chart == null) {
            this.console.getErr().println("No chart opened, can't plot indicators.");
            this.context.stop();
        }else if(this.chart != null) {
            chart.add(indicators.getIndicator("MACD"), new Object[]{fastMACDPeriod, slowMACDPeriod, signalMACDPeriod});
        }
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        this.currBar = askBar;
        //this.console.getInfo().println(this.getCurrentTime(this.currBar.getTime()));
        if (period != this.defaultPeriod || instrument != this.instrument) {
            return;
        }
        if(this.isDiBeili(instrument)){

        }
    }

    class MacdDto {
        private int shift;
        private double[] macd;

        public MacdDto(int shift, double[] macd){
            this.shift = shift;
            this.macd = macd;
        }

        public int getShift() {
            return shift;
        }

        public void setShift(int shift) {
            this.shift = shift;
        }

        public double[] getMacd() {
            return macd;
        }

        public void setMacd(double[] macd) {
            this.macd = macd;
        }
    }

    private boolean isDiBeili(Instrument instrument) throws JFException {
        int leng = 4;
        int size = 3;
        boolean flag = false;
        List<MacdDto> oddList = new ArrayList<>();
        List<MacdDto> eveList = new ArrayList<>();
        List<Double> macdlist = new ArrayList<>();
        List<IBar> askBarList = new ArrayList<>();

        for (int i = 0; i < 500; i++) {
            IBar askBar = this.context.getHistory().getBar(instrument, defaultPeriod, offerSide, i);
            double[] macd0 = this.indicators.macd(instrument, this.defaultPeriod, offerSide, appliedPrice, fastMACDPeriod, slowMACDPeriod, signalMACDPeriod, i);
            double[] macd1 = this.indicators.macd(instrument, this.defaultPeriod, offerSide, appliedPrice, fastMACDPeriod, slowMACDPeriod, signalMACDPeriod, i + 1);
            macdlist.add(macd0[HIST] * capacity);
            askBarList.add(askBar);
            //1、3、5...
            if (macd0[HIST] > 0 && macd1[HIST] <= 0) {
                flag = true;
                if (oddList.size() < size) {
                    oddList.add(new MacdDto(i, macd0));
                }
            }
            //2、4、6...
            if (flag && macd0[HIST] <= 0 && macd1[HIST] > 0) {
                if (eveList.size() > size) {
                    eveList.add(new MacdDto(i, macd0));
                }
            }
            if (oddList.size() > size && eveList.size() > size) {
                break;
            }
        }
        this.console.getInfo().println(this.getCurrentTime(this.currBar.getTime()) + "macdlist:" + ToString.listToString(macdlist));
        this.console.getInfo().println(this.getCurrentTime(this.currBar.getTime()) + "askBarList:" + ToString.iBarlistToString(askBarList));
        if (oddList.size() > size && eveList.size() > size) {
            MacdDto oddMacd1 = oddList.get(0);
            MacdDto oddMacd2 = eveList.get(0);
            MacdDto eveMacd3 = oddList.get(1);
            MacdDto eveMacd4 = eveList.get(1);
            if(oddMacd2.getShift() - oddMacd1.getShift() > leng && eveMacd3.getShift() - oddMacd2.getShift() > leng && eveMacd4.getShift() - eveMacd3.getShift() > leng){
                Map<String, Double> macd_map0 = this.getMaxMinDouble(macdlist, oddMacd1.getShift(), oddMacd2.getShift());
                Map<String, Double> macd_map1 = this.getMaxMinDouble(macdlist, eveMacd3.getShift(), eveMacd4.getShift());
                Map<String, Double> bar_map0 = this.getMaxMinBar(askBarList, oddMacd1.getShift(), oddMacd2.getShift());
                Map<String, Double> bar_map1 = this.getMaxMinBar(askBarList, eveMacd3.getShift(), eveMacd4.getShift());
                if(macd_map0.get("min") > macd_map1.get("min") && bar_map0.get("min") < bar_map1.get("min")){
                    this.createSignalUp();
                }
            }
        }
        return false;
    }

    private Map<String, Double> getMaxMinBar(List<IBar> list, int bin, int end){
        this.initBinEnd(bin, end);
        list = list.subList(bin - 1, end);

        Ordering<IBar> lowOrdering = Ordering.natural().nullsFirst().onResultOf(new Function<IBar, Double>() {
            @Override
            public Double apply(IBar bar) {
                return bar.getLow();
            }
        });
        Ordering<IBar> highOrdering = Ordering.natural().reverse().nullsFirst().onResultOf(new Function<IBar, Double>() {
            @Override
            public Double apply(IBar bar) {
                return bar.getHigh();
            }
        });

        final List<IBar> lowlist = lowOrdering.sortedCopy(list);
        final List<IBar> highlist = highOrdering.sortedCopy(list);

        return new HashMap<String, Double>() {{
            put("max", highlist.get(0).getHigh());
            put("min", lowlist.get(0).getLow());
        }};
    }

    private List<MacdDto> sortMacdAsc(List<MacdDto> list) {
        Ordering<MacdDto> ordering = Ordering.from(new Comparator<MacdDto>() {
            @Override
            public int compare(MacdDto o1, MacdDto o2) {
                return Doubles.compare(o1.getMacd()[2], o2.getMacd()[2]);
            }
        });
        return ordering.sortedCopy(list);
    }

    private Map<String, MacdDto> getMaxMinMacd(List<MacdDto> list, int bin, int end) {
        this.initBinEnd(bin, end);
        list = list.subList(bin - 1, end);
        final List<MacdDto> _list = sortMacdAsc(list);
        return new HashMap<String, MacdDto>() {{
            put("max", _list.get(_list.size() - 1));
            put("min", _list.get(0));
        }};
    }

    private Map<String, Double> getMaxMinDouble(List<Double> list, int bin, int end) {
        this.initBinEnd(bin, end);
        list = list.subList(bin - 1, end);
        final List<Double> _list = Ordering.natural().sortedCopy(list);
        return new HashMap<String, Double>() {{
            put("max", _list.get(_list.size() - 1));
            put("min", _list.get(0));
        }};
    }

    private void initBinEnd(int bin, int end){
        if (bin < end) {
            int temp = bin;
            bin = end;
            end = temp;
        }
    }

    private void createSignalUp() throws JFException {
        try {
            String chartKey = this.getChartKey("signalUp");
            console.getInfo().println("chartKey: " + chartKey);
            IChartObject chartObject = this.chart.get(chartKey);
            console.getInfo().println("chartObject : " + chartObject);
            if (chartObject == null) {
                console.getInfo().println("this.currBar.getTime(): " + this.currBar.getTime());
                IChartObjectFactory chartObjectFactory = chart.getChartObjectFactory();
                ISignalUpChartObject signalArr = chartObjectFactory.createSignalUp(chartKey, this.currBar.getTime(), currBar.getLow() - 0.0001);
                signalArr.setStickToCandleTimeEnabled(false);
                signalArr.setColor(Color.YELLOW);
                this.chart.add(signalArr);
            }
        } catch (Exception e) {
            console.getOut().println("Exception : " + e.toString());
        }
    }
    private String getChartKey(String type) {
        return type + this.currBar.getTime();
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (instrument != this.instrument) {
            return;
        }
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onStop() throws JFException {
    }

    private IOrder submitOrder(OrderCommand orderCmd) throws JFException {

        double stopLossPrice, takeProfitPrice;

        // Calculating order price, stop loss and take profit prices
        if (orderCmd == OrderCommand.BUY) {
            stopLossPrice = history.getLastTick(this.instrument).getBid() - getPipPrice(this.stopLossPips);
            takeProfitPrice = history.getLastTick(this.instrument).getBid() + getPipPrice(this.takeProfitPips);
        } else {
            stopLossPrice = history.getLastTick(this.instrument).getAsk() + getPipPrice(this.stopLossPips);
            takeProfitPrice = history.getLastTick(this.instrument).getAsk() - getPipPrice(this.takeProfitPips);
        }

        return engine.submitOrder(getLabel(instrument), instrument, orderCmd, amount, 0, 20, stopLossPrice, takeProfitPrice);
    }

    private void closeOrder(IOrder order) throws JFException {
        if (order == null) {
            return;
        }
        if (order.getState() != IOrder.State.CLOSED && order.getState() != IOrder.State.CREATED && order.getState() != IOrder.State.CANCELED) {
            order.close();
            order = null;
        }
    }

    private double getPipPrice(int pips) {
        return pips * this.instrument.getPipValue();
    }

    private String getLabel(Instrument instrument) {
        String label = instrument.name();
        label = label + (counter++);
        label = label.toUpperCase();
        return label;
    }

    private void print(Object... o) {
        for (Object ob : o) {
            //console.getOut().print(ob + "  ");
            if (ob instanceof double[]) {
                print((double[]) ob);
            } else if (ob instanceof double[]) {
                print((double[][]) ob);
            } else if (ob instanceof Long) {
                print(dateToStr((Long) ob));
            } else {
                print(ob);
            }
            print(" ");
        }
        console.getOut().println();
    }

    private void print(Object o) {
        console.getOut().print(o);
    }

    private void println(Object o) {
        console.getOut().println(o);
    }

    private void print(double[] arr) {
        println(arrayToString(arr));
    }

    private void print(double[][] arr) {
        println(arrayToString(arr));
    }

    private void printIndicatorInfos(IIndicator ind) {
        for (int i = 0; i < ind.getIndicatorInfo().getNumberOfInputs(); i++) {
            println(ind.getIndicatorInfo().getName() + " Input " + ind.getInputParameterInfo(i).getName() + " " + ind.getInputParameterInfo(i).getType());
        }
        for (int i = 0; i < ind.getIndicatorInfo().getNumberOfOptionalInputs(); i++) {
            println(ind.getIndicatorInfo().getName() + " Opt Input " + ind.getOptInputParameterInfo(i).getName() + " " + ind.getOptInputParameterInfo(i).getType());
        }
        for (int i = 0; i < ind.getIndicatorInfo().getNumberOfOutputs(); i++) {
            println(ind.getIndicatorInfo().getName() + " Output " + ind.getOutputParameterInfo(i).getName() + " " + ind.getOutputParameterInfo(i).getType());
        }
        console.getOut().println();
    }

    public static String arrayToString(double[] arr) {
        String str = "";
        for (int r = 0; r < arr.length; r++) {
            str += "[" + r + "] " + (new DecimalFormat("#.#######")).format(arr[r]) + "; ";
        }
        return str;
    }

    public static String arrayToString(double[][] arr) {
        String str = "";
        if (arr == null) {
            return "null";
        }
        for (int r = 0; r < arr.length; r++) {
            for (int c = 0; c < arr[r].length; c++) {
                str += "[" + r + "][" + c + "] " + (new DecimalFormat("#.#######")).format(arr[r][c]);
            }
            str += "; ";
        }
        return str;
    }

    public String toDecimalToStr(double d) {
        return (new DecimalFormat("#.#######")).format(d);
    }

    public String dateToStr(Long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") {

            {
                setTimeZone(TimeZone.getTimeZone("GMT"));
            }
        };
        return sdf.format(time);
    }

    private String getCurrentTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(time);
    }
}


class ToString {

    /**
     * 定义分割常量 （#在集合中的含义是每个元素的分割，|主要用于map类型的集合用于key与value中的分割）
     */
    private static final String SEP1 = "; ";
    private static final String SEP2 = "|";

    /**
     * List转换String
     *
     * @param list :需要转换的List
     * @return String转换后的字符串
     */
    public static String listToString(List<?> list) {
        StringBuffer sb = new StringBuffer();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == null || list.get(i) == "") {
                    continue;
                }
                // 如果值是list类型则调用自己
                if (list.get(i) instanceof List) {
                    sb.append(listToString((List<?>) list.get(i)) + "");
                    sb.append(SEP1);
                } else if (list.get(i) instanceof Map) {
                    sb.append(mapToString((Map<?, ?>) list.get(i)) + "");
                    sb.append(SEP1);
                } else {
                    sb.append(list.get(i));
                    sb.append(SEP1);
                }
            }
        }
        return sb.toString();
    }

    public static String iBarlistToString(List<IBar> list) {
        StringBuffer sb = new StringBuffer();
        IBar bar;
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                bar = list.get(i);
                sb.append("[");
                sb.append(bar.getHigh());
                sb.append(",");
                sb.append(bar.getLow());
                sb.append("]; ");
            }
        }
        return sb.toString();
    }

    /**
     * Map转换String
     *
     * @param map :需要转换的Map
     * @return String转换后的字符串
     */
    public static String mapToString(Map<?, ?> map) {
        StringBuffer sb = new StringBuffer();
        // 遍历map
        for (Object obj : map.keySet()) {
            if (obj == null) {
                continue;
            }
            Object key = obj;
            Object value = map.get(key);
            if (value instanceof List<?>) {
                sb.append(key.toString() + SEP1 + listToString((List<?>) value));
                sb.append(SEP2);
            } else if (value instanceof Map<?, ?>) {
                sb.append(key.toString() + SEP1
                        + mapToString((Map<?, ?>) value));
                sb.append(SEP2);
            } else {
                sb.append(key.toString() + SEP1 + value.toString());
                sb.append(SEP2);
            }
        }
        return sb.toString();
    }

    private static void fun1() {
        List<Double> macdlist = Lists.newArrayList(1.2, 3.2, 4.3);
        System.out.println(listToString(macdlist));
    }
}