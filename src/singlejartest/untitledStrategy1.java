package singlejartest;

import com.dukascopy.api.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


/*
 * Created by VisualJForex Generator, version 2.40
 * Date: 18.06.2017 02:28
 */
public class untitledStrategy1 implements IStrategy {

	private CopyOnWriteArrayList<TradeEventAction> tradeEventActions = new CopyOnWriteArrayList<TradeEventAction>();
	private static final String DATE_FORMAT_NOW = "yyyyMMdd_HHmmss";
	private IEngine engine;
	private IConsole console;
	private IHistory history;
	private IContext context;
	private IIndicators indicators;
	private IUserInterface userInterface;

	@Configurable("defaultTakeProfit:")
	public int defaultTakeProfit = 50;
	@Configurable("defaultInstrument:")
	public Instrument defaultInstrument = Instrument.EURUSD;
	@Configurable("defaultSlippage:")
	public int defaultSlippage = 5;
	@Configurable("defaultTradeAmount:")
	public double defaultTradeAmount = 0.001;
	@Configurable("defaultStopLoss:")
	public int defaultStopLoss = 25;
	@Configurable("defaultPeriod:")
	public Period defaultPeriod = Period.TEN_MINS;

	private Candle LastBidCandle =  null ;
	private String AccountId = "";
	private double _line10;
	private double Equity;
	private Tick LastTick =  null ;
	private IOrder _Buy =  null ;
	private String AccountCurrency = "";
	private int OverWeekendEndLeverage;
	private double Leverage;
	private List<IOrder> PendingPositions =  null ;
	private List<IOrder> OpenPositions =  null ;
	private double UseofLeverage;
	private IMessage LastTradeEvent =  null ;
	private IOrder _Sell =  null ;
	private boolean GlobalAccount;
	private Candle LastAskCandle =  null ;
	private int MarginCutLevel;
	private List<IOrder> AllPositions =  null ;
	private IOrder _Position =  null ;
	private IOrder _Position1 =  null ;


	public void onStart(IContext context) throws JFException {
		this.engine = context.getEngine();
		this.console = context.getConsole();
		this.history = context.getHistory();
		this.context = context;
		this.indicators = context.getIndicators();
		this.userInterface = context.getUserInterface();

		subscriptionInstrumentCheck(defaultInstrument);

		ITick lastITick = context.getHistory().getLastTick(defaultInstrument);
		LastTick = new Tick(lastITick, defaultInstrument);

		IBar bidBar = context.getHistory().getBar(defaultInstrument, defaultPeriod, OfferSide.BID, 1);
		IBar askBar = context.getHistory().getBar(defaultInstrument, defaultPeriod, OfferSide.ASK, 1);
		LastAskCandle = new Candle(askBar, defaultPeriod, defaultInstrument, OfferSide.ASK);
		LastBidCandle = new Candle(bidBar, defaultPeriod, defaultInstrument, OfferSide.BID);

		if (indicators.getIndicator("SMA") == null) {
			indicators.registerDownloadableIndicator("1314","SMA");
		}
		subscriptionInstrumentCheck(Instrument.fromString("EUR/USD"));

	}

	public void onAccount(IAccount account) throws JFException {
		AccountCurrency = account.getCurrency().toString();
		Leverage = account.getLeverage();
		AccountId= account.getAccountId();
		Equity = account.getEquity();
		UseofLeverage = account.getUseOfLeverage();
		OverWeekendEndLeverage = account.getOverWeekEndLeverage();
		MarginCutLevel = account.getMarginCutLevel();
		GlobalAccount = account.isGlobal();
	}

	private void updateVariables(Instrument instrument) {
		try {
			AllPositions = engine.getOrders();
			List<IOrder> listMarket = new ArrayList<IOrder>();
			for (IOrder order: AllPositions) {
				if (order.getState().equals(IOrder.State.FILLED)){
					listMarket.add(order);
				}
			}
			List<IOrder> listPending = new ArrayList<IOrder>();
			for (IOrder order: AllPositions) {
				if (order.getState().equals(IOrder.State.OPENED)){
					listPending.add(order);
				}
			}
			OpenPositions = listMarket;
			PendingPositions = listPending;
		} catch(JFException e) {
			e.printStackTrace();
		}
	}

	public void onMessage(IMessage message) throws JFException {
		if (message.getOrder() != null) {
			updateVariables(message.getOrder().getInstrument());
			LastTradeEvent = message;
			for (TradeEventAction event :  tradeEventActions) {
				IOrder order = message.getOrder();
				if (order != null && event != null && message.getType().equals(event.getMessageType())&& order.getLabel().equals(event.getPositionLabel())) {
					Method method;
					try {
						method = this.getClass().getDeclaredMethod(event.getNextBlockId(), Integer.class);
						method.invoke(this, new Integer[] {event.getFlowId()});
					} catch (SecurityException e) {
							e.printStackTrace();
					} catch (NoSuchMethodException e) {
						  e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} 
					tradeEventActions.remove(event); 
				}
			}   
		}
	}

	public void onStop() throws JFException {
	}

