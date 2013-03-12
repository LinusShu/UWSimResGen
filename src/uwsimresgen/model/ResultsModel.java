package uwsimresgen.model;

import java.io.File;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
	public static String BASEPAR_TABLE_NAME = "BasePARTable";
	public static String BONUSPAR_TABLE_NAME = "BonusPARTable";

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
	private ArrayList<Payline> paylines = new ArrayList<Payline>();
	private ArrayList<PaytableEntry> basepaytable = new ArrayList<PaytableEntry>();
	private ArrayList<PaytableEntry> bonuspaytable = new ArrayList<PaytableEntry>();
	private ArrayList<Integer> basescatterpaytable = new ArrayList<Integer>(); 
	private HashMap<String, ArrayList<Integer>> bonuscatterpaytable = new HashMap<String, ArrayList<Integer>>();
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
	private Block currblock = null;
	private int currblockindex = 0;
	private boolean bonusactive = false;
	private int currspin = 0;
	private int currconsumedspin = 0;
	private int failedspins = 0;
	private int freespins = 0;
	private int totalspins = 0;
	private short genallnumlines = 1;

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

	public void setGenAllNumLines(short value) {
		this.genallnumlines = value;
		this.UpdateViews();
	}

	public boolean getGenAllStops() {
		return this.simulator.getSeqStops();
	}

	public short getGenAllNumLines() {
		return this.genallnumlines;
	}

	public int getCurrSpin() {
		return this.currspin;
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
				b.setDenomination((short) 1);
				b.setNumLines(this.genallnumlines);
				b.setLineBet((short) 1);
				b.setNumSpins(this.reel1.size() * this.reel2.size()
						* this.reel3.size() * this.reel4.size()
						* this.reel5.size());
				this.blocks.add(b);
			} else {
				result2 = loadBlocks(blocksfile);
				this.outputLog.outputStringAndNewLine("Mode: Do Blocks.");
			}

			if (result2) {
				start();
				produce();
				// consume();
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

	private void produce() {
		Thread t = new Thread() {
			public void run() {
				int total_freespin = 0;
				boolean orign_mode = ResultsModel.this.simulator.getSeqStops();
				
				ResultsModel.this.outputLog
						.outputStringAndNewLine("START PRODUCTION");

				int ts = 0;

				for (Block b : ResultsModel.this.blocks) {
					ts += b.numspins;
				}

				ResultsModel.this.setTotalSpins(ts);

				ResultsModel.this.outputLog
						.outputStringAndNewLine("Total Spins: "
								+ Integer.toString(ts));

				if (ts > 0) {

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

					try {
						ResultsModel.this.currblock = ResultsModel.this.blocks
								.get(ResultsModel.this.currblockindex);

						while (ResultsModel.this.currblock != null
								&& !ResultsModel.this.cancelled) {

							if (!ResultsModel.this.paused) {

								ResultsModel.this.checkBonusState();
								Result r = ResultsModel.this.simulator
										.simulateSpin();
								if (r != null) {

									// ResultsModel.this.resultqueue.put(r);
									
									// ResultsModel.this.results.add(r);

									r.setRecordNumber(ResultsModel.this
											.getCurrSpin() + 1);
									r.setBlockNumber(ResultsModel.this.blocks
											.get(ResultsModel.this.currblockindex)
											.getBlockNumber());
									Database.insertIntoTable(ResultsModel.this
											.getSpinResultsDBTableName(), r);
									ResultsModel.this.incrementCurrSpin();
									
									// If there are free spins awarded
									if (r.freespinsawarded > 0) { 
										total_freespin += r.freespinsawarded;
									
										// Check if the number of free spins awarded exceeds the allowed
										if (total_freespin > 200) {
											int extras = 200 - (total_freespin - r.freespinsawarded);
											ResultsModel.this.addFreeSpins(extras);
										} else {
											ResultsModel.this.addFreeSpins(r.freespinsawarded);
										}
									}
									
									// Add extra spins to total spin when get free spins in Generate-all-results mode
									if (orign_mode && r.bonusactivated) {
										ts += r.freespinsawarded;
										ResultsModel.this.setTotalSpins(ts);
										ResultsModel.this.blocks.get(ResultsModel.this.currblockindex)
										.addNumSpins(r.freespinsawarded);
										ResultsModel.this.simulator.setSeqStops(false);
									}

									if (ResultsModel.this.bonusactive) {
										ResultsModel.this.decrementFreeSpins();
									} else if (orign_mode != ResultsModel.this.simulator.getSeqStops()
											&& !r.bonusactivated) {
											ResultsModel.this.simulator.setSeqStops(orign_mode);
											total_freespin = 0;
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

						Database.flushBatch();

						ResultsModel.this.stop();

					} catch (Exception e) {
						ResultsModel.this.stop();
						ResultsModel.this.outputLog
								.outputStringAndNewLine("ERROR: Production thread caught exception. Message="
										+ e.getMessage());
						e.printStackTrace();
					} finally {
						try {
							Database.shutdownConnection();
						} catch (SQLException e) {
							ResultsModel.this.outputLog
									.outputStringAndNewLine("ERROR: Closing database connection. Message="
											+ e.getMessage());
							e.printStackTrace();
						}
					}
				} else {
					ResultsModel.this.stop();
				}

				ResultsModel.this.outputLog
						.outputStringAndNewLine("END PRODUCTION");

			}
		};

		t.setPriority(Thread.MAX_PRIORITY);
		t.start();

	}

	/* CONSUMER */
	// NOT USED!!!!
//	private void consume() {
//		Thread t = new Thread() {
//			public void run() {
//
//				int timeouts = 0;
//				int timeoutlimit = 3;
//
//				try {
//					Database.createConnection();
//					while (!ResultsModel.this.isCancelled()
//							&& ResultsModel.this.isRunning()) {
//
//						Result r = resultqueue.take();
//						if (!r.getNullObject()) {
//							timeouts = 0;
//							Database.insertIntoTable(ResultsModel.this
//									.getSpinResultsDBTableName(), r);
//							ResultsModel.this.incrementConsumed();
//						} else {
//							break;
//						}
//					}
//
//					ResultsModel.this.stop();
//
//				} catch (Exception e) {
//					ResultsModel.this.cancel();
//					ResultsModel.this.outputLog
//							.outputStringAndNewLine("ERROR: Consumption thread caught an exception. Message="
//									+ e.getMessage());
//					e.printStackTrace();
//				} finally {
//					try {
//						Database.shutdownConnection();
//					} catch (SQLException e) {
//						ResultsModel.this.outputLog
//								.outputStringAndNewLine("ERROR: Closing database connection. Message="
//										+ e.getMessage());
//						e.printStackTrace();
//					}
//				}
//			}
//		};
//
//		t.setPriority(Thread.MAX_PRIORITY);
//		t.start();
//	}

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

		if (this.currblock.incrementCurrSpin()) {
			this.currblockindex++;
			if (this.currblockindex < this.blocks.size()) {
				this.currblock = this.blocks.get(currblockindex);
			} else {
				this.currblock = null;
			}
		}

		this.UpdateViews();
	}

//	protected void incrementConsumed() {
//		this.currconsumedspin++;
//		this.UpdateViews();
//	}

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
		// Read Blocks
		NodeList list = doc.getElementsByTagName("block");
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) node;

				short numlines = -1;
				short linebet = -1;
				short denomination = -1;
				int numspins = -1;

				try {
					numlines = Short.parseShort(e.getAttribute("numlines")); // Integer.parseInt(e.getAttribute("numlines"));
				} catch (NumberFormatException nfe) {
					this.addErrorToLog2("Block[" + Integer.toString(i)
							+ "] - Invalid value for 'numlines'. Value: "
							+ e.getAttribute("numlines"));
				}

				try {
					linebet = Short.parseShort(e.getAttribute("linebet")); // Integer.parseInt(e.getAttribute("linebet"));
				} catch (NumberFormatException nfe) {
					this.addErrorToLog2("Block[" + Integer.toString(i)
							+ "] - Invalid value for 'linebet'. Value: "
							+ e.getAttribute("linebet"));
				}

				try {
					denomination = Short.parseShort(e
							.getAttribute("denomination")); // Integer.parseInt(e.getAttribute("denomination"));
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

				if (numlines > 0 && linebet > 0 && denomination > 0
						&& numspins > 0) {
					Block b = new Block();
					b.setNumLines(numlines);
					b.setLineBet(linebet);
					b.setDenomination(denomination);
					b.setNumSpins(numspins);
					this.blocks.add(b);
				} else {
					this.addErrorToLog2("Block["
							+ Integer.toString(i)
							+ "] - All attributes must be integers greater than 0.");
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
					
					if (type == WinType.WBBONUS) 
						this.wbbonus_sequence = sequence;
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
					
					if (type != WinType.WBBONUS) {
						String key = String.valueOf(sequence.charAt(0));
						int numOfSymbol = this.numOfSymbol(sequence) - 1;
						
						this.bonuscatterpaytable.get(key).set(numOfSymbol, payout);
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
		private short denomination = 1;

		private int creditswon = 0;
		private long blocknumber = 0;
		private long recordNumber = 0;
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
					+ Integer.toString(linebet) + " denom="
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

		public void setLineBet(short value) {
			this.linebet = value;
		}

		public void setDenomination(short value) {
			this.denomination = value;
		}

		public void setCreditsWon(int value) {
			this.creditswon = value;
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

		public void setFreeSpinsAwarded(short value) {
			this.freespinsawarded = value;
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

		public short getDenomination() {
			return this.denomination;
		}

		public int getCreditsWon() {
			return this.creditswon;
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
		private int currspin = 0;
		private long blockNumber = 0;

		public Block() {
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

		public double getFormattedDenomination() {
			return ResultsModel.roundTwoDecimals(this.denomination / 100.0);
		}

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

				if (!seqstops) {
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
				/* populate symbol set */

// 				TEST: Cheat spin results here 				
//				reelstop1 = (short)22;
//				reelstop2 = (short)6;
//				reelstop3 = (short)5;
//				reelstop4 = (short)22;
//				reelstop5 = (short)27;
//				this.model.bonusactive = true;

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

				
				r.setNumLines(this.model.currblock.numlines);
				r.setLineBet(this.model.currblock.linebet);
				r.setDenomination(this.model.currblock.denomination);

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
			
			for (int i = 0; i < this.model.currblock.numlines
					&& i < this.model.paylines.size(); i++) {
				p = this.model.paylines.get(i);
				winsequence = "";
				winsequence += symbolset[REEL_ONE][p.getR1()];
				winsequence += symbolset[REEL_TWO][p.getR2()];
				winsequence += symbolset[REEL_THREE][p.getR3()];
				winsequence += symbolset[REEL_FOUR][p.getR4()];
				winsequence += symbolset[REEL_FIVE][p.getR5()];

				if (!this.model.bonusactive) {
					calculatePayoutBase(winsequence, i);
				} else {
					calculatePayoutBonus(winsequence, i);
				}
			}
		}

		private void calculateScatterPayout() {
			// Check if there's a scatter win in base mode
			if (!this.model.bonusactive) {
				int num = findScatterSymbols(this.model.scatter_symbol);
				
				if (num > 1 && num <= 5) {
					int scatterwin = this.model.basescatterpaytable.get(num - 1);
					scatterwin *= currblock.linebet * currblock.numlines;
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
							switch(type) {
							// Bonus Scatter Symbol win
							case "SCATTER":
								scatterwin = this.model.getBonusPayout(s.getAlias(), num - 1);
								scatterwin *= currblock.linebet * r.maxlines;
								r.setScatter(scatterwin);
								r.setCreditsWon(r.getCreditsWon() + scatterwin);
								break;
						
							// Bonus Free Storm Scatter win
							case "BONUS":
								if (num > 2) {
									scatterwin = this.model.getBonusPayout(s.getAlias(), num - 1);
									short freespins = this.getAwardedSpins(scatterwin);
									r.setCreditsWon(r.getCreditsWon() + scatterwin);
									r.setFreeSpinsAwarded(freespins);
									r.setBonusActivated(true);
								}
								break;
							// Bonus scatter win of all the other symbols
							default:
								scatterwin = this.model.getBonusPayout(s.getAlias(), num - 1);
								//r.setScatter(scatterwin);
								r.setCreditsWon(r.getCreditsWon() + scatterwin);
								break;
							}
						}
					}
					
				}
			}
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
				
				if (checkSequence(winsequence, this.model.wbbonus_sequence)) {
					int slice = random.nextInt(25);
					int wbbmultiplier = lookUpWBBonusPaytable(slice);
					int creditswon = getWBBonusCredit(wbbmultiplier);
					
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

		private void calculatePayoutBonus(String winsequence, int line) {
//			SimpleResult sr = new SimpleResult();
//			PaytableEntry pe;
//			
//			for (int i = 0; i < this.model.bonuspaytable.size(); i++) {
//				pe = this.model.bonuspaytable.get(i);
//				// Only calculate Weather Beacon Bonus as in base mode
//				if (pe.getType() == WinType.WBBONUS)
//					calculateSimpleResult(pe, winsequence, sr);
//			}

			r.setBonusSpin(true);
//			updateResult(sr, line);
		}

		// This method updates the result of one spin when each payline/played line is checked
		private void updateResult(SimpleResult sr, int line) {
			int creditswon = (sr.bestPayout);
			
			// if the simpleResult triggers the bonus mode
			if (sr.activatedBonus) {
				r.setBonusActivated(true);
				if (sr.bestFreeStormPayout > 0) {
					creditswon += sr.bestFreeStormPayout;
					r.addFreeStormWinAmount(line, sr.bestFreeStormPayout);
					r.setFreeSpinsAwarded(getAwardedSpins(sr.bestFreeStormPayout));
				} 
			}
//			// if the simpleResult has a Scatter win
//			if (sr.bestScatterPayout > 0)
//				r.setScatter(true);
			
//			 if the simpleResult has a Weather Beacon Bonus win
//			if (sr.wbBonusMultiplier > 0 && line < 3) {
//				int wbcreditswon = this.getWBBonusCredit(sr, r);
//				
//				creditswon += wbcreditswon;
//				r.addWBBonusCreditWin(line, wbcreditswon);
//			}
				
			// if the simpleResult contains a win of any type, increment the lines won on this spin by 1
			if ((sr.bestPayout > 0 || sr.activatedBonus) && !sr.wbBonusWon)
				r.incrementLinesWon();
				
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
//			int slice = random.nextInt(25);
			
			// check if the win sequence matches the paytable sequence
			match = checkSequence(winsequence, sequence);

			// if it's a match, determine the type of win and update SimpleResult accordingly.
			if (match) {
				if (pe.getType() == WinType.BASIC) {
					if (pe.getPayout() >= simpleResult.bestPayout) {
						simpleResult.bestPayout = pe.getPayout();
					}
//				} else if (pe.getType() == WinType.SCATTER) {
//					if (pe.getPayout() >= simpleResult.bestScatterPayout) {
//						simpleResult.bestScatterPayout = pe.getPayout();
//					}
					
				} else if (pe.getType() == WinType.BONUS) { 
					simpleResult.activatedBonus = true;
					if (pe.getPayout() >= simpleResult.bestFreeStormPayout) {
						simpleResult.bestFreeStormPayout = pe.getPayout();
					}
				} else if (pe.getType() == WinType.WBBONUS) {
					simpleResult.wbBonusWon = true;
				}
			}
			
			//if not a match, SimpleResult will have every win amount set to defaults as 0s.
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

			protected int bestPayout = 0;
			protected int bestScatterPayout = 0;
			protected boolean activatedBonus = false;
			protected int bestFreeStormPayout = 0;
			protected boolean wbBonusWon = false;
			
			public SimpleResult() {

			}

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
