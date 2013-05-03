package uwsimresgen.model;

import java.io.File;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uwsimresgen.OutputLog;
import uwsimresgen.view.IView;
import DB.Database;

public class ResultsModel {

	public static String DEFAULT_TABLE_PREFIX = "SimResults";
	public static String RESULTS_TABLE_NAME = "SpinResults";
	public static String BASE_PAYTABLE_TABLE_NAME = "BasePaytable";
	public static String BONUS_PAYTABLE_TABLE_NAME = "BonusPaytable";
	public static String REELMAPPINGS_TABLE_NAME = "ReelMappings";
	public static String SYMBOLS_TABLE_NAME = "Symbols";
	public static String BLOCKS_TABLE_NAME = "Blocks";
	public static String PAYLINES_TABLE_NAME = "Paylines";
	public static String LOSS_PER_TABLE_NAME = "LossPercentage";
	public static String GAMBLERS_RUIN_TABLE_NAME = "GamblersRuin";
	public static String PRIZE_SIZES_TABLE_NAME = "PrizeSizes";
	public static String FORCED_FREE_SPINS_TABLE_NAME = "ForcedFreeSpins";

	public static enum SymbolType {
		BASIC, SCATTER, BONUS, WBBONUS, UNKNOWN
	}

	public static enum WinType {
		BASIC, SCATTER, BONUS, BSCATTER, WBBONUS, UNKNOWN
	}
	
	public final int WBBonusPayTable[] = {2, 
										5, 5, 5, 5, 
										8, 8, 8, 8, 8, 8, 8, 8, 
										10, 10, 10, 10, 10, 10, 10, 
										15, 15, 15,
										25, 25};

	public static final int REEL_TOP = 0;
	public static final int REEL_MID = 1;
	public static final int REEL_BOT = 2;
	public static final int REEL_UNK = 999;

	public static final int REEL_ONE = 0;
	public static final int REEL_TWO = 1;
	public static final int REEL_THREE = 2;
	public static final int REEL_FOUR = 3;
	public static final int REEL_FIVE = 4;

	public static final int PAYTABLE_BADPAYOUT = -99999;
	public static final int MAX_BLOCKREPEATS = 10000;
	public final int MAX_FREESPINS = 200;
	
	@SuppressWarnings("serial")
	private final HashMap<Integer, Integer> freespin_lookuptable = new HashMap<Integer, Integer>() {
		{
			put(25, 3);
			put(100, 10);
			put(250, 15);
		};
	};

	private ArrayList<Result> results = new ArrayList<Result>();
	private ArrayList<Symbol> symbols = new ArrayList<Symbol>();
	private ArrayList<Block> blocks = new ArrayList<Block>();
	private ArrayList<GRBlock> grblocks = new ArrayList<GRBlock>();
	private List<Block> ffsblocks = new ArrayList<Block>();
	private ArrayList<Payline> paylines = new ArrayList<Payline>();
	private ArrayList<PaytableEntry> basepaytable = new ArrayList<PaytableEntry>();
	private ArrayList<PaytableEntry> bonuspaytable = new ArrayList<PaytableEntry>();
	private ArrayList<Integer> basescatterpaytable = new ArrayList<Integer>(); 
	private ArrayList<Integer> bonusspincounts = new ArrayList<Integer>();
	private HashMap<String, ArrayList<Integer>> bonuscatterpaytable = new HashMap<String, ArrayList<Integer>>();
	private HashMap<SimpleEntry<String, Integer>, Integer> basehittable = new HashMap<SimpleEntry<String, Integer>, Integer>();
	private HashMap<SimpleEntry<String, Integer>, Integer> bonushittable = new HashMap<SimpleEntry<String, Integer>, Integer>();
	
	private ArrayList<String> reel1 = new ArrayList<String>();
	private ArrayList<String> reel2 = new ArrayList<String>();
	private ArrayList<String> reel3 = new ArrayList<String>();
	private ArrayList<String> reel4 = new ArrayList<String>();
	private ArrayList<String> reel5 = new ArrayList<String>();

	private ArrayList<IView> views = new ArrayList<IView>();
	private ArrayList<String> errorLog = new ArrayList<String>();
	private ArrayList<String> warningLog = new ArrayList<String>();
	private ArrayList<String> errorLog2 = new ArrayList<String>();
	private ArrayList<String> warningLog2 = new ArrayList<String>();

	private boolean cancelled = false;
	private boolean running = false;
	private boolean paused = false;
	private boolean error = false;
	
	private boolean ldwResetRequired = true;
	private boolean repeatComplete = false;
	private boolean blockComplete = false;
	
	private boolean genAllBonusSpins = false;
	private short genallnumlines = 1;
	private boolean createSpinTable = true;
	private boolean genGamblersRuin = false;
	private boolean genPrizeSize = false;
	private boolean genForcedFreeSpins = false;
	
	private Block currblock = null;
	private GRBlock currgrblock = null;
	private int currblockindex = 0;
	private int currblockrepeat = 1;
	private LossPercentageEntry currlpe = null;
	private GamblersRuinEntry currgre = null;
	private PrizeSizeEntry currpze = null;
	private ForcedFreeSpinEntry currffs = null;
	
	private boolean bonusactive = false;

	private int wins = 0;
	private int losses = 0;
	private int currspin = 0;
	private int currconsumedspin = 0;
	private int failedspins = 0;
	private int freespins = 0;
	private int totalspins = 0;
	
	private File configfile = null;
	private File blocksfile = null;
	
	private String tableprefix = DEFAULT_TABLE_PREFIX;
	private String tablesuffix = "";
	private Boolean suffixAvailable = false;

	private BlockingQueue<Result> resultqueue = new LinkedBlockingQueue<Result>();
	private Random random;
	
	private Simulator simulator;
	private OutputLog outputLog;
	
	private String scatter_symbol;
	private String wbbonus_sequence;

	public ResultsModel() {
		this.simulator = new Simulator(this);
		this.outputLog = new OutputLog("log");
		this.random = new Random();
		
		for (int i = 0; i < 9; i++) 
			this.bonusspincounts.add(0);
	}

	public void AddView(IView view) {
		if (view != null) {
			this.views.add(view);
			view.updateView();
		}
	}

	public void RemoveView(IView view) {
		this.views.remove(view);
	}

	public void UpdateViews() {
		for (IView v : this.views) {
			v.updateView();
		}
	}

	public void setConfigFile(File value) {
		this.configfile = value;
		this.UpdateViews();
	}

	public void setBlocksFile(File value) {
		this.blocksfile = value;
		this.UpdateViews();
	}

	public void setTablePrefix(String value) {
		this.tableprefix = value;
		this.UpdateViews();
	}

	public void setTablePrefix() {
		this.tableprefix = DEFAULT_TABLE_PREFIX;
		this.UpdateViews();
	}

	public void setTableSuffix(String value) {
		this.tablesuffix = value;
		this.suffixAvailable = true;
		this.UpdateViews();
	}

	public void setTableSuffix() {
		java.util.Date date = new java.util.Date();
		SimpleDateFormat sft = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss_SSSS");
		String formattedDate = sft.format(date);
		this.tablesuffix = formattedDate;
		this.suffixAvailable = true;
		this.UpdateViews();
	}

	public void setDBName(String value) {
		Database.setDbName(value);
		this.UpdateViews();
	}

	public void setDBName() {
		Database.setDbName(Database.DEFAULT_DB_NAME);
		this.UpdateViews();
	}

	public void setTotalSpins(int value) {
		this.totalspins = value;
		this.UpdateViews();
	}

	public void setGenAllStops(boolean value) {
		this.simulator.setSeqStops(value);
		this.UpdateViews();
	}
	
	public void setCreateSpinTable(boolean value) {
		this.createSpinTable = value;
		this.UpdateViews();
	}
	
	public void setGenAllBonusSpin(boolean value) {
		this.genAllBonusSpins = value;
		this.UpdateViews();
	}

	public void setGenAllNumLines(short value) {
		this.genallnumlines = value;
		this.UpdateViews();
	}

	public void setGenGamblersRuin(boolean value) {
		this.genGamblersRuin = value;
		this.UpdateViews();
	}
	
	public void setGenPrizeSize(boolean value) {
		this.genPrizeSize = value;
		this.UpdateViews();
	}
	
	public void setGenForcedFreeSpins(boolean value) {
		this.genForcedFreeSpins = value;
		this.UpdateViews();
	}
	
	public boolean getGenAllStops() {
		return this.simulator.getSeqStops();
	}

	public short getGenAllNumLines() {
		return this.genallnumlines;
	}

	public boolean getGenGamblersRuin() {
		return this.genGamblersRuin;
	}
	
	public boolean getGenPrizeSize() {
		return this.genPrizeSize;
	}
	
	public boolean getGenAllBonusSpin() {
		return this.genAllBonusSpins;
	}
	
	public boolean getGenForcedFreeSpins() {
		return this.genForcedFreeSpins;
	}
	
	public Block getCurrBlock() {
		return this.currblock;
	}
	
	public GRBlock getCurrGRBlock() {
		return this.currgrblock;
	}
	
	public boolean getCreateSpinTable() {
		return this.createSpinTable;
	}
	
	public int getCurrSpin() {
		return this.currspin;
	}
	
	public int getFreeSpins() {
		return this.freespins;
	}
	
	public double getCurrBalance() {
		return this.currgrblock.getCurrBalance();
	}

	public int getWins() {
		return this.wins;
	}
	
	public int getLosses() {
		return this.losses;
	}
	
	public int getCurrConsumedSpin() {
		return this.currconsumedspin;
	}

	public int getTotalSpins() {
		return this.totalspins;
	}

	public int getBonusPayout(String key, int index) {
		return this.bonuscatterpaytable.get(key).get(index);
	}
	
	public int getBonusSpinCounts(int index) {
		return bonusspincounts.get(index);
	}

	public void incrementBonusSpinCounts(int index) {
		this.bonusspincounts.set(index, bonusspincounts.get(index) + 1);
	}
	
	public void updateBonusSpinCounts(short freespins) {
		switch (freespins) {
		
		case 3:
			incrementBonusSpinCounts(0);
			break;
			
		case 6:
			incrementBonusSpinCounts(1);
			break;
			
		case 10:
			incrementBonusSpinCounts(2);
			break;
			
		case 13:
			incrementBonusSpinCounts(3);
			break;
			
		case 15:
			incrementBonusSpinCounts(4);
			break;
			
		case 18:
			incrementBonusSpinCounts(5);
			break;
		
		case 20:
			incrementBonusSpinCounts(6);
			break;
			
		case 25:
			incrementBonusSpinCounts(7);
			break;
			
		default:
			incrementBonusSpinCounts(8);
			break;
				
		}
	}

	public File getConfigFile() {
		return this.configfile;
	}

	public File getBlocksFile() {
		return this.blocksfile;
	}

	public String getTablePrefix() {
		return this.tableprefix;
	}

	public String getTableSuffix() {
		if (this.suffixAvailable)
			return this.tablesuffix;
		else
			return "<suffix unavailable>";
	}

	public String getDBName() {
		return Database.getDbName();
	}

	private String buildDBTableName(String tablename) {
		return getTablePrefix() + "_" + tablename + "_" + getTableSuffix();
	}

	public String getBlockDBTableName() {
		return this.buildDBTableName(ResultsModel.BLOCKS_TABLE_NAME);
	}

	public String getSpinResultsDBTableName() {
		return this.buildDBTableName(ResultsModel.RESULTS_TABLE_NAME);
	}

	public String getSymbolsDBTableName() {
		return this.buildDBTableName(ResultsModel.SYMBOLS_TABLE_NAME);
	}

	public String getPaylinesDBTableName() {
		return this.buildDBTableName(ResultsModel.PAYLINES_TABLE_NAME);
	}

	public String getBasePaytableDBTableName() {
		return this.buildDBTableName(ResultsModel.BASE_PAYTABLE_TABLE_NAME);
	}

	public String getBonusPaytableDBTableName() {
		return this.buildDBTableName(ResultsModel.BONUS_PAYTABLE_TABLE_NAME);
	}

	public String getReelMappingsDBTableName() {
		return this.buildDBTableName(ResultsModel.REELMAPPINGS_TABLE_NAME);
	}
	
	public String getLossPercentageDBTableName() {
		return this.buildDBTableName(ResultsModel.LOSS_PER_TABLE_NAME);
	}

	public String getGamblersRuinDBTableName() {
		return this.buildDBTableName(ResultsModel.GAMBLERS_RUIN_TABLE_NAME);
	}
	
	public String getPrizeSizesDBTableName() {
		return this.buildDBTableName(ResultsModel.PRIZE_SIZES_TABLE_NAME);
	}
	
	public String getForcedFreeSpinsDBTableName() {
		return this.buildDBTableName(ResultsModel.FORCED_FREE_SPINS_TABLE_NAME);
	}
	
	public String getOutputLogFilePath() {
		return this.outputLog.getFilePath();
	}
	
	public int numOfSymbol(String str) {
		int num = 0;
		
		for (char c : str.toCharArray()) {
			if (c != '#') num += 1;
		}
		
		return num;
	}
	
	public void updateLDWWins(boolean isWin, Result pre_r) {
		// If it's the first spin
		if (pre_r == null)
			this.wins = (isWin) ? 1 : -1;
		else {
			// If the spin is an LDW as a win
			if (isWin) {
				if (this.wins < 0) {
					pre_r.setLDWWins(this.wins);
					this.wins = 1;
				} else {
					pre_r.setLDWWins(0);
					this.wins += 1;
				}
			} else {
				if (this.wins > 0) {
					pre_r.setLDWWins(this.wins);
					this.wins = -1;
				} else {
					pre_r.setLDWWins(0);
					this.wins -= 1;
				}
			}
		}
	}
	
	public void updateLDWLosses(boolean isWin, Result pre_r) {
		// If it's the first spin
		if (pre_r == null)
			this.losses = (isWin) ? 1 : -1;
		else {
			// If the spin is an LDW as a lose
			if (isWin) {
				if (this.losses < 0) {
					pre_r.setLDWLosses(this.losses);
					this.losses = 1;
				} else {
					pre_r.setLDWLosses(0);
					this.losses += 1;
				}
			} else {
				if (this.losses > 0) {
					pre_r.setLDWLosses(this.losses);
					this.losses = -1;
				} else {
					pre_r.setLDWLosses(0);
					this.losses -= 1;
				}
			}
		}
	}
	
	public void incrementBaseHit(String sequence, int payout) {
		SimpleEntry<String, Integer> se = new SimpleEntry<String, Integer>(sequence, payout);
		Integer value = new Integer(this.basehittable.get(se) + 1);
		
		this.basehittable.put(se, value);
	}
	
	public void incrementBonusHit(String sequence, int payout) {
		SimpleEntry<String, Integer> se = new SimpleEntry<String, Integer>(sequence, payout);
		Integer value = new Integer(this.bonushittable.get(se) + 1);
		
		this.bonushittable.put(se, value);
	}

	public boolean isCancelled() {
		return this.cancelled;
	}

	public boolean isPaused() {
		return this.paused;
	}

