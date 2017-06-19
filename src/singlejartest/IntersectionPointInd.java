package singlejartest;


import com.dukascopy.api.*;
import com.dukascopy.api.IIndicators.AppliedPrice;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.ISignalDownChartObject;

import java.util.List;

/**
 * The strategy finds indicator two indicator output line intersection points
 * by using a geometric method. It also plots a signal chart object on each of those intersections.
 *
 */
public class IntersectionPointInd implements IStrategy {
    
    private IHistory history;
    private IChart chart;
    private IConsole console;
    private IIndicators indicators;
    
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
        public PointDbl(double x, double y){
            this.x = x;
            this.y = y;
        }
    }
    
    @Override
    public void onStart(IContext context) throws JFException {
        history = context.getHistory();
        console = context.getConsole();
        indicators = context.getIndicators();
        
        chart = context.getChart(instrument);
        if(chart == null){
            console.getErr().println("No chart opened, can't plot indicators.");
            context.stop();
        }
        if(chart.getSelectedPeriod() != period || chart.getSelectedOfferSide() != side){
            console.getErr().println("Chart feed does not match the one of indicators. Can't plot indicators.");
            context.stop();
        }
        chart.add(indicators.getIndicator("SMA"), new Object [] {smaPeriod1});
        chart.add(indicators.getIndicator("SMA"), new Object [] {smaPeriod2});
        this.createSignalDown();
    }

    private void createSignalDown() throws JFException{
        IBar bar = history.getBar(instrument, period, side, toShift);
        double[] sma1 = indicators.sma(instrument, period, side, AppliedPrice.CLOSE, smaPeriod1, chart.getFilter(), fromShift, bar.getTime(), 0);
        double[] sma2 = indicators.sma(instrument, period, side, AppliedPrice.CLOSE, smaPeriod2, chart.getFilter(), fromShift, bar.getTime(), 0);
        IBar[] bars = history.getBars(instrument, period, side, chart.getFilter(),fromShift, bar.getTime(), 0).toArray(new IBar[] {});
        String chartKey = "";
        for(int i=0; i< sma1.length - 1; i++){
            PointDbl point = intersection(
                    bars[i].getTime(), sma1[i], bars[i+1].getTime(), sma1[i+1],
                    bars[i].getTime(), sma2[i], bars[i+1].getTime(), sma2[i+1]
            );
            long intersectTime = (long)((point != null)?point.x:0);
            if(point == null || intersectTime < bars[i].getTime() || intersectTime > bars[i+1].getTime()){
               // console.getOut().println("Lines don't intersect between " + DateUtils.format(bars[i].getTime()) + " and " + DateUtils.format(bars[i+1].getTime()));
            } else {
                //console.getOut().println(String.format("Lines intersect at %s:%.5f ",DateUtils.format(point.x), point.y));
                IChartObjectFactory chartObjectFactory = chart.getChartObjectFactory();

                chartKey = "intersection_point" + bar.getTime();
                console.getInfo().println("(long)point.x + period.getInterval()/2 : " + (long)point.x + period.getInterval()/2);
                console.getInfo().println("point.y : " + point.y);
                ISignalDownChartObject signalArr = chartObjectFactory.createSignalDown(chartKey, (long)point.x + period.getInterval()/2, point.y);
                signalArr.setStickToCandleTimeEnabled(false);
                chart.add(signalArr);
            }
        }

        try {
            IChartObject chartObject = chart.get(chartKey);
            console.getOut().println("chartObject.toString() :" + chartObject.toString());
            console.getOut().println("chartObject.getKey() :" + chartObject.getKey());
            console.getOut().println("chartObject.getInstrument() :" + chartObject.getInstrument());
            List<IChartObject> list = chart.getAll();
            if(list != null && list.size() > 0){
                for(IChartObject object :list){
                    console.getOut().println("object.getType().name() :" + object.getType().name());
                    console.getOut().println("object.getKey() :" + object.getKey());
                    console.getOut().println("object.clone().getKey() :" + object.clone().getKey());
                }
            }
        } catch (Exception e) {
            console.getOut().println("错误 : " + e.getMessage());
        }
    }


    public PointDbl intersection(
            double x1, double y1, double x2, double y2, 
            double x3, double y3, double x4, double y4
        ) {
        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (d == 0)
            return null;

        double xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
        double yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;

        return new PointDbl(xi, yi);
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        createSignalDown();
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {}

    @Override
    public void onMessage(IMessage message) throws JFException {}

    @Override
    public void onAccount(IAccount account) throws JFException {}

    @Override
    public void onStop() throws JFException {}

}