	public void onTick(Instrument instrument, ITick tick) throws JFException {
		LastTick = new Tick(tick, instrument);
		updateVariables(instrument);


	}

	public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
		LastAskCandle = new Candle(askBar, period, instrument, OfferSide.ASK);
		LastBidCandle = new Candle(bidBar, period, instrument, OfferSide.BID);
		updateVariables(instrument);
			SMA_block_10(1);

	}

    public void subscriptionInstrumentCheck(Instrument instrument) {
		try {
		      if (!context.getSubscribedInstruments().contains(instrument)) {
		          Set<Instrument> instruments = new HashSet<Instrument>();
		          instruments.add(instrument);
		          context.setSubscribedInstruments(instruments, true);
		          Thread.sleep(100);
		      }
		  } catch (InterruptedException e) {
		      e.printStackTrace();
		  }
		}

    public double round(double price, Instrument instrument) {
		BigDecimal big = new BigDecimal("" + price); 
		big = big.setScale(instrument.getPipScale() + 1, BigDecimal.ROUND_HALF_UP); 
		return big.doubleValue(); 
	}

    public ITick getLastTick(Instrument instrument) {
		try { 
			return (context.getHistory().getTick(instrument, 0)); 
		} catch (JFException e) { 
			 e.printStackTrace();  
		 } 
		 return null; 
	}

	private void SMA_block_10(Integer flow) {
		Instrument argument_1 = defaultInstrument;
		Period argument_2 = Period.TEN_MINS;
		int argument_3 = 0;
		int argument_4 = 30;
		OfferSide[] offerside = new OfferSide[1];
		IIndicators.AppliedPrice[] appliedPrice = new IIndicators.AppliedPrice[1];
		offerside[0] = OfferSide.BID;
		appliedPrice[0] = IIndicators.AppliedPrice.CLOSE;
		Object[] params = new Object[1];
		params[0] = 30;
		try {
			subscriptionInstrumentCheck(argument_1);
			long time = context.getHistory().getBar(argument_1, argument_2, OfferSide.BID, argument_3).getTime();
			Object[] indicatorResult = context.getIndicators().calculateIndicator(argument_1, argument_2, offerside,
					"SMA", appliedPrice, params, Filter.WEEKENDS, 1, time, 0);
			if ((new Double(((double [])indicatorResult[0])[0])) == null) {
				this._line10 = Double.NaN;
			} else { 
				this._line10 = (((double [])indicatorResult[0])[0]);
			} 
		If_block_11(flow);
		} catch (JFException e) {
			e.printStackTrace();
			console.getErr().println(e);
			this._line10 = Double.NaN;
		}
	}

	private  void If_block_11(Integer flow) {
		int argument_1 = OpenPositions.size();
		int argument_2 = 1;
		if (argument_1< argument_2) {
			If_block_13(flow);
		}
		else if (argument_1> argument_2) {
			If_block_12(flow);
		}
		else if (argument_1== argument_2) {
			If_block_12(flow);
		}
	}

	private  void If_block_12(Integer flow) {
		double argument_1 = LastAskCandle.getClose();
		double argument_2 = _line10;
		if (argument_1< argument_2) {
			PositionsViewer_block_16(flow);
		}
		else if (argument_1> argument_2) {
			PositionsViewer_block_17(flow);
		}
		else if (argument_1== argument_2) {
		}
	}

	private  void If_block_13(Integer flow) {
		double argument_1 = LastAskCandle.getClose();
		double argument_2 = _line10;
		if (argument_1< argument_2) {
			OpenatMarket_block_14(flow);
		}
		else if (argument_1> argument_2) {
			OpenatMarket_block_15(flow);
		}
		else if (argument_1== argument_2) {
		}
	}

	private  void OpenatMarket_block_14(Integer flow) {
		Instrument argument_1 = defaultInstrument;
		double argument_2 = defaultTradeAmount;
		int argument_3 = defaultSlippage;
		int argument_4 = defaultStopLoss;
		int argument_5 = defaultTakeProfit;
		String argument_6 = "";
		ITick tick = getLastTick(argument_1);

		IEngine.OrderCommand command = IEngine.OrderCommand.SELL;

		double stopLoss = tick.getAsk() + argument_1.getPipValue() * argument_4;
		double takeProfit = round(tick.getAsk() - argument_1.getPipValue() * argument_5, argument_1);
		
           try {
               String label = getLabel();           
               _Sell = context.getEngine().submitOrder(label, argument_1, command, argument_2, 0, argument_3,  stopLoss, takeProfit, 0, argument_6);
		        } catch (JFException e) {
            e.printStackTrace();
        }
	}

	private  void OpenatMarket_block_15(Integer flow) {
		Instrument argument_1 = defaultInstrument;
		double argument_2 = defaultTradeAmount;
		int argument_3 = defaultSlippage;
		int argument_4 = defaultStopLoss;
		int argument_5 = defaultTakeProfit;
		String argument_6 = "";
		ITick tick = getLastTick(argument_1);

		IEngine.OrderCommand command = IEngine.OrderCommand.BUY;

		double stopLoss = tick.getBid() - argument_1.getPipValue() * argument_4;
		double takeProfit = round(tick.getBid() + argument_1.getPipValue() * argument_5, argument_1);
		
           try {
               String label = getLabel();           
               _Buy = context.getEngine().submitOrder(label, argument_1, command, argument_2, 0, argument_3,  stopLoss, takeProfit, 0, argument_6);
		        } catch (JFException e) {
            e.printStackTrace();
        }
	}

	private  void PositionsViewer_block_16(Integer flow) {
		List<IOrder> argument_1 = OpenPositions;
		for (IOrder order : argument_1){
			if (order.getState() == IOrder.State.OPENED||order.getState() == IOrder.State.FILLED){
				_Position = order;
				If_block_18(flow);
			}
		}
	}

	private  void PositionsViewer_block_17(Integer flow) {
		List<IOrder> argument_1 = OpenPositions;
		for (IOrder order : argument_1){
			if (order.getState() == IOrder.State.OPENED||order.getState() == IOrder.State.FILLED){
				_Position = order;
				If_block_19(flow);
			}
		}
	}

	private  void If_block_18(Integer flow) {
		boolean argument_1 = _Position.isLong();
		boolean argument_2 = true;
		if (argument_1!= argument_2) {
		}
		else if (argument_1 == argument_2) {
			CloseandCancelPosition_block_20(flow);
		}
	}

	private  void If_block_19(Integer flow) {
		boolean argument_1 = _Position.isLong();
		boolean argument_2 = false;
		if (argument_1!= argument_2) {
		}
		else if (argument_1 == argument_2) {
			CloseandCancelPosition_block_21(flow);
		}
	}

	private  void CloseandCancelPosition_block_20(Integer flow) {
		try {
			if (_Position != null && (_Position.getState() == IOrder.State.OPENED||_Position.getState() == IOrder.State.FILLED)){
				_Position.close();
			}
		} catch (JFException e)  {
			e.printStackTrace();
		}
	}

	private  void CloseandCancelPosition_block_21(Integer flow) {
		try {
			if (_Position != null && (_Position.getState() == IOrder.State.OPENED||_Position.getState() == IOrder.State.FILLED)){
				_Position.close();
			}
		} catch (JFException e)  {
			e.printStackTrace();
		}
	}