	public boolean isRunning() {
		return this.running;
	}

	public boolean isError() {
		return this.error;
	}

	public void launch() {

		this.outputLog.outputStringAndNewLine("LAUNCH NEW SESSION:");

		this.generateDBTableName();

		boolean result = loadConfiguration(configfile);

		if (result) {

			boolean result2;

			if (this.simulator.getSeqStops()) {
				result2 = true;
				this.outputLog
						.outputStringAndNewLine("Mode: Do All Reel Stop Combinations.");
				Block b = new Block();
				//Set Denom. for All-Reel-Stop mode here
				b.setDenomination((short) 1);
				b.setNumLines(this.genallnumlines);
				b.setLineBet((short) 1);
				b.setNumSpins(this.reel1.size() * this.reel2.size()
						* this.reel3.size() * this.reel4.size()
						* this.reel5.size());
				this.blocks.add(b);
			} else {
				result2 = loadBlocks(blocksfile);
				
				if (this.genGamblersRuin)
					this.outputLog.outputStringAndNewLine("Mode: Do Gamblers Ruin.");
				else if (this.genForcedFreeSpins)
					this.outputLog.outputStringAndNewLine("Mode: Do Forced Free Spins.");
				else
					this.outputLog.outputStringAndNewLine("Mode: Do Blocks.");
			}

			if (result2) {
				start();
				produce();
			} else {
				this.setError();
			}
		} else {
			this.setError();
		}
	}

	public void setError() {
		this.error = true;
		this.UpdateViews();
	}

	public void cancel() {
		this.cancelled = true;
		this.running = false;
		this.UpdateViews();
	}

	public void pause() {
		this.paused = true;
		this.UpdateViews();
	}

	public void resume() {
		this.paused = false;
		this.UpdateViews();
	}

	public void stop() {
		this.running = false;
		this.paused = false;
		this.UpdateViews();
	}

	public void start() {
		this.running = true;
		this.paused = false;
		this.UpdateViews();
	}

	public boolean isSetupValid() {
		if (this.configfile != null
				&& (this.blocksfile != null || this.getGenAllStops())
				&& this.getDBName().length() > 0
				&& this.getTablePrefix().length() > 0) {
			return true;
		}

		return false;
	}

	public void addErrorToLog(String err) {
		this.errorLog.add(err);
	}

	public void addWarningToLog(String warn) {
		this.warningLog.add(warn);
	}

	public void addErrorToLog2(String err) {
		this.errorLog2.add(err);
	}

	public void addWarningToLog2(String warn) {
		this.warningLog2.add(warn);
	}

	/* PRODUCER */
    
	protected void initializeDBTables() {
		try {
			Database.createConnection();
			Database.setMaxLines(ResultsModel.this.paylines.size());

			// Add symbols to the database
			if (ResultsModel.this.symbols != null
					&& ResultsModel.this.symbols.size() > 0) {
				for (int i = 0; i < ResultsModel.this.symbols
						.size(); i++) {
					Symbol symbol = ResultsModel.this.symbols
							.get(i);
					symbol.setID(i);
					Database.insertIntoTable(
							getSymbolsDBTableName(), symbol);
				}
				Database.flushBatch();
			}

			if (ResultsModel.this.blocks != null
					&& ResultsModel.this.blocks.size() > 0) {
				for (int i = 0; i < ResultsModel.this.blocks.size(); i++) {
					Block block = ResultsModel.this.blocks.get(i);
					block.setBlockNumber(i + 1);
					Database.insertIntoTable(getBlockDBTableName(),
							block);
				}
				Database.flushBatch();
			}

			if (ResultsModel.this.paylines != null
					&& ResultsModel.this.paylines.size() > 0) {
				for (int i = 0; i < ResultsModel.this.paylines
						.size(); i++) {
					Payline payline = ResultsModel.this.paylines
							.get(i);
					payline.setNumber(i);
					Database.insertIntoTable(
							getPaylinesDBTableName(), payline);
				}
				Database.flushBatch();
			}

			if (ResultsModel.this.basepaytable != null
					&& ResultsModel.this.basepaytable.size() > 0) {
				for (int i = 0; i < ResultsModel.this.basepaytable
						.size(); i++) {
					PaytableEntry paytableEntry = ResultsModel.this.basepaytable
							.get(i);
					paytableEntry.setEntryID(i + 1);
					Database.insertIntoTable(
							getBasePaytableDBTableName(),
							paytableEntry);
				}
				Database.flushBatch();
			}
			
			if (ResultsModel.this.bonuspaytable != null
					&& ResultsModel.this.bonuspaytable.size() > 0) {
				for (int i = 0; i < ResultsModel.this.bonuspaytable
						.size(); i++) {
					PaytableEntry paytableEntry = ResultsModel.this.bonuspaytable
							.get(i);
					paytableEntry.setEntryID(i + 1);
					Database.insertIntoTable(
							getBonusPaytableDBTableName(),
							paytableEntry);
				}
				Database.flushBatch();
			}

		} catch (Exception e) {
			ResultsModel.this.setError();
			ResultsModel.this.outputLog
					.outputStringAndNewLine("ERROR: Failed to connect to database. Message="
							+ e.getMessage());
		}
		
	}
	
	private void produce() {
		Thread t = new Thread() {
			public void run() {
				int total_freespin = 0;
				int extra_freespin = 0;
				boolean orign_mode = ResultsModel.this.simulator.getSeqStops();
				Result pre_result = null;
				
				ResultsModel.this.outputLog
						.outputStringAndNewLine("START PRODUCTION");

				int ts = 0;

				// Calculate the number of total spins if not in GamblersRuin mode
				if (! ResultsModel.this.genGamblersRuin) {
					
					if (ResultsModel.this.genForcedFreeSpins) {
						for (Block b : ResultsModel.this.ffsblocks) 
							ts += b.getNumSpins() * b.getNumRepeats();
						
					} else {
						for (Block b : ResultsModel.this.blocks) 
							ts += b.getNumSpins() * b.getNumRepeats();
					}

					ResultsModel.this.setTotalSpins(ts);

					ResultsModel.this.outputLog
							.outputStringAndNewLine("Total Spins: "
								+ Integer.toString(ts));
				}
				
				// Create Symbols, Blocks, and paytables DB tables
				if (ts > 0 || ResultsModel.this.genGamblersRuin) {
					initializeDBTables();
					

					try {
						//Producer Main Flow here
						// If in GamblersRuin mode
						if (ResultsModel.this.genGamblersRuin) {
							doGamblersRuinMode();
							
						// If in ForcedFreeSpin mode
						} else if (ResultsModel.this.genForcedFreeSpins) {
							doForcedFreeSpinsMode();
							
						// If in regular mode	
						} else {
							ResultsModel.this.currblock = ResultsModel.this.blocks
									.get(ResultsModel.this.currblockindex);
							ResultsModel.this.currlpe = new LossPercentageEntry(ResultsModel.this.currblock);
							ResultsModel.this.currpze = new PrizeSizeEntry();
							
							while (ResultsModel.this.currblock != null
									&& !ResultsModel.this.cancelled) {
	
								if (!ResultsModel.this.paused) {
	
									ResultsModel.this.checkBonusState();
									// Reset total_freespin and game mode
									if (! ResultsModel.this.bonusactive) {
										total_freespin = 0;
										ResultsModel.this.simulator.setSeqStops(orign_mode);
									}
									
									Result r = ResultsModel.this.simulator
											.simulateSpin();
									if (r != null) {
	
										r.setRecordNumber(ResultsModel.this
												.getCurrSpin() + 1);
										r.setBlockNumber(ResultsModel.this.blocks
												.get(ResultsModel.this.currblockindex)
												.getBlockNumber());
										r.setRepeatNumber(ResultsModel.this.currblockrepeat);
										
										// If there are free spins awarded
										if (r.freespinsawarded > 0) { 
											total_freespin += r.freespinsawarded;
											
											// Check if the number of free spins awarded exceeds the allowed
											if (total_freespin > ResultsModel.this.MAX_FREESPINS) 
												extra_freespin = ResultsModel.this.MAX_FREESPINS - (total_freespin - r.freespinsawarded);
											else 
												extra_freespin = r.freespinsawarded;
											
											// Update the BonusHitCounts list if not in bonus mode
											if (!r.bonusspin)
												ResultsModel.this.updateBonusSpinCounts(r.freespinsawarded);
											
											
											ResultsModel.this.addFreeSpins(extra_freespin);
										}
										
										
										// Add extra spins to total spin when get free spins in Do-All-Reel-Stops mode
										if (orign_mode || ResultsModel.this.genAllBonusSpins && r.bonusactivated) {
											ts += extra_freespin;
											ResultsModel.this.setTotalSpins(ts);
											ResultsModel.this.blocks.get(ResultsModel.this.currblockindex)
											.addNumSpins(extra_freespin);
											ResultsModel.this.simulator.setSeqStops(false);
											ResultsModel.this.currlpe.incrementNumFreeSpins(extra_freespin);
											extra_freespin = 0;
										}

										
										if (ResultsModel.this.bonusactive) {
											ResultsModel.this.decrementFreeSpins();									
										}
										
										// Update the LDW information on the previous spin result
										if (ResultsModel.this.ldwResetRequired) {
											ResultsModel.this.updateLDWWins(r.isLDWWin(), null);
											ResultsModel.this.updateLDWLosses(r.isLDWLose(), null);
											ResultsModel.this.ldwResetRequired = false;
										} else {
											ResultsModel.this.updateLDWWins(r.isLDWWin(), pre_result);
											ResultsModel.this.updateLDWLosses(r.isLDWLose(), pre_result);
											
											// Insert previous spin result into the database
											if (ResultsModel.this.createSpinTable)
												Database.insertIntoTable(ResultsModel.this
														.getSpinResultsDBTableName(), pre_result);
										}
	
										ResultsModel.this.incrementCurrSpin();
										
										
										// Update the PrizeSizeEntry if needed
										if (ResultsModel.this.genPrizeSize)
											currpze.updatePrizeSizeEntry(r);
										
										// Update the LossPercentageTable entry
										ResultsModel.this.updateLPE(r);
										
										pre_result = r;
										
										// If one block or one blockrepeat is done
										if (ResultsModel.this.ldwResetRequired) {
											pre_result.setLDWWins(ResultsModel.this.wins);
											pre_result.setLDWLosses(ResultsModel.this.losses);
											
											if (ResultsModel.this.createSpinTable)
												Database.insertIntoTable(ResultsModel.this
														.getSpinResultsDBTableName(), pre_result);
											
											ResultsModel.this.ldwResetRequired = false;
										}
									} else {
										ResultsModel.this.incrementFailedSpins();
										ResultsModel.this.outputLog
												.outputStringAndNewLine("Spin["
														+ Integer
																.toString(ResultsModel.this.currspin)
														+ "] - Failed!");
									}
								}
							}
	
							// Insert the last spin results into the database
							if (pre_result != null) {
								pre_result.setLDWWins(ResultsModel.this.wins);
								pre_result.setLDWLosses(ResultsModel.this.losses);
								
								if (ResultsModel.this.createSpinTable)
									Database.insertIntoTable(ResultsModel.this
											.getSpinResultsDBTableName(), pre_result);
								}
							
							// Update the base & bonus hit counts
							Database.updateTableHit(ResultsModel.this.getBasePaytableDBTableName(), 
									ResultsModel.this.basehittable);
							Database.updateTableHit(ResultsModel.this.getBonusPaytableDBTableName(), 
									ResultsModel.this.bonushittable);
							
							Database.flushBatch();
	
							ResultsModel.this.stop();
						}
					} catch (Exception e) {
						ResultsModel.this.stop();
						ResultsModel.this.outputLog
								.outputStringAndNewLine("ERROR: Production thread caught exception. Message="
										+ e.getMessage());
						e.printStackTrace();
					} finally {
						System.gc();
						//Note: The following code is moved to the shutdown hook in Main.java,
						//		so that it closes database connection only on exiting the application.
						/* try {
							Database.shutdownConnection();
							System.gc();
						} catch (SQLException e) {
							ResultsModel.this.outputLog
									.outputStringAndNewLine("ERROR: Closing database connection. Message="
											+ e.getMessage());
							e.printStackTrace();
						} */
					}
				} else {
					ResultsModel.this.stop();
				}
				
				ResultsModel.this.outputLog
						.outputStringAndNewLine("END PRODUCTION");
				
				// Add the bonus spins counts to log
				ResultsModel.this.outputLog.outputStringAndNewLine("Bonus Spin Counts: ");
				for (int i = 0; i < ResultsModel.this.bonusspincounts.size(); i++)
					ResultsModel.this.outputLog.outputStringAndNewLine("\t\t " + ResultsModel.this.bonusspincounts.get(i));
				
			}
		};
		
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();

	}

	

