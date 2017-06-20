package singlejartest;


import com.dukascopy.api.*;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IIndicators.AppliedPrice;
import com.dukascopy.api.indicators.IIndicator;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class MacdBeili implements IStrategy {

    public static final int HIST = 2;

    private IOrder order;
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;

    private int counter = 0;

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

        IChart chart = context.getChart(instrument);
        if (chart != null) {
            chart.add(indicators.getIndicator("MACD"), new Object[]{fastMACDPeriod, slowMACDPeriod, signalMACDPeriod});
        }
    }


    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {

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
            this.macd[0] = macd[0] * 100000;
            this.macd[1] = macd[1] * 100000;
            this.macd[2] = macd[2] * 100000;
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
        List<Double> macdlist = new ArrayList<>();
        List<MacdDto> oddList = new ArrayList<>();
        List<MacdDto> eveList = new ArrayList<>();
        List<IBar> askBarList = new ArrayList<>();

        for (int i = 0; i < 500; i++) {
            IBar askBar = this.context.getHistory().getBar(instrument, defaultPeriod, offerSide, i);
            double[] macd0 = this.indicators.macd(instrument, this.defaultPeriod, offerSide, appliedPrice, fastMACDPeriod, slowMACDPeriod, signalMACDPeriod, i);
            double[] macd1 = this.indicators.macd(instrument, this.defaultPeriod, offerSide, appliedPrice, fastMACDPeriod, slowMACDPeriod, signalMACDPeriod, i + 1);
            macdlist.add(i, macd0[HIST] * 100000);
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
                if (eveList.size() < size) {
                    eveList.add(new MacdDto(i, macd0));
                }
            }
            if (oddList.size() >= size && eveList.size() >= size) {
                break;
            }
        }

        if (oddList.size() > size && eveList.size() > size) {
            MacdDto macdDto1 = oddList.get(0);
            MacdDto macdDto2 = eveList.get(0);
            MacdDto macdDto3 = oddList.get(1);
            MacdDto macdDto4 = eveList.get(1);
            if(macdDto2.getShift() - macdDto1.getShift() > leng && macdDto3.getShift() - macdDto2.getShift() > leng && macdDto4.getShift() - macdDto3.getShift() > leng){
                Map<String, Double> map0 = this.getMaxMinDouble(macdlist, macdDto1.getShift(), macdDto2.getShift());
                Map<String, Double> map1 = this.getMaxMinDouble(macdlist, macdDto3.getShift(), macdDto4.getShift());
                if(map0.get("min") > map1.get("min")){

                }
            }
        }
        return false;
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
}