class Candle  {

    IBar bar;
    Period period;
    Instrument instrument;
    OfferSide offerSide;

    public Candle(IBar bar, Period period, Instrument instrument, OfferSide offerSide) {
        this.bar = bar;
        this.period = period;
        this.instrument = instrument;
        this.offerSide = offerSide;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public OfferSide getOfferSide() {
        return offerSide;
    }

    public void setOfferSide(OfferSide offerSide) {
        this.offerSide = offerSide;
    }

    public IBar getBar() {
        return bar;
    }

    public void setBar(IBar bar) {
        this.bar = bar;
    }

    public long getTime() {
        return bar.getTime();
    }

    public double getOpen() {
        return bar.getOpen();
    }

    public double getClose() {
        return bar.getClose();
    }

    public double getLow() {
        return bar.getLow();
    }

    public double getHigh() {
        return bar.getHigh();
    }

    public double getVolume() {
        return bar.getVolume();
    }
}
class Tick {

    private ITick tick;
    private Instrument instrument;

    public Tick(ITick tick, Instrument instrument){
        this.instrument = instrument;
        this.tick = tick;
    }

    public Instrument getInstrument(){
       return  instrument;
    }

    public double getAsk(){
       return  tick.getAsk();
    }

    public double getBid(){
       return  tick.getBid();
    }

    public double getAskVolume(){
       return  tick.getAskVolume();
    }

    public double getBidVolume(){
        return tick.getBidVolume();
    }

   public long getTime(){
       return  tick.getTime();
    }

   public ITick getTick(){
       return  tick;
    }
}

    protected String getLabel() {
        String label;
        label = "IVF" + getCurrentTime(LastTick.getTime()) + generateRandom(10000) + generateRandom(10000);
        return label;
    }

    private String getCurrentTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(time);
    }

    private static String generateRandom(int n) {
        int randomNumber = (int) (Math.random() * n);
        String answer = "" + randomNumber;
        if (answer.length() > 3) {
            answer = answer.substring(0, 4);
        }
        return answer;
    }

    class TradeEventAction {
		private IMessage.Type messageType;
		private String nextBlockId = "";
		private String positionLabel = "";
		private int flowId = 0;

        public IMessage.Type getMessageType() {
            return messageType;
        }

        public void setMessageType(IMessage.Type messageType) {
            this.messageType = messageType;
        }

        public String getNextBlockId() {
            return nextBlockId;
        }

        public void setNextBlockId(String nextBlockId) {
            this.nextBlockId = nextBlockId;
        }
        public String getPositionLabel() {
            return positionLabel;
       }

        public void setPositionLabel(String positionLabel) {
            this.positionLabel = positionLabel;
        }
        public int getFlowId() {
            return flowId;
        }
        public void setFlowId(int flowId) {
            this.flowId = flowId;
        }
    }
}