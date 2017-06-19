package singlejartest;


import com.dukascopy.api.*;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.ISignalDownChartObject;
import com.dukascopy.api.drawings.ISignalUpChartObject;

import java.awt.*;

public class SignalArrow implements IStrategy {

    private IHistory history;
    private IChart chart;
    private IConsole console;
    private IIndicators indicators;
    private IContext context;
    private IBar currBar;

    @Configurable("")
    public Instrument instrument = Instrument.EURUSD;
    @Configurable("")
    public Period period = Period.ONE_MIN;
    @Configurable("")
    public OfferSide side = OfferSide.BID;
    @Configurable("")
    public int smaPeriod1 = 10;
    @Configurable("")
    public int smaPeriod2 = 30;
    @Configurable("")
    public int fromShift = 50;
    @Configurable("")
    public int toShift = 1;

    class PointDbl {
        public final double x;
        public final double y;

        public PointDbl(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    @Override
    public void onStart(IContext context) throws JFException {
        this.context = context;
        this.history = context.getHistory();
        this.console = context.getConsole();
        this.indicators = context.getIndicators();

        this.chart = context.getChart(instrument);
        if (this.chart == null) {
            this.console.getErr().println("No chart opened, can't plot indicators.");
            this.context.stop();
        }
        if (this.chart.getSelectedPeriod() != period || chart.getSelectedOfferSide() != side) {
            this.console.getErr().println("Chart feed does not match the one of indicators. Can't plot indicators.");
            this.context.stop();
        }
        this.chart.add(indicators.getIndicator("SMA"), new Object[]{smaPeriod1});
        this.chart.add(indicators.getIndicator("SMA"), new Object[]{smaPeriod2});
        this.createSignalDown();
    }

    private void createSignalDown() throws JFException {
        try {
            String chartKey = this.getChartKey("signalDown");
            console.getInfo().println("chartKey: " + chartKey);
            IChartObject chartObject = this.chart.get(chartKey);
            console.getInfo().println("chartObject : " + chartObject);
            if (chartObject == null) {
                console.getInfo().println("this.currBar.getTime(): " + this.currBar.getTime());
                IChartObjectFactory chartObjectFactory = chart.getChartObjectFactory();
                ISignalDownChartObject signalArr = chartObjectFactory.createSignalDown(chartKey, this.currBar.getTime(), currBar.getHigh() + 0.0001);
                signalArr.setStickToCandleTimeEnabled(false);
                signalArr.setColor(Color.GREEN);
                this.chart.add(signalArr);
            }
        } catch (Exception e) {
            console.getOut().println("Exception : " + e.toString());
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

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        this.currBar = this.context.getHistory().getBar(Instrument.EURUSD, Period.ONE_MIN, OfferSide.BID, 0);
        this.createSignalDown();
        this.createSignalUp();
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
    }

    @Override
    public void onMessage(IMessage message) throws JFException {
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
    }

    @Override
    public void onStop() throws JFException {
    }

}