	protected void doGamblersRuinMode() {
		ResultsModel.this.currgrblock = ResultsModel.this.grblocks.get(ResultsModel.this.currblockindex);
		ResultsModel.this.currgre = new GamblersRuinEntry();
		int total_freespin = 0;
		int extra_freespin = 0;
		Result pre_result = null;
		
		try {
			while (ResultsModel.this.currgrblock != null
					&& !ResultsModel.this.cancelled) {
				
				if (!ResultsModel.this.paused) {
					// Check/update if still in bonus state
					ResultsModel.this.checkBonusState();
					// Reset total free spin count when no longer in bonus state
					if (!ResultsModel.this.bonusactive) 
						total_freespin = 0;
					
					// Simulate spin
					Result r = ResultsModel.this.simulator.simulateSpin();
					
					if (r != null) {
						// Set record numbers of the result
						r.setRecordNumber(ResultsModel.this.getCurrSpin() + 1);
						r.setBlockNumber(ResultsModel.this.currgrblock.getBlockNum());
						r.setRepeatNumber(ResultsModel.this.currblockrepeat);
						
						// If there are free spins awarded from the spin
						if (r.freespinsawarded > 0) {
							total_freespin += r.freespinsawarded;
							
							// Check if the number of free spins awarded exceeds the allowed
							if (total_freespin > ResultsModel.this.MAX_FREESPINS) 
								extra_freespin = ResultsModel.this.MAX_FREESPINS - (total_freespin - r.freespinsawarded);
							else 
								extra_freespin = r.freespinsawarded;
							
							// Add free spins to simulate
							ResultsModel.this.addFreeSpins(extra_freespin);
							
							// Add free spins to the Gamblers Ruin Entry
							currgre.addNumFreeSpins(extra_freespin);
							
							// Add bonus initialization
							if (!r.bonusspin)
								currgre.incrementNumBonusActivation();
							
							extra_freespin = 0;
						}
						
						if (ResultsModel.this.bonusactive)
							ResultsModel.this.decrementFreeSpins();
						
						ResultsModel.this.incrementGRCurrSpin(r);
						
						// Update the LDW information on the previous spin result
						if (ResultsModel.this.ldwResetRequired) {
							ResultsModel.this.updateLDWWins(r.isLDWWin(), null);
							ResultsModel.this.updateLDWLosses(r.isLDWLose(), null);
							ResultsModel.this.ldwResetRequired = false;
						} else {
							ResultsModel.this.updateLDWWins(r.isLDWWin(), pre_result);
							ResultsModel.this.updateLDWLosses(r.isLDWLose(), pre_result);
							
							// Insert previous spin result into the database
							if (ResultsModel.this.createSpinTable)
								Database.insertIntoTable(ResultsModel.this
										.getSpinResultsDBTableName(), pre_result);
						}
						
						pre_result = r;
						
						// If one block or one blockrepeat is done
						if (ResultsModel.this.ldwResetRequired) {
							pre_result.setLDWWins(ResultsModel.this.wins);
							pre_result.setLDWLosses(ResultsModel.this.losses);
							
							if (ResultsModel.this.createSpinTable)
								Database.insertIntoTable(ResultsModel.this
										.getSpinResultsDBTableName(), pre_result);
							
							ResultsModel.this.ldwResetRequired = false;
						}
					} // if spin result is not valid
				} // if paused
			} // while all grblocks finished 
			
			// Insert the last spin results into the database
			if (pre_result != null) {
				pre_result.setLDWWins(ResultsModel.this.wins);
				pre_result.setLDWLosses(ResultsModel.this.losses);
				
				if (ResultsModel.this.createSpinTable)
					Database.insertIntoTable(ResultsModel.this
							.getSpinResultsDBTableName(), pre_result);
				}
			
			// Update the base & bonus hit counts
			Database.updateTableHit(ResultsModel.this.getBasePaytableDBTableName(), 
					ResultsModel.this.basehittable);
			Database.updateTableHit(ResultsModel.this.getBonusPaytableDBTableName(), 
					ResultsModel.this.bonushittable);
			
			Database.flushBatch();
			
			ResultsModel.this.stop();
		} catch (Exception e) {
			ResultsModel.this.stop();
			ResultsModel.this.outputLog
					.outputStringAndNewLine("ERROR: Production thread caught exception. Message="
							+ e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	protected void doForcedFreeSpinsMode() {
		ResultsModel.this.currblock = ResultsModel.this.ffsblocks.get(ResultsModel.this.currblockindex);
		ResultsModel.this.currffs = new ForcedFreeSpinEntry();
		int total_freespin = 0;
		int extra_freespin = 0;
		Result pre_result = null;
		
		try {
			while (ResultsModel.this.currblock != null
					&& !ResultsModel.this.cancelled) {
				
				if (!ResultsModel.this.paused) {
					// Check/update if still in bonus state
					ResultsModel.this.checkBonusState();
					// Reset total free spin count when no longer in bonus state
					if (!ResultsModel.this.bonusactive) {
						//TODO stop here
						total_freespin = 0;
					}
					
					// Simulate spin
					Result r = ResultsModel.this.simulator.simulateSpin();
					
					if (r != null) {
						// Set record numbers of the result
						r.setRecordNumber(ResultsModel.this.getCurrSpin() + 1);
						r.setBlockNumber(ResultsModel.this.currblock.getBlockNumber());
						r.setRepeatNumber(ResultsModel.this.currblockrepeat);
						
						// If there are free spins awarded from the spin
						if (r.freespinsawarded > 0) {
							total_freespin += r.freespinsawarded;
							
							// Check if the number of free spins awarded exceeds the allowed
							if (total_freespin > ResultsModel.this.MAX_FREESPINS) 
								extra_freespin = ResultsModel.this.MAX_FREESPINS - (total_freespin - r.freespinsawarded);
							else 
								extra_freespin = r.freespinsawarded;
							
							// Add free spins to simulate
							ResultsModel.this.addFreeSpins(extra_freespin);
							
							extra_freespin = 0;
						}
						// Decrement the number of free spins
						if (ResultsModel.this.bonusactive)
							ResultsModel.this.decrementFreeSpins();
						
						ResultsModel.this.incrementCurrSpin();
						
						currffs.updateFFSE(r);
						
						// Update the LDW information on the previous spin result
						if (ResultsModel.this.ldwResetRequired) {
							ResultsModel.this.updateLDWWins(r.isLDWWin(), null);
							ResultsModel.this.updateLDWLosses(r.isLDWLose(), null);
							ResultsModel.this.ldwResetRequired = false;
						} else {
							ResultsModel.this.updateLDWWins(r.isLDWWin(), pre_result);
							ResultsModel.this.updateLDWLosses(r.isLDWLose(), pre_result);
							
							// Insert previous spin result into the database
							if (ResultsModel.this.createSpinTable)
								Database.insertIntoTable(ResultsModel.this
										.getSpinResultsDBTableName(), pre_result);
						}
						
						pre_result = r;
						
						// If one block or one blockrepeat is done
						if (ResultsModel.this.ldwResetRequired) {
							pre_result.setLDWWins(ResultsModel.this.wins);
							pre_result.setLDWLosses(ResultsModel.this.losses);
							
							if (ResultsModel.this.createSpinTable)
								Database.insertIntoTable(ResultsModel.this
										.getSpinResultsDBTableName(), pre_result);
							
							ResultsModel.this.ldwResetRequired = false;
						}
					} // if spin result is not valid
				} // if paused
			} // while all grblocks finished 
			
			// Insert the last spin results into the database
			if (pre_result != null) {
				pre_result.setLDWWins(ResultsModel.this.wins);
				pre_result.setLDWLosses(ResultsModel.this.losses);
				
				if (ResultsModel.this.createSpinTable)
					Database.insertIntoTable(ResultsModel.this
							.getSpinResultsDBTableName(), pre_result);
				}
			
			// Update the base & bonus hit counts
			Database.updateTableHit(ResultsModel.this.getBasePaytableDBTableName(), 
					ResultsModel.this.basehittable);
			Database.updateTableHit(ResultsModel.this.getBonusPaytableDBTableName(), 
					ResultsModel.this.bonushittable);
			
			Database.flushBatch();
			
			ResultsModel.this.stop();
		} catch (Exception e) {
			ResultsModel.this.stop();
			ResultsModel.this.outputLog
					.outputStringAndNewLine("ERROR: Production thread caught exception. Message="
							+ e.getMessage());
			e.printStackTrace();
		}
	}


	public void shutdown() {
		if (Database.isConnected()) {
			try {
				Database.shutdownConnection();
				System.gc();
			} catch (SQLException e) {
				ResultsModel.this.outputLog
						.outputStringAndNewLine("ERROR: Closing database connection. Message="
								+ e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	protected void generateDBTableName() {
		this.setTableSuffix();
		this.outputLog.outputStringAndNewLine("Set Blocks DB Table Name: "
				+ this.getBlockDBTableName());
		this.outputLog.outputStringAndNewLine("Set Results DB Table Name: "
				+ this.getSpinResultsDBTableName());
		this.outputLog.outputStringAndNewLine("Set Symbols DB Table Name: "
				+ this.getSymbolsDBTableName());
		this.outputLog.outputStringAndNewLine("Set Paylines DB Table Name: "
				+ this.getPaylinesDBTableName());
		this.outputLog
				.outputStringAndNewLine("Set Base Paytable DB Table Name: "
						+ this.getBasePaytableDBTableName());
		this.outputLog
				.outputStringAndNewLine("Set Bonus Paytable DB Table Name: "
						+ this.getBonusPaytableDBTableName());
		this.outputLog
				.outputStringAndNewLine("Set Reel Mappings DB Table Name: "
						+ this.getReelMappingsDBTableName());
	}

	protected void incrementFailedSpins() {
		this.failedspins++;
	}

	protected void addFreeSpins(int value) {
		this.freespins += value;
	}

	protected void decrementFreeSpins() {
		this.freespins--;
	}

	protected void checkBonusState() {
		this.bonusactive = (this.freespins > 0);
	}

	protected void incrementCurrSpin() {
		this.currspin++;
		// TODO debug required for ffs
		// If the currblock runs out of spins 
		if (this.currblock.incrementCurrSpin()) {
			// If currblock needs to be repeated
			if (this.currblockrepeat < this.currblock.repeats) {
				this.currblockrepeat++;
				
				this.ldwResetRequired = true;
				this.repeatComplete = true; 
				
				// Reset spins in the block to start a new repeat if not in ForceFreeSpins mode
				if (! ResultsModel.this.genForcedFreeSpins) {
					this.currblock.resetNumOfSpins();
					this.currblock.currspin = 0;
				}
			} else { // If the currblock is done
				this.currblockindex++;
				this.blockComplete = true;
				this.currblockrepeat = 1;
				
				// If there are more blocks
				// If in ForcedFreeSpins mode
				if (ResultsModel.this.genForcedFreeSpins) {
					if (this.currblockindex < this.ffsblocks.size()) {
						this.currblock = this.ffsblocks.get(currblockindex);
						this.ldwResetRequired = true;
					} else {
						this.currblock = null;
					}
				// If not in ForcedFreeSpins mode
				} else {
					if (this.currblockindex < this.blocks.size()) {
						this.currblock = this.blocks.get(currblockindex);
						this.ldwResetRequired = true;
					} else {
						this.currblock = null;
					}
				}
			}
		}

		this.UpdateViews();
	}
	
	protected void incrementGRCurrSpin(Result r) {
		this.currspin++;

		// If the currgrblock has balance <= 0
		if (this.currgrblock.incrementCurrSpin(r)) {
			// If currgrblock needs to be repeated
			if (this.currblockrepeat < this.currgrblock.repeats) {
				this.repeatComplete = true;
				this.currgre.updateGRE();
				this.currgre.resetCurrPeakBalance();
				this.currblockrepeat++;
				this.currgrblock.reset();
				this.ldwResetRequired = true;
				
			// If the currgrblock is done
			} else {
				this.blockComplete = true;
				this.currgre.updateGRE();
				
				// Insert GamblersRuinEntry to the database
				Database.insertIntoTable(getGamblersRuinDBTableName(), currgre, grblocks.size());

				
				this.currblockindex++;
				this.currblockrepeat = 1;
				
				// If there are more grblocks
				if (this.currblockindex < this.grblocks.size()) {
					this.currgrblock = this.grblocks.get(currblockindex);
					this.currgre.reset();
					this.ldwResetRequired = true;
				} else {
					this.currgrblock = null;
					this.currgre.reset();
				}
				
			}
		}
		this.UpdateViews();
	}
	
	protected void updateLPE(Result r) {
		// update the balance after each spin
		if (this.bonusactive)
			currlpe.setBalance(r.creditswon);
		else
			currlpe.setBalance(r.creditswon - currlpe.getBet());
		
		// update the bonusactivation count 
		if (r.bonusactivated && !r.bonusspin)
			currlpe.incrementBonusActivations();
		
		// update win/lose and ldws 
		if (r.creditswon > 0) {
			if ((r.creditswon - currlpe.getBet()) >= 0)
				currlpe.incrementWin();
			else {
				currlpe.incrementLdw();
			}
		} else 
			currlpe.incrementLoss();
		
		// if one repeat is completed
		if (this.repeatComplete) {
			currlpe.updateLossPercentage();
			currlpe.updateLossBalance();
			currlpe.incrementCurBalanceIndex();
			this.repeatComplete = false;
		}
		
		// if one block is completed
		if (this.blockComplete) {
			currlpe.updateLossPercentage();
			currlpe.updateLossBalance();
			currlpe.calculateAvgLossBalance();
			currlpe.calculateSD();

			
			try {
				Database.insertIntoTable(this.getLossPercentageDBTableName(), currlpe, this.blocks.size());
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			if (currblockindex < this.blocks.size())
				currlpe = new LossPercentageEntry(this.blocks.get(currblockindex));
			this.blockComplete = false;
		}
		
	}

	/* GET CONFIGURATION */

	private boolean loadBlocks(File file) {

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();
			this.readBlocks(doc);
		} catch (Exception e) {
			this.addErrorToLog("An exception occurred reading blocks file. Exception message: "
					+ e.getMessage()
					+ ", Stack Trace: "
					+ e.getStackTrace().toString());
		}

		this.outputLog
				.outputStringAndNewLine("Finished reading blocks file. Errors: "
						+ this.errorLog2.size()
						+ ", Warnings: "
						+ this.warningLog2.size());

		if (this.errorLog2.size() > 0) {
			for (String s : this.errorLog2) {
				this.outputLog.outputStringAndNewLine("ERROR: " + s);
			}
		}

		if (this.warningLog2.size() > 0) {
			for (String s : this.warningLog2) {
				this.outputLog.outputStringAndNewLine("WARNING: " + s);
			}
		}

		return (this.errorLog2.size() == 0);
	}

	private void readBlocks(Document doc) {
		// Read Gamblers Ruin Blocks
		if (this.genGamblersRuin) {
			NodeList list = doc.getElementsByTagName("grblock");
			
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) node;
					
					short numlines = -1;
					short linebet = -1;
					int bankroll = -1;
					short denomination = -1;
					int blockrepeats = -1;
					GRBlock grb = new GRBlock();
					
					try {
						numlines = Short.parseShort(e.getAttribute("numlines"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("GRBlock[" + Integer.toString(i)
								+ "] - Invalid value for 'numlines'. Value: "
								+ e.getAttribute("numlines"));
					}
					
					try {
						linebet = Short.parseShort(e.getAttribute("linebet"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("GRBlock[" + Integer.toString(i)
								+ "] - Invalid value for 'linebet'. Value: "
								+ e.getAttribute("linebet"));
					}
					
					try {
						bankroll = Integer.parseInt(e.getAttribute("bankroll"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("GRBlock[" + Integer.toString(i)
								+ "] - Invalid value for 'bankroll'. Value: "
								+ e.getAttribute("bankroll"));
					}
					
					try {
						denomination = Short.parseShort(e.getAttribute("denomination"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("GRBlock[" + Integer.toString(i)
								+ "] - Invalid value for 'denomination'. Value: "
								+ e.getAttribute("denomination"));
					}
					
					try {
						blockrepeats = Integer.parseInt(e.getAttribute("blockrepeats"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("GRBlock[" + Integer.toString(i)
								+ "] - Invalid value for 'blockrepeats'. Value: "
								+ e.getAttribute("blockrepeats"));
					}
					
					if (numlines > 0 && linebet > 0 && denomination > 0
							&& bankroll > 0 && blockrepeats > 0 && blockrepeats <= MAX_BLOCKREPEATS) {
						grb.setNumLines(numlines);
						grb.setLineBet(linebet);
						grb.setBankRoll(bankroll);
						grb.setDenomination(denomination);
						grb.setRepeats(blockrepeats);
						grb.setBlockNum(i);
						this.grblocks.add(grb);
					} else {
						this.addErrorToLog2("GRBlock["
								+ Integer.toString(i)
								+ "] - All attributes must be integers greater than 0; blockrepeats must be less than " +
								+ MAX_BLOCKREPEATS + ".");
					}
				}
			}
			
		// Read Forced Free Spin Blocks (or "ffsblock")	
		} else if (this.genForcedFreeSpins) {
			NodeList list = doc.getElementsByTagName("ffsblock");
			
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) node;
					
					short numlines = -1;
					short linebet = -1;
					int inifreespins = -1;
					short denomination = -1;
					int blockrepeats = -1;
					Block ffsb = new FFSBlock();
					
					try {
						numlines = Short.parseShort(e.getAttribute("numlines"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("FFSBlock[" + Integer.toString(i)
								+ "] - Invalid value for 'numlines'. Value: "
								+ e.getAttribute("numlines"));
					}
					
					try {
						linebet = Short.parseShort(e.getAttribute("linebet"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("FFSBlock[" + Integer.toString(i)
								+ "] - Invalid value for 'linebet'. Value: "
								+ e.getAttribute("linebet"));
					}
					
					try {
						inifreespins = Integer.parseInt(e.getAttribute("initialfs"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("FFSBlock[" + Integer.toString(i)
								+ "] - Invalid value for 'initialfs'. Value: "
								+ e.getAttribute("initialfs"));
					}
					
					try {
						denomination = Short.parseShort(e.getAttribute("denomination"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("FFSBlock[" + Integer.toString(i)
								+ "] - Invalid value for 'denomination'. Value: "
								+ e.getAttribute("denomination"));
					}
					
					try {
						blockrepeats = Integer.parseInt(e.getAttribute("blockrepeats"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("FFSBlock[" + Integer.toString(i)
								+ "] - Invalid value for 'blockrepeats'. Value: "
								+ e.getAttribute("blockrepeats"));
					}
					
					if (numlines > 0 && linebet > 0 && denomination > 0
							&& inifreespins > 0 && blockrepeats > 0 && blockrepeats <= MAX_BLOCKREPEATS) {
						ffsb.setNumLines(numlines);
						ffsb.setLineBet(linebet);
						ffsb.setNumSpins(inifreespins);
						ffsb.setDenomination(denomination);
						ffsb.setRepeats(blockrepeats);
						ffsb.setBlockNumber(i);
						this.ffsblocks.add(ffsb);
					} else {
						this.addErrorToLog2("FFSBlock["
								+ Integer.toString(i)
								+ "] - All attributes must be integers greater than 0; blockrepeats must be less than " +
								+ MAX_BLOCKREPEATS + ".");
					}
				}
			}
		// Read normal Blocks
		} else {
			NodeList list = doc.getElementsByTagName("block");
		
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) node;

					short numlines = -1;
					short linebet = -1;
					short denomination = -1;
					int numspins = -1;
					int blockrepeats = -1;

					try {
						numlines = Short.parseShort(e.getAttribute("numlines")); 
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("Block[" + Integer.toString(i)
								+ "] - Invalid value for 'numlines'. Value: "
								+ e.getAttribute("numlines"));
					}

					try {
						linebet = Short.parseShort(e.getAttribute("linebet")); 
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("Block[" + Integer.toString(i)
								+ "] - Invalid value for 'linebet'. Value: "
								+ e.getAttribute("linebet"));
					}

					try {
						denomination = Short.parseShort(e
								.getAttribute("denomination")); 
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("Block[" + Integer.toString(i)
								+ "] - Invalid value for 'denomination'. Value: "
								+ e.getAttribute("denomination"));
					}

					try {
						numspins = Integer.parseInt(e.getAttribute("numspins"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("Block[" + Integer.toString(i)
								+ "] - Invalid value for 'numspins'. Value: "
								+ e.getAttribute("numspins"));
					}
				
					try {
						blockrepeats = Integer.parseInt(e.getAttribute("blockrepeats"));
					} catch (NumberFormatException nfe) {
						this.addErrorToLog2("Block[" + Integer.toString(i)
								+ "] - Invalid value for 'blockrepeats'. Value: "
								+ e.getAttribute("blockrepeats"));
					}

					if (numlines > 0 && linebet > 0 && denomination > 0
							&& numspins > 0 && blockrepeats > 0 && blockrepeats <= MAX_BLOCKREPEATS) {
						Block b = new Block();
						b.setNumLines(numlines);
						b.setLineBet(linebet);
						b.setDenomination(denomination);
						b.setNumSpins(numspins);
						b.setRepeats(blockrepeats);
						this.blocks.add(b);
					} else {
						this.addErrorToLog2("Block["
								+ Integer.toString(i)
								+ "] - All attributes must be integers greater than 0; blockrepeats must be less than " +
								+ MAX_BLOCKREPEATS + ".");
					}

				}
			}
		}
	}

	private boolean loadConfiguration(File file) {

		clearAll();

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();
			this.readSymbols(doc);
			this.readPaylines(doc);
			this.readBasePaytable(doc);
			this.readBonusPaytable(doc);
			this.readReelMapping(doc);
		} catch (Exception e) {
			this.addErrorToLog("An exception occurred reading configuration file. Exception message: "
					+ e.getMessage()
					+ ", Stack Trace: "
					+ e.getStackTrace().toString());
		}

		this.outputLog
				.outputStringAndNewLine("Finished reading configuration file. Errors: "
						+ this.errorLog.size()
						+ ", Warnings: "
						+ this.warningLog.size());

		if (this.errorLog.size() > 0) {
			for (String s : this.errorLog) {
				this.outputLog.outputStringAndNewLine("ERROR: " + s);
			}
		}

		if (this.warningLog.size() > 0) {
			for (String s : this.warningLog) {
				this.outputLog.outputStringAndNewLine("WARNING: " + s);
			}
		}

		return (this.errorLog.size() == 0);
	}

	private void readSymbols(Document doc) {
		// Read Symbols
		NodeList list = doc.getElementsByTagName("symbol");
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) node;

				SymbolType type = convertToSymbolType(e.getAttribute("type"),
						e.getAttribute("id"));
				String alias = e.getAttribute("alias");

				if (type != SymbolType.UNKNOWN) {
					Symbol s = new Symbol();
					s.setAlias(alias);
					s.setType(type);
					this.symbols.add(s);
					
					if (type == SymbolType.SCATTER)
						this.scatter_symbol = alias;
				}
			}
		}
	}

	private void readPaylines(Document doc) {
		// Read Paylines
		NodeList list = doc.getElementsByTagName("payLine");
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) node;

				int r1 = convertPaylineR(e.getAttribute("r1"));
				int r2 = convertPaylineR(e.getAttribute("r2"));
				int r3 = convertPaylineR(e.getAttribute("r3"));
				int r4 = convertPaylineR(e.getAttribute("r4"));
				int r5 = convertPaylineR(e.getAttribute("r5"));

				if (r1 == REEL_UNK || r2 == REEL_UNK || r3 == REEL_UNK
						|| r4 == REEL_UNK || r5 == REEL_UNK) {
					this.addErrorToLog("Payline [number:"
							+ e.getAttribute("number")
							+ "] contains invalid 'r#' value(s)! Allowed values: 'top', 'mid', 'bot'. Invalid attributes: "
							+ ((r1 == REEL_UNK) ? " r1=" + e.getAttribute("r1")
									: "")
							+ ((r2 == REEL_UNK) ? " r2=" + e.getAttribute("r2")
									: "")
							+ ((r3 == REEL_UNK) ? " r3=" + e.getAttribute("r3")
									: "")
							+ ((r4 == REEL_UNK) ? " r4=" + e.getAttribute("r4")
									: "")
							+ ((r5 == REEL_UNK) ? " r5=" + e.getAttribute("r5")
									: ""));

				} else {
					Payline p = new Payline();
					p.setR1(r1);
					p.setR2(r2);
					p.setR3(r3);
					p.setR4(r4);
					p.setR5(r5);

					this.paylines.add(p);
				}
			}
		}
	}

	private void readBasePaytable(Document doc) {
		// Read Base Paytable
		for (int i = 0; i < 5; i++)
			this.basescatterpaytable.add(0);
		
		NodeList list = doc.getElementsByTagName("basePayTableEntry");
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) node;

				String winCode = e.getAttribute("winCode");
				String sequence = e.getAttribute("sequence");
				int payout;
				try {
					payout = Integer.parseInt(e.getAttribute("basePayout"));
				} catch (NumberFormatException nfe) {
					this.addErrorToLog("BasePaytableEntry["
							+ Integer.toString(i)
							+ "] Invalid value for 'basePayout'. Value='"
							+ e.getAttribute("basePayout") + "'");
					payout = PAYTABLE_BADPAYOUT;
				}
				WinType type = convertToWinType(e.getAttribute("type"),
						Integer.toString(i));

				if (payout != PAYTABLE_BADPAYOUT && type != WinType.UNKNOWN) {
					PaytableEntry pe = new PaytableEntry();
					pe.setWinCode(winCode);
					pe.setSequence(sequence);
					pe.setPayout(payout);
					pe.setType(type);

					this.basepaytable.add(pe);
					
					// update the scatter win paytable 
					if (type == WinType.SCATTER) {
						int numofsymbol = this.numOfSymbol(sequence) - 1;
						
						this.basescatterpaytable.set(numofsymbol, payout);
					}
					
					// Populate the basehittable
					if (type == WinType.WBBONUS) {
						this.wbbonus_sequence = sequence;
						
						this.basehittable.put(new SimpleEntry<String, Integer>(sequence, 2), 0);
						this.basehittable.put(new SimpleEntry<String, Integer>(sequence, 5), 0);
						this.basehittable.put(new SimpleEntry<String, Integer>(sequence, 8), 0);
						this.basehittable.put(new SimpleEntry<String, Integer>(sequence, 10), 0);
						this.basehittable.put(new SimpleEntry<String, Integer>(sequence, 15), 0);
						this.basehittable.put(new SimpleEntry<String, Integer>(sequence, 25), 0);
						
					} else {
						this.basehittable.put(new SimpleEntry<String, Integer>(sequence, payout), 0);
					}
					
				}
			}
		}
	}

	private void readBonusPaytable(Document doc) {
		// Read Bonus Paytable
		// Initialize the Bonus Scatter paytable
		for (Symbol s: this.symbols) {
			ArrayList<Integer> l = new ArrayList<Integer>();
			
			if (!s.getAlias().equals("W")) {
				for (int i = 0; i < 5; i++) 
					l.add(0);
			
				this.bonuscatterpaytable.put(s.getAlias(), l);
			}
		}
		
		
		NodeList list = doc.getElementsByTagName("bonusPayTableEntry");
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) node;

				String winCode = e.getAttribute("winCode");
				String sequence = e.getAttribute("sequence");
				int payout;
				try {
					payout = Integer.parseInt(e.getAttribute("basePayout"));
				} catch (NumberFormatException nfe) {
					this.addErrorToLog("BonusPaytableEntry["
							+ Integer.toString(i)
							+ "] Invalid value for 'basePayout'. Value='"
							+ e.getAttribute("basePayout") + "'");
					payout = PAYTABLE_BADPAYOUT;
				}
				WinType type = convertToWinType(e.getAttribute("type"),
						Integer.toString(i));

				if (payout != PAYTABLE_BADPAYOUT && type != WinType.UNKNOWN) {
					PaytableEntry pe = new PaytableEntry();
					pe.setWinCode(winCode);
					pe.setSequence(sequence);
					pe.setPayout(payout);
					pe.setType(type);

					this.bonuspaytable.add(pe);
					
					// Populate the bonuscatterpaytable and bonushittable
					if (type != WinType.WBBONUS) {
						String key = String.valueOf(sequence.charAt(0));
						int numOfSymbol = this.numOfSymbol(sequence) - 1;
						
						this.bonuscatterpaytable.get(key).set(numOfSymbol, payout);
						this.bonushittable.put(new SimpleEntry<String, Integer>(sequence, payout), 0);
					} else {
						this.bonushittable.put(new SimpleEntry<String, Integer>(sequence, 2), 0);
						this.bonushittable.put(new SimpleEntry<String, Integer>(sequence, 5), 0);
						this.bonushittable.put(new SimpleEntry<String, Integer>(sequence, 8), 0);
						this.bonushittable.put(new SimpleEntry<String, Integer>(sequence, 10), 0);
						this.bonushittable.put(new SimpleEntry<String, Integer>(sequence, 15), 0);
						this.bonushittable.put(new SimpleEntry<String, Integer>(sequence, 25), 0);
					}
				}
			}
		}
	}

	private void readReelMapping(Document doc) {
		// Read Reel Mapping
		NodeList list = doc.getElementsByTagName("reelStop");
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) node;

				String r1 = e.getAttribute("r1");
				String r2 = e.getAttribute("r2");
				String r3 = e.getAttribute("r3");
				String r4 = e.getAttribute("r4");
				String r5 = e.getAttribute("r5");

				if (!validateReelStop(r1)) {
					this.errorLog
							.add("ReelStop[id:" + e.getAttribute("id")
									+ "] R1 symbol alias, " + r1
									+ ", does not exists!");
				} else if (r1.length() > 0) {
					this.reel1.add(r1);
				}

				if (!validateReelStop(r2)) {
					this.errorLog
							.add("ReelStop[id:" + e.getAttribute("id")
									+ "] R2 symbol alias, " + r2
									+ ", does not exists!");
				} else if (r2.length() > 0) {
					this.reel2.add(r2);
				}

				if (!validateReelStop(r3)) {
					this.errorLog
							.add("ReelStop[id:" + e.getAttribute("id")
									+ "] R3 symbol alias, " + r3
									+ ", does not exists!");
				} else if (r3.length() > 0) {
					this.reel3.add(r3);
				}

				if (!validateReelStop(r4)) {
					this.errorLog
							.add("ReelStop[id:" + e.getAttribute("id")
									+ "] R4 symbol alias, " + r4
									+ ", does not exists!");
				} else if (r4.length() > 0) {
					this.reel4.add(r4);
				}

				if (!validateReelStop(r5)) {
					this.errorLog
							.add("ReelStop[id:" + e.getAttribute("id")
									+ "] R5 symbol alias, " + r5
									+ ", does not exists!");
				} else if (r5.length() > 0) {
					this.reel5.add(r5);
				}

			}
		}
	}

	private SymbolType convertToSymbolType(String value, String id) {
		if (value.toLowerCase().compareTo("scatter") == 0) {
			return SymbolType.SCATTER;
		} else if (value.toLowerCase().compareTo("bonus") == 0) {
			return SymbolType.BONUS;
		} else if (value.toLowerCase().compareTo("basic") == 0) {
			return SymbolType.BASIC;
		} else if (value.toLowerCase().compareTo("wbbonus") == 0) {
			return SymbolType.WBBONUS;
		} else {
			this.addErrorToLog("Symbol [id:"
					+ id
					+ "] contains invalid 'type' value! Allowed values: 'basic', 'bonus', 'scatter', 'wbbonus'.");
			return SymbolType.UNKNOWN;
		}
	}

	private WinType convertToWinType(String value, String id) {
		if (value.toLowerCase().compareTo("scatter") == 0) {
			return WinType.SCATTER;
		} else if (value.toLowerCase().compareTo("bonus") == 0) {
			return WinType.BONUS;
		} else if (value.toLowerCase().compareTo("basic") == 0) {
			return WinType.BASIC;
		} else if (value.toLowerCase().compareTo("bscatter") == 0) {
			return WinType.BSCATTER;
		} else if (value.toLowerCase().compareTo("wbbonus") == 0) {
			return WinType.WBBONUS;
		} else {
			this.addErrorToLog("PayTableEntry [id:"
					+ id
					+ "] contains invalid 'type' value! Allowed values: 'basic', 'bonus', 'scatter', 'bscatter', 'wbbonus'.");
			return WinType.UNKNOWN;
		}
	}

	private int convertPaylineR(String value) {
		if (value.toLowerCase().compareTo("top") == 0) {
			return REEL_TOP;
		} else if (value.toLowerCase().compareTo("mid") == 0) {
			return REEL_MID;
		} else if (value.toLowerCase().compareTo("bot") == 0) {
			return REEL_BOT;
		} else {
			return REEL_UNK;
		}
	}

	public static String convertPaylineR(int value) {
		if (value == REEL_TOP) {
			return "top";
		} else if (value == REEL_MID) {
			return "mid";
		} else if (value == REEL_BOT) {
			return "bot";
		} else {
			return "unk";
		}
	}

	private boolean validateReelStop(String value) {

		if (value.length() == 0)
			return true;

		for (Symbol s : this.symbols) {
			if (s.getAlias().compareTo(value) == 0) {
				return true;
			}
		}

		return false;
	}

	private void clearAll() {
		this.simulator.resetSimulator();
		this.resultqueue.clear();
		this.results.clear();
		this.symbols.clear();
		this.blocks.clear();
		this.paylines.clear();
		this.basepaytable.clear();
		this.bonuspaytable.clear();
		this.reel1.clear();
		this.reel2.clear();
		this.reel3.clear();
		this.reel4.clear();
		this.reel5.clear();
		this.currspin = 0;
		this.currconsumedspin = 0;
		this.cancelled = false;
		this.running = false;
		this.paused = false;
		this.error = false;
		this.currblock = null;
		this.currblockindex = 0;
		this.bonusactive = false;
		this.failedspins = 0;
		this.freespins = 0;
		this.errorLog.clear();
		this.warningLog.clear();
		this.errorLog2.clear();
		this.warningLog2.clear();
	}

	/* CLASSES */

	public class Result {
		private short reelstop1 = -1;
		private short reelstop2 = -1;
		private short reelstop3 = -1;
		private short reelstop4 = -1;
		private short reelstop5 = -1;

		private short numlines = 1;
		private short linebet = 1;
		private double denomination = 1;

		private int creditswon = 0;
		private int ldw_wins = 0;
		private int ldw_losses = 0;
		private long blocknumber = 0;
		private long recordNumber = 0;
		private int repeatNumber = 0;
		private short lineswon = 0;
		private int scatter = 0;
		private boolean bonusactivated = false;
		private boolean bonusspin = false;

		private short freespinsawarded = 0;

		private boolean nullobject = false;

		private ArrayList<Integer> freestormwinamounts;
		private ArrayList<Integer> linecreditwinamounts;
		private ArrayList<Integer> wbbonuscreditwin;
		private int maxlines = 0;

		public Result(int maxlines) {
			this.maxlines = maxlines;
			freestormwinamounts = new ArrayList<Integer>();
			for (int i = 0; i < this.maxlines; i++) {
				freestormwinamounts.add(0);
			}
			
			linecreditwinamounts = new ArrayList<Integer>();
			for (int i = 0; i < this.maxlines; i++) {
				linecreditwinamounts.add(0);
			}
			
			wbbonuscreditwin = new ArrayList<Integer>();
			for (int i = 0; i < 3; i++) {
				wbbonuscreditwin.add(0);
			}
		}

		public void setNullObject(boolean value) {
			this.nullobject = value;
		}

		public boolean getNullObject() {
			return this.nullobject;
		}

		public void reset() {
			reelstop1 = -1;
			reelstop2 = -1;
			reelstop3 = -1;
			reelstop4 = -1;
			reelstop5 = -1;
			numlines = 1;
			linebet = 1;
			denomination = 1;
			creditswon = 0;
			lineswon = 0;
			scatter = 0;
			bonusactivated = false;
			bonusspin = false;
			freespinsawarded = 0;
			
			for (int i = 0; i < this.maxlines; i++) {
				freestormwinamounts.set(i, 0);
			}
			
			for (int i = 0; i < this.maxlines; i++) {
				linecreditwinamounts.set(i, 0);
			}
			
			for (int i=0; i < this.wbbonuscreditwin.size(); i++) {
				wbbonuscreditwin.set(i, 0);
			}
		}

		@Override
		public String toString() {

			String s = "";

			s = "[Result:" + " r1=" + Integer.toString(reelstop1) + " r2="
					+ Integer.toString(reelstop2) + " r3="
					+ Integer.toString(reelstop3) + " r4="
					+ Integer.toString(reelstop4) + " r5="
					+ Integer.toString(reelstop5) + " numlines="
					+ Integer.toString(numlines) + " linebet="
					+ Short.toString(linebet) + " denom="
					+ Double.toString(getFormattedDenomination())
					+ " creditswon=" + Integer.toString(creditswon)
					+ " lineswon=" + Integer.toString(lineswon) + " scatter="
					+ Integer.toString(scatter) + " isbonusactivated="
					+ Boolean.toString(bonusactivated) + " isbonusspin="
					+ Boolean.toString(bonusspin) + " freespinsawarded="
					+ Integer.toString(freespinsawarded)
					+ " wbbonuscreditwin="
					+ wbbonuscreditwin.toString()
					+ " freestormwinamounts="
					+ freestormwinamounts.toString()
					+ " linecreditwinamounts="
					+ linecreditwinamounts.toString();

			return s;
		}

		public void addFreeStormWinAmount(int index, int value) {
			if (index < this.freestormwinamounts.size())
				this.freestormwinamounts.set(index, (this.freestormwinamounts.get(index) + value));
			else
				System.err
						.println("AddLineWinAmount: Attempted to add line free storm win past max lines. Index: "
								+ Integer.toString(index));
		}

		public void addLineCreditWinAmount(int index, int value) {
			if (index < this.linecreditwinamounts.size())
				this.linecreditwinamounts.set(index, (this.linecreditwinamounts.get(index) + value));
			else
				System.err
						.println("AddLineWinAmount: Attempted to add line credit win past max lines. Index: "
								+ Integer.toString(index));
		}
		
		public void addWBBonusCreditWin(int index, int value) {
			if (index < this.wbbonuscreditwin.size())
				this.wbbonuscreditwin.set(index, value);
			else 
				System.err
						.println("AddWBBonusCreditWin: Attempted to add Weather Beacon Bonus credit win other than line 0, 1, or 2. Index: "
								+ Integer.toString(index));
		}

		public void incrementLinesWon() {
			this.lineswon++;
		}

		public void setRecordNumber(long recordNumber) {
			this.recordNumber = recordNumber;
		}

		public void setReelStop1(short value) {
			this.reelstop1 = value;
		}

		public void setReelStop2(short value) {
			this.reelstop2 = value;
		}

		public void setReelStop3(short value) {
			this.reelstop3 = value;
		}

		public void setReelStop4(short value) {
			this.reelstop4 = value;
		}

		public void setReelStop5(short value) {
			this.reelstop5 = value;
		}

		public void setNumLines(short value) {
			this.numlines = value;
		}

		public void setBlockNumber(long value) {
			this.blocknumber = value;
		}
		
		public void setRepeatNumber(int value) {
			this.repeatNumber = value;
		}

		public void setLineBet(short value) {
			this.linebet = value;
		}

		public void setDenomination(short value) {
			this.denomination = value;
		}

		public void setDenomination(double value) {
			this.denomination = value;
		}
		
		public void setCreditsWon(int value) {
			this.creditswon = value;
		}

		public void setLDWWins(int value) {
			this.ldw_wins = value;
		}
		
		public void setLDWLosses(int value) {
			this.ldw_losses = value;
		}
		
		public void setLinesWon(short value) {
			this.lineswon = value;
		}

		public void setScatter(int value) {
			this.scatter += value;
		}

		public void setBonusActivated(boolean value) {
			this.bonusactivated = value;
		}

		public void setBonusSpin(boolean value) {
			this.bonusspin = value;
		}

		public void addFreeSpinsAwarded(short value) {
			this.freespinsawarded += value;
		}

		public void setFreeStormWinAmounts(ArrayList<Integer> value) {
			this.freestormwinamounts = value;
		}

		public void setLineCreditWinAmounts(ArrayList<Integer> value) {
			this.linecreditwinamounts = value;
		}
		
		public void setWBBonusCreditWin(ArrayList<Integer> value) {
			this.wbbonuscreditwin = value;
		}

		public long getRecordNumber() {
			return this.recordNumber;
		}

		public long getBlockNumber() {
			return this.blocknumber;
		}
		
		public int getRepeatNumber() {
			return this.repeatNumber;
		}

		public short getReelStop1() {
			return this.reelstop1;
		}

		public short getReelStop2() {
			return this.reelstop2;
		}

		public short getReelStop3() {
			return this.reelstop3;
		}

		public short getReelStop4() {
			return this.reelstop4;
		}

		public short getReelStop5() {
			return this.reelstop5;
		}

		public short getNumLines() {
			return this.numlines;
		}

		public short getLineBet() {
			return this.linebet;
		}

		public double getDenomination() {
			return this.denomination;
		}

		public int getCreditsWon() {
			return this.creditswon;
		}

		public int getLDWWins() {
			return this.ldw_wins;
		}
		
		public int getLDWLosses() {
			return this.ldw_losses;
		}
		
		public short getLinesWon() {
			return this.lineswon;
		}

		public int getScatter() {
			return this.scatter;
		}

		public boolean getBonusActivated() {
			return this.bonusactivated;
		}

		public boolean getBonusSpin() {
			return this.bonusspin;
		}
		
		public boolean isLDWWin() {
			return this.creditswon > 0;
		}
		
		public boolean isLDWLose() {
			if (ResultsModel.this.bonusactive)
				return true;
			else
				return (this.creditswon - this.linebet * this.numlines) >= 0;
		}

		public short getFreeSpinsAwarded() {
			return this.freespinsawarded;
		}

		public double getFormattedDenomination() {
			return ResultsModel.roundTwoDecimals(this.denomination / 100.0);
		}

		public int getLineCreditWinAmount(int index) {
			if (index >= 0 && index < this.linecreditwinamounts.size()) {
				return this.linecreditwinamounts.get(index);
			}

			return -1;
		}
		
		public int getWBBonusCreditWinOn(int index) {
			if (index >= 0 && index < this.wbbonuscreditwin.size()) {
				return this.wbbonuscreditwin.get(index);
			}
			
			return -1;
		}

		public int getFreeStormWinAmount(int index) {
			if (index >= 0 & index < this.freestormwinamounts.size()) {
				return this.freestormwinamounts.get(index);
			}
			
			return -1;
		}

		public ArrayList<Integer> getFreeStormWinAmounts() {
			return this.freestormwinamounts;
		}

		public ArrayList<Integer> getLineCreditWinAmounts() {
			return this.linecreditwinamounts;
		}

		public ArrayList<Integer> getWBBonusCreditWin() {
			return this.wbbonuscreditwin;
		}
	}

	public class LossPercentageEntry {
		private int numspins = 0;
		private short numline = 0;
		private int numfreespins = 0;
		private long blocknum = 0;
		private double denomination = 0;
		private int bet = 0;
		private int loss = 0;
		private int ldw = 0;
		private int win = 0;
		
		private double initialbalance = 0;
		private double avglossbalance = 0;
		private int curBalanceIndex = 0;
		private int curBonusActivations = 0;
		
		private int repeats = 0;
		private double sd = 0;
		
		private ArrayList<Integer> losspercentages = new ArrayList<Integer>();
		private List<Integer> bonusActivations = new ArrayList<Integer>();
		private ArrayList<Double> balances = new ArrayList<Double>();
		private ArrayList<Range> ranges = new ArrayList<Range>();

		public LossPercentageEntry(Block currblock) {
			this.numspins = currblock.getNumSpins();
			this.numline = currblock.getNumLines();
			this.blocknum = currblock.getBlockNumber();
			this.repeats = currblock.getNumRepeats();
			this.denomination = currblock.getFormattedDenomination();
			
			this.bet = currblock.getNumLines() * currblock.getLineBet();
			this.curBalanceIndex = 0;
			
			this.initialbalance = numspins * numline * currblock.getLineBet();
			
			// populate the balance array
			for(int i = 0; i < this.repeats; i++) 
				this.balances.add(initialbalance);
		
			// populate the loss percentage range check array
			double percent = 1;
			while (percent >= -9) {
				if (percent == -9) {
					ranges.add(new Range(-9, -9));
					percent -= 0.25;
				} else {
					ranges.add(new Range(percent - 0.25, percent));
					percent -= 0.25;
				}
			}
			
			// populate the loss percentage array
			for(int i = 0; i < ranges.size(); i++)
				this.losspercentages.add(0);
			
			// populate the bonusactivation array
			for (int i = 0; i < ranges.size(); i++)
				this.bonusActivations.add(0);
		}
		
		public void updateLossBalance() {
			double currbalance = this.balances.get(curBalanceIndex);
			
			this.balances.set(curBalanceIndex, currbalance - initialbalance);
		}

		public int getNumSpins() {
			return numspins;
		}

		public void setNumSpins(int numspins) {
			this.numspins = numspins;
		}

		public short getNumLine() {
			return numline;
		}

		public void setNumLine(short numline) {
			this.numline = numline;
		}
		
		public long getBlockNum() {
			return this.blocknum;
		}

		public int getLoss() {
			return loss;
		}

		public void incrementLoss() {
			this.loss++;
		}

		public int getLdw() {
			return ldw;
		}

		public void incrementLdw() {
			this.ldw++;
		}

		public int getWin() {
			return win;
		}

		public double getSd() {
			return sd;
		}

		private void calculateSD() {
			double sdbalance = 0;
			
			for (double b : this.balances)
				sdbalance += (avglossbalance - b * this.denomination) * (avglossbalance - b * this.denomination);
			
			sdbalance /= this.balances.size();
			this.sd = Math.sqrt(sdbalance);
		}

		public void incrementWin() {
			this.win++;
		}
		
		public void incrementCurBalanceIndex() {
			this.curBalanceIndex++;
		}

		public void setBalance(int value) {
			this.balances.set(curBalanceIndex, balances.get(curBalanceIndex) + value);
		}
		
		public int getLossPercentage(int index) {
			return this.losspercentages.get(index);
		}
		
		public ArrayList<Integer> getLossPercentages() {
			return this.losspercentages;
		}
		
		public int getBet() {
			return this.bet;
		}
		
		public void incrementLossPercentage(int index) {
			this.losspercentages.set(index, this.losspercentages.get(index) + 1);
		}
		
		public void calculateAvgLossBalance() {
			int totallossbalance = 0;
			
			//Note: balances are stored as credits value in the list, hence need to be converted into dollar values.
			for (double b : this.balances)
				totallossbalance += b * this.denomination;
			
			this.avglossbalance = totallossbalance / repeats;
		}
		
		public double getAvgLossBalance() {
			return this.avglossbalance;
		}
		
		public void updateLossPercentage() {
			double losspercentage;
			DecimalFormat df = new DecimalFormat("#.########");
			
			losspercentage = (initialbalance - balances.get(curBalanceIndex))
					/ (double)initialbalance;
			losspercentage = Double.valueOf(df.format(losspercentage));
			
			for (int i = 0; i < this.ranges.size(); i++) {
				boolean isInRange = ranges.get(i).isLossPercentageInRange(losspercentage);
				
				if (isInRange) {
					this.incrementLossPercentage(i);
					this.addAvgBonusActivation(i);
					break;
				}
					
			}
			
		}
		
		private void addAvgBonusActivation(int index) {
			int temp = this.bonusActivations.get(index);
				
			this.bonusActivations.set(index, temp + this.curBonusActivations);
			this.curBonusActivations = 0;
		}

		public int getNumFreeSpins() {
			return numfreespins;
		}

		public void incrementNumFreeSpins(int numfreespins) {
			this.numfreespins += numfreespins;
		}
		
		public void incrementBonusActivations() {
			this.curBonusActivations++;
		}

		public ArrayList<Range> getRangeArray() {
			return this.ranges;
		}

		public List<Integer> getBonusActivations() {
			return this.bonusActivations;
		}
		
		public int getAvgBonusActivation(int index) {
			return this.bonusActivations.get(index);
		}
		
	}
	
	public class Symbol {
		private SymbolType type = SymbolType.BASIC;
		private String alias = "";
		private int id;

		public Symbol() {
		}

		public void setType(SymbolType value) {
			this.type = value;
		}

		public void setAlias(String value) {
			this.alias = value;
		}

		public void setID(int id) {
			this.id = id;
		}

		public SymbolType getType() {
			return this.type;
		}

		public String getAlias() {
			return this.alias;
		}

		public int getID() {
			return this.id;
		}
	}

	public class Block {
		private short numlines = 1;
		private short linebet = 1;
		private short denomination = 1;
		private int numspins = 0;
		private int initialspins = 0;
		private int repeats = 1;
		private int currspin = 0;
		private long blockNumber = 0;
		
		public Block() {
		}

		public void resetNumOfSpins() {
			this.numspins = this.initialspins;
			
		}

		public void addNumSpins(int value) {
			this.numspins += value;
		}

		public boolean incrementCurrSpin() {
			currspin++;
			return (currspin >= numspins);
		}

		public void setNumLines(short value) {
			this.numlines = value;
		}

		public void setLineBet(short value) {
			this.linebet = value;
		}

		public void setDenomination(short value) {
			this.denomination = value;
		}

		public void setNumSpins(int value) {
			this.numspins = value;
			this.initialspins = value;
		}

		public void setRepeats(int value) {
			this.repeats = value;
		}
		
		public void setBlockNumber(long value) {
			this.blockNumber = value;
		}

		public short getNumLines() {
			return this.numlines;
		}

		public short getLineBet() {
			return this.linebet;
		}

		public short getDenomination() {
			return this.denomination;
		}

		public long getBlockNumber() {
			return this.blockNumber;
		}
		
		public int getNumSpins() {
			return this.numspins;
		}
		
		public int getNumRepeats() {
			return this.repeats;
		}

		public double getFormattedDenomination() {
			return ResultsModel.roundTwoDecimals(this.denomination / 100.0);
		}

	}
	
	public class GRBlock {
		private short numlines = 1;
		private short linebet = 1;
		private double denomination = 1;
		private int bankroll = 0;
		private int repeats = 1;
		private int currspin = 0;
		private long blocknum = 0;
		private double currbalance = 0;
		
		public int getBankRoll() {
			return bankroll;
		}
		
		public void setRepeats(int blockrepeats) {
			this.repeats = blockrepeats;
			
		}

		public void setDenomination(short denom) {
			this.denomination = (double)denom / this.numlines;
			
		}

		public void setLineBet(short linebet) {
			this.linebet = linebet;
			
		}

		public void setNumLines(short numlines) {
			this.numlines = numlines;
			
		}

		public void setBankRoll(int bankroll) {
			this.bankroll = bankroll;
		}
		
		public int getCurrSpin() {
			return currspin;
		}
		
		public void setCurrSpin(int currspin) {
			this.currspin = currspin;
		}
		
		public boolean incrementCurrSpin(Result r) {
			updateCurrBalance(r);
			
			if (this.currbalance <= 0)
				return true;
			else {
				this.currspin++;
				return false;
			}
		}
		
		public short getNumLine() {
			return numlines;
		}
		
		public long getBlockNum() {
			return blocknum;
		}
		
		public void setBlockNum(long blocknum) {
			this.blocknum = blocknum;
		}
		
		public void reset() {
			this.currspin = 0;
			currbalance = (double)this.bankroll;
		}

		public double getCurrBalance() {
			return currbalance;
		}
		
		public void setCurrBalance(int initial_balance) {
			this.currbalance = (double)initial_balance;
		}

		public double getFormattedDenomination() {
			return ResultsModel.roundTwoDecimals(this.denomination / 100.0);
		}
		
		public void updateCurrBalance(Result r) {
			if (ResultsModel.this.bonusactive)
				currbalance += r.getCreditsWon() * getFormattedDenomination();
			else {
				currbalance -= linebet * numlines * getFormattedDenomination();
				currbalance += r.getCreditsWon() * getFormattedDenomination();
			}
			
			// Update the peak balance in the current Gamblers Ruin Entry
			if (currbalance > ResultsModel.this.currgre.currpeakbalance)
				ResultsModel.this.currgre.setCurrPeakBalance(currbalance);
			
			// Update the win/loss/ldw info in the current Gamblers Ruin Entry
			if (ResultsModel.this.bonusactive)
				ResultsModel.this.currgre.incrementWins();
			
			else if (r.creditswon > 0) {
				if ((r.creditswon - r.linebet * r.numlines) < 0)
					ResultsModel.this.currgre.incrementLdws();
				else
					ResultsModel.this.currgre.incrementWins();
			} else 
				ResultsModel.this.currgre.incrementLosses();
		}

		
	}

	public class FFSBlock extends Block {
		private boolean startFreeSpin = false;
		
		public boolean getStartFreeSpin() {
			return startFreeSpin;
		}
		
		public void resetStartFreeSpin() {
			startFreeSpin = false;
		}
		
		@Override
		public boolean incrementCurrSpin() {
			// Only increment spin in bonus mode, not the bonus initialization spin
			super.currspin++;
			currffs.incrementCurrSpin();
			return ResultsModel.this.freespins == 0;
		}
		
		//TODO add extended methods here		
	}
	
	public class Payline {
		/**
		 * values are -1, 0, 1 or bottom, mid, top
		 */
		private int r1 = REEL_UNK;
		private int r2 = REEL_UNK;
		private int r3 = REEL_UNK;
		private int r4 = REEL_UNK;
		private int r5 = REEL_UNK;

		private int number;

		public Payline() {
		}

		public void setR1(int value) {
			this.r1 = value;
		}

		public void setR2(int value) {
			this.r2 = value;
		}

		public void setR3(int value) {
			this.r3 = value;
		}

		public void setR4(int value) {
			this.r4 = value;
		}

		public void setR5(int value) {
			this.r5 = value;
		}

		public void setNumber(int value) {
			this.number = value;
		}

		public int getNumber() {
			return this.number;
		}

		public int getR1() {
			return this.r1;
		}

		public int getR2() {
			return this.r2;
		}

		public int getR3() {
			return this.r3;
		}

		public int getR4() {
			return this.r4;
		}

		public int getR5() {
			return this.r5;
		}
	}

	public class PaytableEntry {
		private String wincode = "";
		private String sequence = "";
		private int payout = 0;
		private WinType type = null;
		private int entryID;

		public PaytableEntry() {
		}

		public void setEntryID(int value) {
			this.entryID = value;
		}

		public void setWinCode(String value) {
			this.wincode = value;
		}

		public void setSequence(String value) {
			this.sequence = value;
		}

		public void setPayout(int value) {
			this.payout = value;
		}

		public void setType(WinType value) {
			this.type = value;
		}

		public String getWinCode() {
			return this.wincode;
		}

		public int getEntryID() {
			return this.entryID;
		}

		public String getSequence() {
			return this.sequence;
		}

		public int getPayout() {
			return this.payout;
		}

		public WinType getType() {
			return this.type;
		}
	}

	class Simulator {
		private String symbolset[][] = new String[5][3];
		private Result r;
		private ResultsModel model = null;

		private boolean seqstops = false;

		private short r1stop = 0;
		private short r2stop = 0;
		private short r3stop = 0;
		private short r4stop = 0;
		private short r5stop = 0;

		private boolean hiteverystop = false;

		public Simulator(ResultsModel model) {
			this.model = model;
		}

		public void resetSimulator() {
			resetStops();
		}

		public void setSeqStops(boolean value) {
			this.seqstops = value;
		}

		public boolean getSeqStops() {
			return this.seqstops;
		}

		private void resetStops() {
			r1stop = 0;
			r2stop = 0;
			r3stop = 0;
			r4stop = 0;
			r5stop = 0;
		}
		
		private ArrayList<String> convertSymbolSet() {
			ArrayList<String> symbollist = new ArrayList<String>();
			
			for (String[] row : this.symbolset) {
				for (String symbol : row) {
					symbollist.add(symbol);
				}
			}
			return symbollist;
		}
		
		/**
		 * Method used to simulate one spin.
		 * 
		 * @return   the Result object corresponding to this one spin.
		 */
		public Result simulateSpin() {
			r = new Result(ResultsModel.this.paylines.size());
			try {

				reset();

				short reelstop1 = 0;
				short reelstop2 = 0;
				short reelstop3 = 0;
				short reelstop4 = 0;
				short reelstop5 = 0;
				
				// Generate the first spin of ForcedFreeSpin mode
				if (ResultsModel.this.genForcedFreeSpins && ResultsModel.this.currblock.currspin == 0) {
					int inifreespins = ResultsModel.this.currblock.getNumSpins();
					
					switch (inifreespins) {
					case 3:
						reelstop1 = 5;
						reelstop2 = 14;
						reelstop3 = 12;
						reelstop4 = 3;
						reelstop5 = 10;
						break;
					case 10:
						reelstop1 = 5;
						reelstop2 = 14;
						reelstop3 = 12;
						reelstop4 = 15;
						reelstop5 = 10;
						break;
					case 15:
						reelstop1 = 5;
						reelstop2 = 14;
						reelstop3 = 12;
						reelstop4 = 15;
						reelstop5 = 8;
						break;
					default:
						break;
							
					}
					
				// If in generate random results mode	
				} else if (!seqstops) {
					reelstop1 = (short) (Math.random() * this.model.reel1
							.size());
					reelstop2 = (short) (Math.random() * this.model.reel2
							.size());
					reelstop3 = (short) (Math.random() * this.model.reel3
							.size());
					reelstop4 = (short) (Math.random() * this.model.reel4
							.size());
					reelstop5 = (short) (Math.random() * this.model.reel5
							.size());
				// If in generate all reel stop mode
				} else {
					reelstop1 = r1stop;
					reelstop2 = r2stop;
					reelstop3 = r3stop;
					reelstop4 = r4stop;
					reelstop5 = r5stop;

					r1stop++;
					if (r1stop >= this.model.reel1.size()) {
						r1stop = 0;
						r2stop++;
					}
					if (r2stop >= this.model.reel2.size()) {
						r2stop = 0;
						r3stop++;
					}
					if (r3stop >= this.model.reel3.size()) {
						r3stop = 0;
						r4stop++;
					}
					if (r4stop >= this.model.reel4.size()) {
						r4stop = 0;
						r5stop++;
					}
					if (r5stop >= this.model.reel5.size()) {
						r5stop = 0;
						this.setHitEveryStop(true);
					}
				}
				

// 				TEST: Cheat spin results here 				
//				reelstop1 = (short)14;
//				reelstop2 = (short)30;
//				reelstop3 = (short)25;
//				reelstop4 = (short)0;
//				reelstop5 = (short)12;
//				this.model.bonusactive = true;
				
				/* populate symbol set */
				populateSymbolSetColumn(reelstop1, REEL_ONE, this.model.reel1);
				populateSymbolSetColumn(reelstop2, REEL_TWO, this.model.reel2);
				populateSymbolSetColumn(reelstop3, REEL_THREE, this.model.reel3);
				populateSymbolSetColumn(reelstop4, REEL_FOUR, this.model.reel4);
				populateSymbolSetColumn(reelstop5, REEL_FIVE, this.model.reel5);
				
				r.setReelStop1(reelstop1);
				r.setReelStop2(reelstop2);
				r.setReelStop3(reelstop3);
				r.setReelStop4(reelstop4);
				r.setReelStop5(reelstop5);

				if (this.model.genGamblersRuin) {
					r.setNumLines(this.model.currgrblock.numlines);
					r.setLineBet(this.model.currgrblock.linebet);
					r.setDenomination(this.model.currgrblock.denomination);
				} else {
					r.setNumLines(this.model.currblock.numlines);
					r.setLineBet(this.model.currblock.linebet);
					r.setDenomination(this.model.currblock.denomination);
				}
				/* calculate payout */
				calculatePayout();

				return r;

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		protected void setHitEveryStop(boolean value) {
			this.hiteverystop = value;
			if (this.hiteverystop) {
				this.model.cancel();
				this.hiteverystop = false;
			}
		}

		protected boolean getHitEveryStop() {
			return this.hiteverystop;
		}

		private void reset() {
			// r.reset();
			clearSymbolSet();
		}

		private void clearSymbolSet() {
			for (int i = 0; i < symbolset.length; i++) {
				for (int j = 0; j < symbolset[i].length; j++) {
					symbolset[i][j] = "?";
				}
			}
		}

		private void populateSymbolSetColumn(int stop, int reelnum,
				ArrayList<String> reel) throws IndexOutOfBoundsException {

			String mid = reel.get(stop);
			String top = "?";
			String bot = "?";

			if (stop == reel.size() - 1) {
				top = reel.get(stop - 1);
				bot = reel.get(0);
			} else if (stop == 0) {
				top = reel.get(reel.size() - 1);
				bot = reel.get(stop + 1);
			} else {
				top = reel.get(stop - 1);
				bot = reel.get(stop + 1);
			}
			
			symbolset[reelnum][REEL_TOP] = top;
			symbolset[reelnum][REEL_MID] = mid;
			symbolset[reelnum][REEL_BOT] = bot;
		}

		private void calculatePayout() {
			String winsequence = "";
			Payline p = null;
			
			calculateScatterPayout();
			calculateWBBonusPayout();
			
			if (!this.model.bonusactive) {
				// If it is in Gamblers Ruin mode
				if (!this.model.genGamblersRuin) {
					for (int i = 0; i < this.model.currblock.numlines
							&& i < this.model.paylines.size(); i++) {
						p = this.model.paylines.get(i);
						winsequence = "";
						winsequence += symbolset[REEL_ONE][p.getR1()];
						winsequence += symbolset[REEL_TWO][p.getR2()];
						winsequence += symbolset[REEL_THREE][p.getR3()];
						winsequence += symbolset[REEL_FOUR][p.getR4()];
						winsequence += symbolset[REEL_FIVE][p.getR5()];
	
						calculatePayoutBase(winsequence, i);
					}
				// If not in Gamblers Ruin mode
				} else {
					for (int i = 0; i < this.model.currgrblock.numlines
							&& i < this.model.paylines.size(); i++) {
						p = this.model.paylines.get(i);
						winsequence = "";
						winsequence += symbolset[REEL_ONE][p.getR1()];
						winsequence += symbolset[REEL_TWO][p.getR2()];
						winsequence += symbolset[REEL_THREE][p.getR3()];
						winsequence += symbolset[REEL_FOUR][p.getR4()];
						winsequence += symbolset[REEL_FIVE][p.getR5()];
	
						calculatePayoutBase(winsequence, i);
					}
				}
			} else {
					r.setBonusSpin(true);
			}
			
		}

		private void calculateScatterPayout() {
			String winsequence = "";
			// Check if there's a scatter win in base mode
			if (!this.model.bonusactive) {
				int num = findScatterSymbols(this.model.scatter_symbol);
				
				if (num > 1) {
					int scatterwin = this.model.basescatterpaytable.get(num - 1);
					winsequence = buildWinsequence(this.model.scatter_symbol, num);
					
					this.model.incrementBaseHit(winsequence, scatterwin);
					scatterwin *= r.linebet * r.numlines;
					r.setScatter(scatterwin);
					r.setCreditsWon(r.getCreditsWon() + scatterwin);
				}
				
			} else { 
				for (Symbol s : this.model.symbols) {
					int num = 0;
					int scatterwin = 0;
					
					if (s.getType() != SymbolType.WBBONUS) {
						num = findScatterSymbols(s.getAlias());
						String type = s.getType().toString();
						if (num > 0) {
							scatterwin = this.model.getBonusPayout(s.getAlias(), num - 1);
							
							switch(type) {
							// Bonus Scatter Symbol win
							case "SCATTER":
								if (scatterwin > 0) {
									winsequence = buildWinsequence(s.getAlias(), num);
									this.model.incrementBonusHit(winsequence, scatterwin);
									scatterwin *= r.linebet * r.maxlines;
									r.setScatter(scatterwin);
									r.setCreditsWon(r.getCreditsWon() + scatterwin);
								}
								break;
						
							// Bonus Free Storm Scatter win
							case "BONUS":					
								if (scatterwin > 0) {
									winsequence = buildWinsequence(s.getAlias(), num);
									this.model.incrementBonusHit(winsequence, scatterwin);
									short freespins = this.getAwardedSpins(scatterwin);
									scatterwin *= r.linebet;
									r.setCreditsWon(r.getCreditsWon() + scatterwin);
									r.addFreeSpinsAwarded(freespins);
									r.setBonusActivated(true);
								}
								break;
							// Bonus scatter win of all the other symbols
							default:
								if (scatterwin > 0) {
									winsequence = buildWinsequence(s.getAlias(), num);
									this.model.incrementBonusHit(winsequence, scatterwin);
									scatterwin *= r.linebet;
									//r.setScatter(scatterwin);
									r.setCreditsWon(r.getCreditsWon() + scatterwin);
								}
								break;
							}
						}
					}
					
				}
			}
		}
		
		// Note: "symbol" can be any symbol except for the Weather Beacon symbol
		private String buildWinsequence(String symbol, int num) {
			String sequence =  "";
			
			for (int i=0; i<num; i++) 
				sequence += symbol;
			while (sequence.length() < 5)
				sequence += "#";
			return sequence;
		}

		private int findScatterSymbols(String symbol) {
			int numofsymbols = 0;
			ArrayList<String> symbollist = this.convertSymbolSet();
			
			for (String s : symbollist) {
				if (s.equalsIgnoreCase(symbol)) 
					numofsymbols += 1;
			}
			
			return numofsymbols;
		}
		
		private void calculateWBBonusPayout() {
			Payline p = null;
			String winsequence = "";
			boolean match;
			
			// WARNING: Only the first 3 paylines where WBBonus could occur are checked.
			//		 Hence, changing the order of the paylines defined in xml file will cause the
			//       following code to fail.
			for (int i = 0; i < 3; i++) {
				p = this.model.paylines.get(i);
				
				winsequence += symbolset[REEL_ONE][p.getR1()];
				winsequence += symbolset[REEL_TWO][p.getR2()];
				winsequence += symbolset[REEL_THREE][p.getR3()];
				winsequence += symbolset[REEL_FOUR][p.getR4()];
				winsequence += symbolset[REEL_FIVE][p.getR5()];
				
				match = checkSequence(winsequence, this.model.wbbonus_sequence);
				if (match) {
					int slice = random.nextInt(25);
					int wbbmultiplier = lookUpWBBonusPaytable(slice);
					int creditswon = getWBBonusCredit(wbbmultiplier);
					
					if (!this.model.bonusactive)
						this.model.incrementBaseHit(wbbonus_sequence, wbbmultiplier);
					else 
						this.model.incrementBonusHit(wbbonus_sequence, wbbmultiplier);
					
					r.addWBBonusCreditWin(i, creditswon);
					r.setCreditsWon(r.getCreditsWon() + creditswon);
					r.incrementLinesWon();
				}
				
				winsequence = "";
			}
		}

		private void calculatePayoutBase(String winsequence, int line) {
			SimpleResult sr = new SimpleResult();
			PaytableEntry pe;
			
			for (int i = 0; i < this.model.basepaytable.size(); i++) {
				pe = this.model.basepaytable.get(i);
				
				if (pe.getType() != WinType.SCATTER) {
					calculateSimpleResult(pe, winsequence, sr);
				}
			}

			updateResult(sr, line);
		}

		// This method updates the result of one spin when each pay/played line is checked
		private void updateResult(SimpleResult sr, int line) {
			int creditswon = sr.bestBasicPayout * r.getLineBet();
			
			// if the simpleResult triggers the bonus mode
			if (sr.activatedBonus) {
				r.setBonusActivated(true);
				if (sr.bestFreeStormPayout > 0) {
					creditswon += sr.bestFreeStormPayout;
					r.addFreeStormWinAmount(line, getAwardedSpins(sr.bestFreeStormPayout));
					r.addFreeSpinsAwarded(getAwardedSpins(sr.bestFreeStormPayout));
				} 
			}
			
			// if the simpleResult contains a win of any type, increment the lines won on this spin by 1
			if ((sr.bestBasicPayout > 0 || sr.activatedBonus) && !sr.wbBonusWon) 
				r.incrementLinesWon();
			
			// increment the hit counts if there's a win of basic/bonus type
			if (sr.bestBasicPayout > 0) 
				this.model.incrementBaseHit(sr.winsequence, sr.bestBasicPayout);
			else if (sr.activatedBonus) 
				this.model.incrementBaseHit(sr.winsequence, sr.bestFreeStormPayout);
			
				
			// add the win to a particular line.
			r.addLineCreditWinAmount(line, creditswon);
			
			// update the wins of the current spin.
			r.setCreditsWon(r.getCreditsWon() + creditswon);
		}
		
		// Used to determine the payout on Base and FSS Bonus wins in Base mode
		private void calculateSimpleResult(PaytableEntry pe,
				String winsequence, SimpleResult simpleResult) {
			boolean match = true;
			String sequence = pe.getSequence();
			
			// check if the win sequence matches the paytable sequence
			match = checkSequence(winsequence, sequence);

			// if it's a match, determine the type of win and update SimpleResult accordingly.
			if (match) {
				if (pe.getType() == WinType.BASIC) {
					if (pe.getPayout() >= simpleResult.bestBasicPayout) {
						simpleResult.bestBasicPayout = pe.getPayout();
						simpleResult.winsequence = sequence;
					}
					
				} else if (pe.getType() == WinType.BONUS) { 
					simpleResult.activatedBonus = true;
					if (pe.getPayout() >= simpleResult.bestFreeStormPayout) {
						simpleResult.bestFreeStormPayout = pe.getPayout();
						simpleResult.winsequence = sequence;
					}
					
				} else if (pe.getType() == WinType.WBBONUS) {
					simpleResult.wbBonusWon = true;
				}
			}
			
			//if not a match, SimpleResult will have every win amount set to defaults as 0s for next played line.
		}

		private boolean checkSequence(String winsequence, String sequence) {
			boolean match = true;
			
			for (int i = 0; i < sequence.length(); i++) {
				if (i >= winsequence.length()) {
					match = false;
					break;
				}

				if (sequence.charAt(i) != '#') {
					if (sequence.charAt(i) != winsequence.charAt(i)) {
						match = false;
						break;
					}
				}
			}
			return match;
		}
		
		// Used to determine the amount of free spins awarded.
		private short getAwardedSpins(int payout) {
			return this.model.freespin_lookuptable.get(payout).shortValue();
		}
		
		private int getWBBonusCredit(int multiplier) {
			int creditwon = 0;
			
			// if in Base mode
			if (!this.model.bonusactive) {
				creditwon = multiplier * r.getLineBet() * r.getNumLines();
			// if in FreeStorm Scatter Bonus mode
			// NOTE: For the simulation purpose, the line bet is always constant, 
			//		 so no need to keep track of the FSSBonus mode initiating line bet. 
			} else {
				creditwon = multiplier * r.getLineBet() * this.model.paylines.size();
			}
			
			return creditwon;
		}
		
		private int lookUpWBBonusPaytable(int index) {
			return this.model.WBBonusPayTable[index];
		}
		
		// the result on one payline/played line of one single spin
		class SimpleResult {

			protected int bestBasicPayout = 0;
			protected boolean activatedBonus = false;
			protected int bestFreeStormPayout = 0;
			protected boolean wbBonusWon = false;
			protected String winsequence = "";
			
			public SimpleResult() {

			}

		}

	}

	public class PrizeSizeEntry {
		private Block currblock = null;
		private int wins = 0;
		private int losses = 0;
		private int ldws = 0;
		private int freespins = 0;
		private double wager = 0;
		private int numspins = 0;
		private double bonuspayout = 0;
		
		private int multiwins = 0;;
		
		private ArrayList<Range> prizeranges = new ArrayList<Range>();
		private ArrayList<Integer> prizesizes = new ArrayList<Integer>();
		
		public PrizeSizeEntry() {
			this.currblock = ResultsModel.this.currblock;
			
			if (currblock != null) {
				this.wager = currblock.getLineBet() * currblock.getNumLines() 
						* currblock.getFormattedDenomination();
				this.numspins = currblock.numspins;
				
				this.prizeranges.add(new Range(0, 1));
				this.prizeranges.add(new Range(1, 2));
				this.prizeranges.add(new Range(2, 5));
				this.prizeranges.add(new Range(5, 10));
				this.prizeranges.add(new Range(10, 20));
				this.prizeranges.add(new Range(20, 50));
				this.prizeranges.add(new Range(50, 100));
				this.prizeranges.add(new Range(100, 320));
				this.prizeranges.add(new Range(320, 800));
				this.prizeranges.add(new Range(800, 800));
				
				for (int i = 0; i < prizeranges.size(); i++)
					this.prizesizes.add(0);
			}
		}
		
		public Block getCurrBlock() {
			return currblock;
		}
		
		public int getWins() {
			return wins;
		}
		
		public void incrementWins() {
			wins++;
		}
		
		public int getLosses() {
			return losses;
		}
		
		public void incrementLosses() {
			losses++;
		}
		
		public int getLdws() {
			return ldws;
		}
		
		public void incrementLdws() {
			ldws++;
		}

		public int getFreeSpins() {
			return freespins;
		}
		
		public void incrementFreeSpins() {
			freespins++;
		}
		
		public int getNumSpins() {
			return numspins;
		}
		
		public int getMultiWins() {
			return this.multiwins;
		}
		
		public void incrementMultiWins() {
			this.multiwins++;
		}
		
		public ArrayList<Range> getPrizeRanges() {
			return prizeranges;
		}

		public Range getPrizeRange(int index) {
			return this.prizeranges.get(index);
		}
		
		public void setPrizeRange(int index, Range value) {
			this.prizeranges.set(index, value);
		}

		public ArrayList<Integer> getPrizeSizes() {
			return prizesizes;
		}

		public int getPrizeSize(int index) {
			return prizesizes.get(index);
		}
		
		public void incrementPrizeSizes(int index) {
			this.prizesizes.set(index, prizesizes.get(index) + 1);
		}
		
		public void updatePrizeSizeEntry(Result r) {
			double payout = r.creditswon * this.currblock.getFormattedDenomination();
			double prizesize = payout / this.wager;
			
			// If just come out of the bonus mode
			if (!ResultsModel.this.bonusactive && bonuspayout != 0) {
				// Update the bonus initiating spin in the spin ranges
				double prizesizebonus = this.bonuspayout / this.wager;
				for (int i = 0; i < prizeranges.size(); i++) {
					if (prizeranges.get(i).isPrizeSizeInRange(prizesizebonus)) {
						this.incrementPrizeSizes(i);
						break;
					}
				}
				
				this.bonuspayout = 0;
				
				// Update the current spins result in the prize range
				if (r.bonusactivated) {
					this.bonuspayout += payout;
					this.incrementWins();
					this.updateMultiWins(r);
				} else if (r.creditswon > 0)
					this.updateWin(r, payout, prizesize);
				else 
					this.incrementLosses();
				
			// If bonus mode is initially activated
			} else if (r.bonusactivated && !r.bonusspin) {
				this.bonuspayout += payout;
				this.incrementWins();
				this.updateMultiWins(r);
			
			// If in bonus mode
			} else if (ResultsModel.this.bonusactive) {
				this.incrementFreeSpins();
				this.bonuspayout += payout;
	
			// If there is a win in regular mode
			} else if (r.creditswon > 0) {
				this.updateWin(r, payout, prizesize);
			// If it is a loss in regular mode
			} else {
				this.incrementLosses();
			}
			
			if (ResultsModel.this.blockComplete) {
				try {
					Database.insertIntoTable(ResultsModel.this.getPrizeSizesDBTableName(), 
							ResultsModel.this.currpze, ResultsModel.this.blocks.size());
					ResultsModel.this.currpze = new PrizeSizeEntry();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		private void updateMultiWins(Result r) {
			int wbbonuswin = 0;
			boolean isMultiWbbWin = false;
			
			for (int i : r.getWBBonusCreditWin()) {
				if (i > 0) {
					wbbonuswin = i;
					break;
				}
			}
			
			// isMultiWbbWin is true if there is a regular win occurs on the same line as the weather beacon bonus win
			if (wbbonuswin > 0)
				isMultiWbbWin = (r.creditswon - r.scatter - wbbonuswin) > 0;
			
			// If there are multi-regular/wbbonus/fsbonus wins or some regular/wbbonus/fsbonus wins + scatter wins or a multiwbb win
			if (r.lineswon > 1 || (r.lineswon > 0 && r.scatter > 0) || isMultiWbbWin) 
				this.multiwins++;
		}
		
		private void updateWin(Result r, double payout, double prizesize) {
			updateMultiWins(r);
			
			if ((payout - this.wager) < 0) {
				this.incrementLdws();
				this.incrementPrizeSizes(0);
			// If the win is a real win
			} else {
				this.incrementWins();
				
				for (int i = 1; i < prizeranges.size(); i++) {
					if (prizeranges.get(i).isPrizeSizeInRange(prizesize)) {
						this.incrementPrizeSizes(i);
						break;
					}
				}
			}
		}
	}
	
	// GamblersRuinEntry for each Gamblers Ruin block
	public class GamblersRuinEntry {
		private int wins = 0;
		private int ldws = 0;
		private int losses = 0;
		
		private int totalspins = 0;
		private int numspins = 0;
		private int numfreespins = 0;
		private int numbonusactivation = 0;
		private long blocknum = 0;
		private short numline = 0;
		
		private int spinmedian = 0;
		private double pbmedian = 0;
		private int spinavg = 0;
		private double pbavg = 0;
		private int maxspins = 0;
		private double maxpb = 0;
		private int sdspins = 0;
		private double sdpb = 0;
		
		private double currpeakbalance = 0;
		
		private List<Range> spinranges = new ArrayList<Range>();
		private List<Integer> spins = new ArrayList<Integer>();
		
		private List<Range> peakbalanceranges = new ArrayList<Range>();
		private List<Integer> peakbalances = new ArrayList<Integer>();
		
		private List<Integer> allspins = new ArrayList<Integer>();
		private List<Double> allpbs = new ArrayList<Double>();
		
		public GamblersRuinEntry() {
			currpeakbalance = (double)ResultsModel.this.currgrblock.getBankRoll();
			this.blocknum = ResultsModel.this.currgrblock.getBlockNum();
			this.numline = ResultsModel.this.currgrblock.getNumLine();
			
			// Initialize the ranges
			int low = 0;
			int increment = 600;
			for (int i = 0; i < 10; i++) {
				int high = low + increment;
				Range r = new Range(low, high);
				low = high;
				spinranges.add(i, r);
			}
			increment = 6000;
			for (int i = 10; i < 14; i++) {
				int high = low + increment;
				Range r = new Range(low, high);
				low = high;
				spinranges.add(i, r);
			}
			spinranges.add(14, new Range(low, low));
			
			
			low = 100;
			increment = 100;
			peakbalanceranges.add(0, new Range(low, low));
			for (int i = 1; i < 10; i++) {
				int high = low + increment;
				Range r = new Range(low, high);
				low = high;
				peakbalanceranges.add(i, r);
			}
			increment = 1000;
			for (int i = 10; i < 14; i++) {
				int high = low + increment;
				Range r = new Range(low, high);
				low = high;
				peakbalanceranges.add(i, r);
			}
			peakbalanceranges.add(14, new Range(low, low));
			
			// Initialize the array lists
			for (int i = 0; i < spinranges.size(); i++)
				this.spins.add(0);
			
			for (int i = 0; i < peakbalanceranges.size(); i++)
				this.peakbalances.add(0);
			
		}
		
		public void updateGRE() {
			this.numspins = ResultsModel.this.currgrblock.getCurrSpin() + 1;
			this.totalspins += numspins;
			
			// Update Num of spin ranges
			for (int i = 0; i < spinranges.size(); i++) {
				if (spinranges.get(i).isTotalSpinCountInRange(numspins)) {
					this.incrementNumSpins(i);
					break;
				}
			}
			
			// Update Peak balance ranges
			for (int i = 0; i < peakbalanceranges.size(); i++) {
				if (peakbalanceranges.get(i).isPeakBalanceInRange(currpeakbalance)) {
					this.incrementPeakBalances(i);
					break;
				}
			}
			
			if (ResultsModel.this.repeatComplete) {
				this.allspins.add(this.numspins);
				this.allpbs.add(this.currpeakbalance);
				ResultsModel.this.repeatComplete = false;
			}
			
			if (ResultsModel.this.blockComplete) {
				this.allspins.add(this.numspins);
				this.allpbs.add(this.currpeakbalance);
				this.calculateMedians();
				this.calculateAverages();
				this.calculateSD();
				this.updateMax();
				ResultsModel.this.blockComplete = false;
			}
			
		}
		
		private void calculateMedians() {
			Collections.sort(allspins);
			Collections.sort(allpbs);
			
			int mid_spins = allspins.size() / 2;
			int mid_pbs = allpbs.size() / 2;
			
			this.spinmedian = (allspins.size() % 2 == 1) ? allspins.get(mid_spins)
						: (allspins.get(mid_spins - 1) + (allspins.get(mid_spins))) / 2;
			
			this.pbmedian = (allpbs.size() % 2 == 1) ? allpbs.get(mid_pbs)
						: (allpbs.get(mid_pbs - 1) + allpbs.get(mid_pbs)) / 2.0;	
		}
		
		private void calculateSD() {
			int sdspins = 0;
			double sdpb = 0;
			
			// Calculate SD for spins
			for (int i : this.allspins)
				sdspins += (this.spinavg - i) * (this.spinavg - i);
			
			sdspins /= this.allspins.size();
			this.sdspins = (int) Math.sqrt(sdspins);
			
			// Calculate SD for peak balances
			for (double d : this.allpbs)
				sdpb += (this.pbavg - d) * (this.pbavg - d);
			
			sdpb /= this.allpbs.size();
			this.sdpb = (int) Math.sqrt(sdpb);
			
		}
		
		private void calculateAverages() {
			int temp = 0;
			double temp2 = 0;
			
			for (int i : allspins)
				temp += i;
			this.spinavg = temp / allspins.size();
			
			for (double d : allpbs)
				temp2 += d;
			this.pbavg = temp2 / allpbs.size();
		}
		
		private void updateMax() {
			this.maxspins = Collections.max(allspins);
			this.maxpb = Collections.max(allpbs);
		}
		
		// Called at the end of each grblock
		public void reset() {
			if (ResultsModel.this.currblockindex < ResultsModel.this.grblocks.size()) {
				this.currpeakbalance = (double)ResultsModel.this.currgrblock.getBankRoll();
				this.blocknum = ResultsModel.this.currgrblock.getBlockNum();
				this.numline = ResultsModel.this.currgrblock.getNumLine();
				this.ldws = 0;
				this.wins = 0;
				this.losses = 0;
				this.numfreespins = 0;
				this.numbonusactivation = 0;
				this.numspins = 0;
				this.totalspins = 0;
				
				this.spinmedian = 0;
				this.pbmedian = 0;
				this.spinavg = 0;
				this.pbavg = 0;
				this.maxspins = 0;
				this.maxpb = 0;
				this.sdspins = 0;
				this.sdpb = 0;
				
				this.allpbs.clear();
				this.allspins.clear();
				
				for (int i = 0; i < this.spins.size(); i++)
					spins.set(i, 0);
				
				for (int i = 0; i < this.peakbalances.size(); i++)
					peakbalances.set(i, 0);
			}
		}
		
		// Called at the end of each block repeat
		public void resetCurrPeakBalance() {
			currpeakbalance = (double)ResultsModel.this.currgrblock.getBankRoll();
		}

		public int getWins() {
			return wins;
		}
		
		public void incrementWins() {
			this.wins++;
		}
		
		public int getLdws() {
			return ldws;
		}
		
		public void incrementLdws() {
			this.ldws++;
		}
		
		public int getLosses() {
			return losses;
		}
		
		public void incrementLosses() {
			this.losses++;
		}

		public int getTotalSpins() {
			return totalspins;
		}
		
		public int getNumSpins() {
			return numspins;
		}
		
		public short getNumLine() {
			return numline;
		}

		public int getNumFreeSpins() {
			return numfreespins;
		}
		
		public long getBlockNum() {
			return blocknum;
		}

		public List<Range> getSpinRanges() {
			return this.spinranges;
		}
		
		public void setNumFreeSpins(int value) {
			this.numfreespins = value;
		}
		
		public void addNumFreeSpins(int value) {
			this.numfreespins += value;
		}

		public int getNumBonusActivation() {
			return numbonusactivation;
		}
		
		public void incrementNumBonusActivation() {
			this.numbonusactivation++;
		}

		public Range getSpinRange(int index) {
			return spinranges.get(index);
		}

		public int getSpins(int index) {
			return spins.get(index);
		}
		
		public int getSpinMedian() {
			return spinmedian;
		}
		
		public double getPeakBalanceMedian() {
			return pbmedian;
		}

		public int getMaxSpins() {
			return maxspins;
		}
		
		public double getMaxPeakBalance() {
			return maxpb;
		}
		
		public int getAvgSpins() {
			return spinavg;
		}
		
		public double getAvgPeakBalance() {
			return pbavg;
		}
		
		public int getSDSpins() {
			return sdspins;
		}
		
		public double getSDPeakBalance() {
			return sdpb;
		}
		
		public void incrementNumSpins(int index) {
			this.spins.set(index, spins.get(index) + 1);
		}

		public List<Range> getPeakBalanceRanges() {
			return peakbalanceranges;
		}
		
		public Range getPeakBalanceRange(int index) {
			return peakbalanceranges.get(index);
		}

		public int getPeakBalance(int index) {
			return peakbalances.get(index);
		}

		public void incrementPeakBalances(int index) {
			this.peakbalances.set(index, peakbalances.get(index) + 1);
		}

		public double getCurrPeakBalance() {
			return currpeakbalance;
		}

		public void setCurrPeakBalance(double currpeakbalance) {
			this.currpeakbalance = currpeakbalance;
		}

	}

	// ForcedFreeSpinEntry for each FFS block
	public class ForcedFreeSpinEntry {
		private long blocknum = 0;
		private short inifreespins = 0;
		private int totalspins = 0;
		private int totalcreditswon = 0;
		private int currcreditswon = 0;
		private int currspins = 0;
		private int maxspins = 0;
		private int maxcreditswon = 0;
		private int spinmedian = 0;
		private int creditswonmedian = 0;
		private int lossspins = 0;
		private List<Integer> bonusretriggers = new ArrayList<Integer>();
		
		private List<Integer> spins = new ArrayList<Integer>();
		private List<Integer> creditswons = new ArrayList<Integer>();
		
		public ForcedFreeSpinEntry() {
			blocknum = ResultsModel.this.currblock.getBlockNumber();
			inifreespins = (short)ResultsModel.this.currblock.getNumSpins();
			
			for (int i = 0; i < 3; i++) 
				bonusretriggers.add(0);
			
		}
		
		public long getBlockNum() {
			return blocknum;
		}
		
		public short getInitialFreeSpins() {
			return inifreespins;
		}
		
		public int getTotalSpins() {
			return totalspins;
		}
		
		public int getTotalCreditsWon() {
			return totalcreditswon;
		}
		
		public int getMaxSpins() {
			return maxspins;
		}
		
		public int getMaxCreditsWon() {
			return maxcreditswon;
		}
		
		public int getSpinMedian() {
			return spinmedian;
		}
		
		public int getCreditsWonMedian() {
			return creditswonmedian;
		}
		
		public int getLossSpins() {
			return lossspins;
		}
		
		public List<Integer> getBonusRetriggers() {
			return bonusretriggers;
		}
		
		public void incrementBonusRetriggers(int index) {
			this.bonusretriggers.set(index, bonusretriggers.get(index) + 1);
		}
		
		public void incrementCurrSpin() {
			this.currspins++;
		}
		
		public void updateFFSE(Result r) {
			// Update bonus retriggering info
			if (r.bonusactivated && r.bonusspin) {
				switch (r.freespinsawarded) {
				case 3: 
					incrementBonusRetriggers(0);
					break;
				case 10:
					incrementBonusRetriggers(1);
					break;
				case 15:
					incrementBonusRetriggers(2);
					break;
				default:
					break;
				}
			}
			
			// Update credits won
		
			if (r.creditswon == 0)
				this.lossspins++;
			else {
				if (this.currspins != 1)
						this.currcreditswon += r.creditswon;
			}
			
			
			// If a block repeat is completed
			if (ResultsModel.this.repeatComplete) {
				this.spins.add(currspins - 1); //-1 because we don't count the bonus initialization spin
				this.creditswons.add(currcreditswon);
				this.totalspins += currspins - 1;
				this.totalcreditswon += currcreditswon;
				
				// Reset things
				ResultsModel.this.currblock.currspin = 0;
				currspins = 0;
				this.currcreditswon = 0;
				ResultsModel.this.repeatComplete = false;
			}
			
			// If a block is completed
			if (ResultsModel.this.blockComplete) {
				this.spins.add(currspins - 1); 
				this.creditswons.add(currcreditswon);
				this.totalspins += currspins - 1;
				this.totalcreditswon += currcreditswon;
				
				this.maxspins = Collections.max(this.spins);
				this.maxcreditswon = Collections.max(this.creditswons);
				this.calculateMedians();
				ResultsModel.this.blockComplete = false;
				
				try {
					Database.insertIntoTable(ResultsModel.this.getForcedFreeSpinsDBTableName(), 
							this, ResultsModel.this.ffsblocks.size());
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (ResultsModel.this.currblock != null)
					ResultsModel.this.currffs = new ForcedFreeSpinEntry();
				else
					ResultsModel.this.currffs = null;
			}

		}
		
		private void calculateMedians() {
			Collections.sort(spins);
			Collections.sort(creditswons);
			
			int mid_spins = spins.size() / 2;
			int mid_creditswon = creditswons.size() / 2;
			
			this.spinmedian = (spins.size() % 2 == 1) ? spins.get(mid_spins)
						: (spins.get(mid_spins - 1) + (spins.get(mid_spins))) / 2;
			
			this.creditswonmedian = (creditswons.size() % 2 == 1) ? creditswons.get(mid_creditswon)
						: (creditswons.get(mid_creditswon - 1) + creditswons.get(mid_creditswon)) / 2;	
		}
	}
	
	public class Range {
		public double low = 0;
		public double high = 0;
		
		public Range (double l, double h) {
			this.low = l;
			this.high = h;
		}
		
		public boolean isLossPercentageInRange(double percentage) {
			// when loss percentage is [-9, infinity)
			if (low == -9 && high == -9)
				return (percentage <= low);
			// When loss percentage is between [0, -9)
			else 
				return (percentage <= high && percentage > low);
			
			
				
		}
		
		public boolean isTotalSpinCountInRange(int count) {
			if (low == high)
				return (count >= low);
			else
				return (count < high && count >= low);
		}
		
		public boolean isPeakBalanceInRange(double balance) {
			if (high == 100)
				return (balance == high);
			else if (low == 5000)
				return (balance >= low);
			else 
				return (balance < high && balance >= low);
		}
		
		public boolean isPrizeSizeInRange(double prizewon) {
			if (low == high) 
				return (prizewon >= low);
			else 
				return (prizewon >= low && prizewon < high);
			
		}
				
	}
	
	public static DecimalFormat twoDForm = new DecimalFormat("#.##");

	public static double roundTwoDecimals(double d) {
		return Double.valueOf(twoDForm.format(d));
	}

	public static float roundTwoDecimalsFloat(float d) {
		return Float.valueOf(twoDForm.format(d));
	}

	
}
