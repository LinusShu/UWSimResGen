package DB;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import uwsimresgen.model.ResultsModel;
import uwsimresgen.model.ResultsModel.BasicInfoEntry;
import uwsimresgen.model.ResultsModel.BettingStrategyEntry;
import uwsimresgen.model.ResultsModel.Block;
import uwsimresgen.model.ResultsModel.ForcedFreeSpinEntry;
import uwsimresgen.model.ResultsModel.GamblersRuinEntry;
import uwsimresgen.model.ResultsModel.LossPercentageEntry;
import uwsimresgen.model.ResultsModel.Payline;
import uwsimresgen.model.ResultsModel.PaytableEntry;
import uwsimresgen.model.ResultsModel.PrizeSizeEntry;
import uwsimresgen.model.ResultsModel.Range;
import uwsimresgen.model.ResultsModel.Result;
import uwsimresgen.model.ResultsModel.StreaksEntry;
import uwsimresgen.model.ResultsModel.Symbol;
import uwsimresgen.model.ResultsModel.WinType;

public class Database {

	public static String DEFAULT_DB_NAME = "MoneyStormDB";
	public static String DP_DB_NAME = "DolphinTreasureDB";
	public static String SOS_DB_NAME = "SandsofSplendorDB";
	
	// Change me to modify the database name.
	private static String dbName = DEFAULT_DB_NAME;
	private static String URL = "jdbc:derby:" + dbName + ";create=true";
	private static final String URL_SHUTDOWN = "jdbc:derby:;shutdown=true";

	private static Connection conn = null;
	
	private static ArrayList<String> listOfTables;
	
	@SuppressWarnings("serial")
	private static final HashMap<String, String> PAR_MAPPING = new HashMap<String, String>() {
		{
			put("A", "F4");
			put("B", "F3");
			put("C", "F2");
			put("D", "F1");
			put("E", "M5");
			put("F", "M4");
			put("G", "M3");
			put("H", "M2");
			put("I", "M1");
			put("J", "LO");
			put("S", "B1");
			put("W", "B2");
			put("Z", "B3");
		};
	};

	private static int maxLines;
	private static ResultsModel.Mode mode;
	
	static PreparedStatement st = null;
	static PreparedStatement lps = null;
	static PreparedStatement grs = null;
	static PreparedStatement pss = null;
	static PreparedStatement ffss = null;
	static PreparedStatement soss = null;
	
	static int batchRequests = 0;
	static int lpeBatchRequests = 0;
	static int greBatchRequests = 0;
	static int pseBatchRequests = 0;
	static int ffseBatchRequests = 0;
	static int sosBatchRequests = 0;
	
	public static void setMaxLines(int maxLines) {
		Database.maxLines = maxLines;
	}
	
	public static void setMode(ResultsModel.Mode value)	{
		Database.mode = value;
	}

	public static void setDbName(String value) {
		Database.dbName = value;
		Database.URL = "jdbc:derby:" + dbName + ";create=true";
	}

	public static String getDbName() {
		return dbName;
	}

	// Populates a list of table names to reference later on.
	private static void popuplateListOfTables() throws SQLException {
		listOfTables = new ArrayList<String>();
		DatabaseMetaData md = conn.getMetaData();
		ResultSet rs = md.getTables(null, null, "%", null);
		while (rs.next()) {
			// Add table name
			listOfTables.add(rs.getString(3).toUpperCase());
		}
	}

	public static void insertIntoTable(String tableName,
			PaytableEntry paytableEntry) throws SQLException {
		tableName = tableName.toUpperCase();

		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName + " "
					+ "(ENTRYID bigint NOT NULL, "
					+ "WINCODE varchar(10) NOT NULL, "
					+ "SEQUENCE varchar(10) NOT NULL, "
					+ "PAYOUT integer NOT NULL, "
					+ "TYPE varchar(20) NOT NULL, "
					+ "HITS bigint NOT NULL)";
			Database.createTable(tableName, query);
		}

		// Otherwise, add it to DB.
		String query = "insert into " + tableName
				+ "(ENTRYID,WINCODE,SEQUENCE,PAYOUT,TYPE,HITS) "
				+ "values(?,?,?,?,?,?)";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			
			if (paytableEntry.getType() == WinType.WBBONUS) {
				int[] wbbonuspay = {2, 5, 8, 10, 15, 25};
				
				for (int i=0; i < wbbonuspay.length; i++) {
					st.setLong(1, paytableEntry.getEntryID());
					st.setString(2, paytableEntry.getWinCode());
					st.setString(3, paytableEntry.getSequence());
					st.setInt(4, wbbonuspay[i]);
					st.setString(5, paytableEntry.getType().toString());
					st.setLong(6, 0);
					
					st.addBatch();
					batchRequests++;
				}
			} else {
				st.setLong(1, paytableEntry.getEntryID());
				st.setString(2, paytableEntry.getWinCode());
				st.setString(3, paytableEntry.getSequence());
				st.setInt(4, paytableEntry.getPayout());
				st.setString(5, paytableEntry.getType().toString());
				st.setLong(6, 0);
				
				st.addBatch();
				batchRequests++;
			}

			if (batchRequests >= 1000) {
				batchRequests = 0;
				st.executeBatch();
			}
		} catch (SQLException e) {
			throw e;
		}
	}

	public static void insertIntoTable(String tableName, Payline payline)
			throws SQLException {
		tableName = tableName.toUpperCase();
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName + " "
					+ "(NUMBER bigint NOT NULL, "
					+ "REEL1 varchar(10) NOT NULL, "
					+ "REEL2 varchar(10) NOT NULL, "
					+ "REEL3 varchar(10) NOT NULL, "
					+ "REEL4 varchar(10) NOT NULL, "
					+ "REEL5 varchar(10) NOT NULL)";
			Database.createTable(tableName, query);
		}

		// Otherwise, add it to DB.
		String query = "insert into " + tableName
				+ "(NUMBER,REEL1,REEL2,REEL3,REEL4,REEL5) "
				+ "values(?,?,?,?,?,?)";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			st.setLong(1, payline.getNumber());
			st.setString(2, ResultsModel.convertPaylineR(payline.getR1()));
			st.setString(3, ResultsModel.convertPaylineR(payline.getR2()));
			st.setString(4, ResultsModel.convertPaylineR(payline.getR3()));
			st.setString(5, ResultsModel.convertPaylineR(payline.getR4()));
			st.setString(6, ResultsModel.convertPaylineR(payline.getR5()));

			st.addBatch();
			batchRequests++;
			if (batchRequests >= 1000) {
				batchRequests = 0;
				st.executeBatch();
			}
		} catch (SQLException e) {
			throw e;
		}
	}

	public static void insertIntoTable(String tableName, Block block)
			throws SQLException {
		tableName = tableName.toUpperCase();
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName + " "
					+ "(BLOCKNUMBER bigint NOT NULL, "
					+ "NUMOFLINES smallint NOT NULL, "
					+ "LINEBET smallint NOT NULL, "
					+ "DENOMINATION smallint NOT NULL, "
					+ "FORMATTEDDENOMINATION float NOT NULL, "
					+ "NUMOFSPINS bigint NOT NULL, " 
					+ "REPEATS integer NOT NULL)";
			Database.createTable(tableName, query);
		}

		// Otherwise, add it to DB.
		String query = "insert into "
				+ tableName
				+ "(BLOCKNUMBER, NUMOFLINES, LINEBET, DENOMINATION, FORMATTEDDENOMINATION, NUMOFSPINS, REPEATS) "
				+ "values(?,?,?,?,?,?,?)";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			st.setLong(1, block.getBlockNumber());
			st.setShort(2, block.getNumLines());
			st.setShort(3, block.getLineBet());
			st.setShort(4, block.getDenomination());
			st.setDouble(5, block.getFormattedDenomination());
			st.setLong(6, block.getNumSpins());
			st.setInt(7, block.getNumRepeats());

			st.addBatch();
			batchRequests++;
			if (batchRequests >= 1000) {
				batchRequests = 0;
				st.executeBatch();
			}
		} catch (SQLException e) {
			throw e;
		}
	}

	public static void insertIntoTable(String tableName, Symbol symbol)
			throws SQLException {
		tableName = tableName.toUpperCase();
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName + " "
					+ "(ID bigint NOT NULL, " + "ALIAS varchar(10) NOT NULL, "
					+ "TYPE varchar(10) NOT NULL)";
			Database.createTable(tableName, query);
		}

		// Otherwise, add it to DB.
		String query = "insert into " + tableName + "(ID, ALIAS, TYPE) "
				+ "values(?,?,?)";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			st.setInt(1, symbol.getID());
			st.setString(2, symbol.getAlias());
			st.setString(3, symbol.getType().toString());

			st.addBatch();
			batchRequests++;
			if (batchRequests >= 1000) {
				batchRequests = 0;
				st.executeBatch();
			}
		} catch (SQLException e) {
			throw e;
		}
	}

	// Inserts a result into the table; if the table does not exist it will be
	// created first.
	public static void insertIntoTable(String tableName, Result result)
			throws SQLException {
		tableName = tableName.toUpperCase();
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String freeStormWin_CREDITS = "";
			String wbBonus_CREDITS = "";
			String lineWins_CREDITS = "";
			
			// Dolphin Treasure does not have WB and FSS wins
			if (mode != ResultsModel.Mode.DOLPHIN_TREASURE) {
				for (int i = 0; i < maxLines; i++) {
					freeStormWin_CREDITS += ", FREESTORM" + i + "WIN_CREDITS smallint NOT NULL";
				}
			
				for (int i=0; i < 3; i++) {
					wbBonus_CREDITS += ", WBBONUS" + i
							+ "WIN_CREDITS smallint NOT NULL";
				}
			}	
			
			for (int i = 0; i < maxLines; i++) {
				lineWins_CREDITS += ", LINE" + i
						+ "WIN_CREDITS smallint NOT NULL";
			}
			
			
			String query = "create table " + tableName + " "
					+ "(RECORDNUMBER bigint NOT NULL, "
					+ "BLOCKNUMBER bigint NOT NULL, "
					+ "REPEATNUMBER integer NOT NULL, "
					+ "REELSTOP1 smallint NOT NULL, "
					+ "REELSTOP2 smallint NOT NULL, "
					+ "REELSTOP3 smallint NOT NULL, "
					+ "REELSTOP4 smallint NOT NULL, "
					+ "REELSTOP5 smallint NOT NULL, "
					+ "NUMOFLINES smallint NOT NULL, "
					+ "LINEBET smallint NOT NULL, "
					+ "DENOMINATION float NOT NULL, "
					+ "CREDITSWON integer NOT NULL, "
					+ "LDW_WINS integer NOT NULL, "
					+ "LDW_LOSSES integer NOT NULL, "
					+ "LINESWON smallint NOT NULL, "
					+ "SCATTER integer NOT NULL, "
					+ "BONUSACTIVATED boolean NOT NULL, "
					+ "BONUSSPIN boolean NOT NULL, "
					+ "FREESPINSAWARDED smallint NOT NULL" 
					+ wbBonus_CREDITS
					+ freeStormWin_CREDITS
					+ lineWins_CREDITS + ")";
			Database.createTable(tableName, query);
		}

		String freeStormWins_Credits = "";
		String freeStormWins_CreditsQ = "";
		String wbbWins_Credits = "";
		String wbbWins_CreditsQ = "";
		
		if (mode != ResultsModel.Mode.DOLPHIN_TREASURE) {
			if (result.getFreeStormWinAmounts() != null
					&& result.getFreeStormWinAmounts().size() > 0) {
				for (int i = 0; i < result.getFreeStormWinAmounts().size(); i++) {
					freeStormWins_Credits += ", FREESTORM" + i + "WIN_CREDITS";
					freeStormWins_CreditsQ += ",?";
				}
			}
			
			if (result.getWBBonusCreditWin() != null
					&& result.getWBBonusCreditWin().size() > 0) {
				for (int i = 0; i < result.getWBBonusCreditWin().size(); i++) {
					wbbWins_Credits += ", WBBONUS" + i + "WIN_CREDITS";
					wbbWins_CreditsQ += ",?";
				}
			}
		}
		
		String lineWins_Credits = "";
		String lineWins_CreditsQ = "";
		if (result.getLineCreditWinAmounts() != null
				&& result.getLineCreditWinAmounts().size() > 0) {
			for (int i = 0; i < result.getLineCreditWinAmounts().size(); i++) {
				lineWins_Credits += ", LINE" + i + "WIN_CREDITS";
				lineWins_CreditsQ += ",?";
			}
		}
		
		// Otherwise, add it to DB.
		String query = "insert into "
				+ tableName
				+ "(RECORDNUMBER, BLOCKNUMBER, REPEATNUMBER, REELSTOP1, REELSTOP2, REELSTOP3, REELSTOP4, REELSTOP5, NUMOfLINES, " 
				+ "LINEBET, DENOMINATION, CREDITSWON, LDW_WINS, LDW_LOSSES, LINESWON, SCATTER, BONUSACTIVATED, BONUSSPIN, FREESPINSAWARDED"
				+ wbbWins_Credits  + freeStormWins_Credits + lineWins_Credits + ") "  
				+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?"
				+ wbbWins_CreditsQ + freeStormWins_CreditsQ + lineWins_CreditsQ + ")";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			st.setLong(1, result.getRecordNumber());
			st.setLong(2, result.getBlockNumber());
			st.setInt(3, result.getRepeatNumber());
			st.setShort(4, result.getReelStop1());
			st.setShort(5, result.getReelStop2());
			st.setShort(6, result.getReelStop3());
			st.setShort(7, result.getReelStop4());
			st.setShort(8, result.getReelStop5());
			st.setShort(9, result.getNumLines());
			st.setShort(10, result.getLineBet());
			st.setDouble(11, result.getFormattedDenomination());
			st.setInt(12, result.getCreditsWon());
			st.setInt(13, result.getLDWWins());
			st.setInt(14, result.getLDWLosses());
			st.setShort(15, result.getLinesWon());
			st.setInt(16, result.getScatter());
			st.setBoolean(17, result.getBonusActivated());
			st.setBoolean(18, result.getBonusSpin());
			st.setShort(19, result.getFreeSpinsAwarded());

			int startingIndex = 20;
			
			if (mode != ResultsModel.Mode.DOLPHIN_TREASURE) {
				if (result.getWBBonusCreditWin() != null
						&& result.getWBBonusCreditWin().size() > 0) {
					for (int i = 0; i < result.getWBBonusCreditWin().size(); i++) {
						st.setDouble(startingIndex, result.getWBBonusCreditWinOn(i));
						startingIndex++;
					}
				}
				
				if (result.getFreeStormWinAmounts() != null
						&& result.getFreeStormWinAmounts().size() > 0) {
					for (int i = 0; i < result.getFreeStormWinAmounts().size(); i++) {
						st.setDouble(startingIndex,
								result.getFreeStormWinAmount(i));
						startingIndex++;
					}
				}
			}
			
			if (result.getLineCreditWinAmounts() != null
					&& result.getLineCreditWinAmounts().size() > 0) {
				for (int i = 0; i < result.getLineCreditWinAmounts().size(); i++) {
					st.setDouble(startingIndex, result.getLineCreditWinAmount(i));
					startingIndex++;
				}
			}
			

			st.addBatch();
			batchRequests++;
			if (batchRequests >= 1000) {
				batchRequests = 0;
				st.executeBatch();
			}
		} catch (SQLException e) {
			throw e;
		}
	}
	
	public static void insertIntoTable(String tableName, LossPercentageEntry lpe, int blocksize) 
			throws SQLException {
		tableName = tableName.toUpperCase();
		
		String ranges = "";
		String rangesQ = "";
		String rangesI = "";
		
		String bas = "";
		String basQ = "";
		String basI = "";
		// Create table if does not exist
		if (!Database.doesTableExist(tableName)) {
			
			

			if (lpe.getRangeArray() != null) {
				for (int i = 0; i < lpe.getRangeArray().size(); i++) {
					Range r = lpe.getRangeArray().get(i);
					
					if (i < 4) {
						ranges += ", LOSS" + (int)(r.high * 100) + "_" + (int)(r.low * 100) + " integer NOT NULL";
						rangesI += ", LOSS" + (int)(r.high * 100) + "_" + (int)(r.low * 100);
						
						bas += ", BA" + (int)(r.high * 100) + "_" + (int)(r.low * 100) + " integer NOT NULL";
						basI += ", BA" + (int)(r.high * 100) + "_" + (int)(r.low * 100);
					} else if (i >= 4 && i < lpe.getRangeArray().size() - 1) {
						ranges += ", WIN" + (int)(r.high * -100) + "_" + (int)(r.low * -100) + " integer NOT NULL";
						rangesI += ", WIN" + (int)(r.high * -100) + "_" + (int)(r.low * -100);
						
						bas += ", BA" + (int)(r.high * -100) + "_" + (int)(r.low * -100) + " integer NOT NULL";
						basI += ", BA" + (int)(r.high * -100) + "_" + (int)(r.low * -100);
					} else {
						ranges += ", WIN900UP integer NOT NULL";
						rangesI += ", WIN900UP";
						
						bas += ", BA900UP integer NOT NULL";
						basI += ", BA900UP";
					}
					
					rangesQ += ",?";
					basQ += ",?";
					
				}
			}
			
			String query = "create table " + tableName 
					+ " (BLOCKID bigint NOT NULL, "
					+ "NUMOFSPINS integer NOT NULL, "
					+ "NUMOFLINES smallint NOT NULL, "
					+ "NUMOFFREESPIN integer NOT NULL, "
					+ "AVG_LOSSBALANCE double NOT NULL, "
					+ "MEDIAN_LOSSBALANCE integer NOT NULL, "
					+ "SD_LOSSBALANCE integer NOT NULL, "
					+ "AVG_PAYBACK double NOT NULL, "
					+ "WINCOUNTS integer NOT NULL, "
					+ "LOSSCOUNTS integer NOT NULL, "
					+ "LDWS integer NOT NULL, "
					+ "OUTOFBANDSMEAN double NOT NULL, "
					+ "PBOVERFLOWMEAN double NOT NULL, "
					+ "PBUNDERFLOWMEAN double NOT NULL"
					+ ranges + bas + ")";
			Database.createTable(tableName, query);
		}
		
		// Otherwise, add the LossPercentageEntry to the table
		String query = "insert into " + tableName
				+ "(BLOCKID, NUMOFSPINS, NUMOFLINES, NUMOFFREESPIN, " 
				+ "AVG_LOSSBALANCE, MEDIAN_LOSSBALANCE, SD_LOSSBALANCE, AVG_PAYBACK, " 
				+ "WINCOUNTS, LOSSCOUNTS, LDWS, OUTOFBANDSMEAN, PBOVERFLOWMEAN, PBUNDERFLOWMEAN" 
				+ rangesI + basI + ") "
				+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?" + rangesQ + basQ + ")";
		
		try {
			if (lps == null)
				lps = conn.prepareStatement(query);
			
			
			lps.setLong(1, lpe.getBlockNum());
			lps.setInt(2, lpe.getNumSpins());
			lps.setShort(3, lpe.getNumLine());
			lps.setInt(4, lpe.getNumFreeSpins());
			lps.setDouble(5, -lpe.getAvgLossBalance());
			lps.setInt(6, -lpe.getLossPercentageMedian());
			lps.setInt(7, (int)lpe.getSd());
			lps.setDouble(8, lpe.getAvgPaybackPercentage());
			lps.setInt(9, lpe.getWin());
			lps.setInt(10, lpe.getLoss());
			lps.setInt(11, lpe.getLdw());
			lps.setDouble(12, lpe.getOutofBandsMean());
			lps.setDouble(13, lpe.getPBOverflowMean());
			lps.setDouble(14, lpe.getPBUnderflowMean());
			
			int index = 15;
			
			for (int i = 0; i < lpe.getLossPercentages().size(); i++) {
				lps.setInt(index, lpe.getLossPercentage(i));
				index++;
			}
			
			for (int i = 0; i < lpe.getBonusActivations().size(); i++) {
				lps.setInt(index, lpe.getAvgBonusActivation(i));
				index++;
			}
			
			lps.addBatch();
			lpeBatchRequests++;
			
			if (lpeBatchRequests >= blocksize) {
				lps.executeBatch();
				lpeBatchRequests = 0;
			}
			
			
		} catch (SQLException e) {
			throw e;
		}
	}
	
	public static void insertIntoTable(String tableName, GamblersRuinEntry gre,
			int blocksize) {
		tableName = tableName.toUpperCase();
		
		String spinranges = "";
		String spinrangesQ = "";
		String spinrangesI = "";
		
		if (gre.getSpinRanges() != null) {
			for (int i = 0; i < gre.getSpinRanges().size(); i++) {
				Range r = gre.getSpinRange(i);
				
				if (r.low == r.high) {
					spinranges += ", SPINS" + (int)r.low + "UP integer NOT NULL";
					spinrangesI += ", SPINS" + (int)r.low + "UP";
				} else {
					spinranges += ", SPINS" + (int)r.low + "_" + (int)r.high + " integer NOT NULL";
					spinrangesI += ", SPINS" + (int)r.low + "_" + (int)r.high;
				}
				
				spinrangesQ += ",?";
			}
		}
		
		
		String peakbalanceranges = "";
		String peakbalancerangesQ = "";
		String peakbalancerangesI = "";
		
		if (gre.getPeakBalanceRanges() != null) {
			for (int i = 0; i < gre.getPeakBalanceRanges().size(); i++) {
				Range r = gre.getPeakBalanceRange(i);
				
				if (r.high == 100) {
					peakbalanceranges += ", PB" + (int)r.low + " integer NOT NULL";
					peakbalancerangesI += ", PB" + (int)r.low;
				} else if (r.low == 5000) {
					peakbalanceranges += ", PB" + (int)r.low  + "UP integer NOT NULL";
					peakbalancerangesI += ", PB" + (int)r.low + "UP";
				} else {
					peakbalanceranges += ", PB" + (int)r.low + "_" + (int)r.high + " integer NOT NULL";
					peakbalancerangesI += ", PB" + (int)r.low + "_" + (int)r.high;
				}
				peakbalancerangesQ += ",?";
			}
		}
		
		try {
		// Create table if does not exist
			if (!Database.doesTableExist(tableName)) {
	
				String query = "create table " + tableName 
						+ " (BLOCKID bigint NOT NULL, "
						+ "NUMOFLINES smallint NOT NULL, "
						+ "NUMOFSPINS integer NOT NULL, "
						+ "FREESPINS integer NOT NULL, "
						+ "BONUSACTIVATION integer NOT NULL, "
						+ "WINS integer NOT NULL, "
						+ "LOSSES integer NOT NULL, "
						+ "LDWS integer NOT NULL"
						+ spinranges 
						+ ", MAX_SPINS integer NOT NULL"
						+ ", AVG_SPINS integer NOT NULL"
						+ ", MEDIAN_SPINS integer NOT NULL"
						+ ", SD_SPINS integer NOT NULL"
						+ peakbalanceranges
						+ ", MAX_PEAKBALANCE integer NOT NULL"
						+ ", AVG_PEAKBALANCE integer NOT NULL"
						+ ", MEDIAN_PEAKBALANCE integer NOT NULL" 
						+ ", SD_PEAKBALANCE integer NOT NULL" 
						+ ", PAYBACKPERCENTAGE float NOT NULL)";
				
				Database.createTable(tableName, query);
			}
			
			// Otherwise, add the LossPercentageEntry to the table
			String query = "insert into " + tableName
					+ "(BLOCKID, NUMOFLINES, NUMOFSPINS, FREESPINS, BONUSACTIVATION, WINS, LOSSES, LDWS" 
					+ spinrangesI + ", MAX_SPINS" + ", AVG_SPINS" + ", MEDIAN_SPINS" + ", SD_SPINS" 
					+ peakbalancerangesI + ", MAX_PEAKBALANCE" + ", AVG_PEAKBALANCE" + ", MEDIAN_PEAKBALANCE" + ", SD_PEAKBALANCE" + ", PAYBACKPERCENTAGE) "
					+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?" + spinrangesQ  + peakbalancerangesQ + ")";
			
			
			if (grs == null)
				grs = conn.prepareStatement(query);
			
			int index = 9;
			grs.setLong(1, gre.getBlockNum());
			grs.setShort(2, gre.getNumLine());
			grs.setInt(3, gre.getTotalSpins());
			grs.setInt(4, gre.getNumFreeSpins());
			grs.setInt(5, gre.getNumBonusActivation());
			grs.setInt(6, gre.getWins());
			grs.setInt(7, gre.getLosses());
			grs.setInt(8, gre.getLdws());
			
			for (int i = 0; i < gre.getSpinRanges().size(); i++) {
				grs.setInt(index, gre.getSpins(i));
				index ++;
			}
			
			grs.setInt(index, gre.getMaxSpins());
			index++;
			
			grs.setInt(index, gre.getAvgSpins());
			index++;
			
			grs.setInt(index, gre.getSpinMedian());
			index++;
			
			grs.setInt(index, gre.getSDSpins());
			index++;

			for (int i = 0; i < gre.getPeakBalanceRanges().size(); i++) {
				grs.setInt(index, gre.getPeakBalance(i));
				index ++;
			}
			
			grs.setInt(index, (int)gre.getMaxPeakBalance());
			index++;
			
			grs.setInt(index, (int)gre.getAvgPeakBalance());
			index++;
			
			grs.setInt(index, (int)gre.getPeakBalanceMedian());
			index++;
			
			grs.setInt(index, (int)gre.getSDPeakBalance());
			index++;
			
			grs.setFloat(index, (float)gre.getPayBackPercentage());
			
			grs.addBatch();
			greBatchRequests++;
			
			if (greBatchRequests >= blocksize) {
				grs.executeBatch();
				greBatchRequests = 0;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}		
		
		
		
	}
	
	public static void insertIntoTable(String tableName, PrizeSizeEntry pse, int blocksize) 
			throws SQLException {
		tableName = tableName.toUpperCase();
		
		String prizeranges = "";
		String prizerangesQ = "";
		String prizerangesI = "";
		// Create table if does not exist
		if (!Database.doesTableExist(tableName)) {
			
			

			if (pse.getPrizeRanges() != null) {
				for (int i = 0; i < pse.getPrizeRanges().size(); i++) {
					Range r = pse.getPrizeRange(i);
					
					if (r.low == r.high) {
						prizeranges += ", SIZE" + (int)r.low  + "UP integer NOT NULL";
						prizerangesI += ", SIZE" + (int)r.low  + "UP";
					} else {
						prizeranges += ", SIZE" + (int)r.low + "_" + (int)r.high + " integer NOT NULL";
						prizerangesI += ", SIZE" + (int)r.low + "_" + (int)r.high;
					}
					
					prizerangesQ += ",?";
				}
			}
			
			String query = "create table " + tableName 
					+ " (BLOCKID bigint NOT NULL, "
					+ "NUMOFSPINS integer NOT NULL, "
					+ "NUMOFLINES smallint NOT NULL, "
					+ "NUMOFFREESPIN integer NOT NULL, "
					+ "WINS integer NOT NULL, "
					+ "MULTIWINS integer NOT NULL, "
					+ "LOSSES integer NOT NULL, "
					+ "LDWS integer NOT NULL"
					+ prizeranges + ")";
			Database.createTable(tableName, query);
		}
		
		// Otherwise, add the LossPercentageEntry to the table
		String query = "insert into " + tableName
				+ "(BLOCKID, NUMOFSPINS, NUMOFLINES, NUMOFFREESPIN, WINS, MULTIWINS, LOSSES, LDWS" 
				+ prizerangesI + ") "
				+ "values(?,?,?,?,?,?,?,?" + prizerangesQ + ")";
		
		try {
			if (pss == null)
				pss = conn.prepareStatement(query);
			
			int index = 9;
			pss.setLong(1, pse.getCurrBlock().getBlockNumber());
			pss.setInt(2, pse.getNumSpins());
			pss.setShort(3, pse.getCurrBlock().getNumLines());
			pss.setInt(4, pse.getFreeSpins());
			pss.setInt(5, pse.getWins());
			pss.setInt(6, pse.getMultiWins());
			pss.setInt(7, pse.getLosses());
			pss.setInt(8, pse.getLdws());
			
			for (int i = 0; i < pse.getPrizeSizes().size(); i++) {
				pss.setInt(index, pse.getPrizeSize(i));
				index ++;
			}

			
			pss.addBatch();
			pseBatchRequests++;
			
			if (pseBatchRequests >= blocksize) {
				pss.executeBatch();
				pseBatchRequests = 0;
			}
			
			
		} catch (SQLException e) {
			throw e;
		}
	}
	
	public static void insertIntoTable(String tableName, ForcedFreeSpinEntry ffse, int blocksize)
			throws SQLException {
		tableName = tableName.toUpperCase();
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName + " "
					+ "(BLOCKNUMBER bigint NOT NULL, "
					+ "INI_FREESPINS smallint NOT NULL, "
					+ "TOTAL_SPINS integer NOT NULL, "
					+ "TOTAL_CREDITSWON integer NOT NULL, "
					+ "MIN_SPINS integer NOT NULL, "
					+ "MAX_SPINS integer NOT NULL, "
					+ "MEDIAN_SPINS integer NOT NULL, "
					+ "MIN_CREDITSWON integer NOT NULL, "
					+ "MAX_CREDITSWON integer NOT NULL, "
					+ "MEDIAN_CREDITSWON integer NOT NULL, "
					+ "LOSSES integer NOT NULL, "
					+ "RETRRIGER_3 integer NOT NULL, "
					+ "RETRRIGER_10 integer NOT NULL, "
					+ "RETRRIGER_15 integer NOT NULL)";
			Database.createTable(tableName, query);
		}

		// Otherwise, add it to DB.
		String query = "insert into "
				+ tableName
				+ "(BLOCKNUMBER, INI_FREESPINS, TOTAL_SPINS, TOTAL_CREDITSWON, " +
				"MIN_SPINS, MAX_SPINS, MEDIAN_SPINS, MIN_CREDITSWON, MAX_CREDITSWON, MEDIAN_CREDITSWON, " +
				"LOSSES, RETRRIGER_3, RETRRIGER_10, RETRRIGER_15) "
				+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		try {
			if (ffss == null)
				ffss = conn.prepareStatement(query);
			ffss.setLong(1, ffse.getBlockNum());
			ffss.setShort(2, ffse.getInitialFreeSpins());
			ffss.setInt(3, ffse.getTotalSpins());
			ffss.setInt(4, ffse.getTotalCreditsWon());
			ffss.setInt(5, ffse.getMinSpins());
			ffss.setInt(6, ffse.getMaxSpins());
			ffss.setInt(7, ffse.getSpinMedian());
			ffss.setInt(8, ffse.getMinCreditsWon());
			ffss.setInt(9, ffse.getMaxCreditsWon());
			ffss.setInt(10, ffse.getCreditsWonMedian());
			ffss.setInt(11, ffse.getLossSpins());
			ffss.setInt(12, ffse.getBonusRetriggers().get(0));
			ffss.setInt(13, ffse.getBonusRetriggers().get(1));
			ffss.setInt(14, ffse.getBonusRetriggers().get(2));

			ffss.addBatch();
			ffseBatchRequests++;
			if (ffseBatchRequests >= blocksize) {
				ffseBatchRequests = 0;
				ffss.executeBatch();
			}
		} catch (SQLException e) {
			throw e;
		}
	}
	
	public static void updateTableHit(String tableName, HashMap<SimpleEntry<String, Integer>, Integer> hittable) 
			throws SQLException {
		tableName = tableName.toUpperCase();
		
		if (!Database.doesTableExist(tableName)) {
			System.err.println("Updating hit counts to " + tableName + " encountered error: table does not exist!\n");
		}
		
		PreparedStatement update = null;
		
		String query = "update " + tableName 
				+ " set HITS = ? "
				+ "where SEQUENCE = ? "
				+ "and PAYOUT = ?";
		
		update = conn.prepareStatement(query);
		Iterator<Map.Entry<SimpleEntry<String, Integer>, Integer>> entries = hittable.entrySet().iterator();
		while (entries.hasNext()) { 
				Map.Entry<SimpleEntry<String, Integer>, Integer> entry = entries.next();
			try {
				conn.setAutoCommit(false);
//				st = conn.prepareStatement(query);
				update.setLong(1, entry.getValue());
				update.setString(2, entry.getKey().getKey());
				update.setInt(3, entry.getKey().getValue());
				update.executeUpdate();
//				st.addBatch();
//				st.executeBatch();
				conn.commit();
			
			} catch (SQLException e) {
				throw e;
			}
		}
	}
	
	public static void insertIntoTable(String tablePrefix,
			StreaksEntry se) throws SQLException {
		tablePrefix = tablePrefix.toUpperCase();
		flushBatch();
		
		// If tables do not exist, create the tables
		String WWName = "DOLPHIN_TREASURE_WINNING_STREAKS"
				+"_LDW_AS_WINS_"
				+ tablePrefix.substring(24);
		
		if (!Database.doesTableExist(WWName)) {
			String query = "create table " + WWName + " "
					+ "(BLOCKID bigint NOT NULL, "
					+ "NUMLINES integer NOT NULL, "
					+ "NUMSPINS integer NOT NULL, "
					+ "NUMFREESPINS integer NOT NULL, "
					+ "STREAKLENGTH integer NOT NULL, " 
					+ "NUMSTREAKS integer NOT NULL) ";
			Database.createTable(WWName, query);
		}
		
		String WLName = "DOLPHIN_TREASURE_WINNING_STREAKS"
				+"_LDW_AS_LOSSES_"
				+ tablePrefix.substring(24);
		
		if (!Database.doesTableExist(WLName)) {
			String query = "create table " + WLName + " "
					+ "(BLOCKID bigint NOT NULL, "
					+ "NUMLINES integer NOT NULL, "
					+ "NUMSPINS integer NOT NULL, "
					+ "NUMFREESPINS integer NOT NULL, "
					+ "STREAKLENGTH integer NOT NULL, " 
					+ "NUMSTREAKS integer NOT NULL) ";
			Database.createTable(WLName, query);
		}
		
		String LWName = "DOLPHIN_TREASURE_LOSING_STREAKS"
				+"_LDW_AS_WINS_"
				+ tablePrefix.substring(24);
		
		if (!Database.doesTableExist(LWName)) {
			String query = "create table " + LWName + " "
					+ "(BLOCKID bigint NOT NULL, "
					+ "NUMLINES integer NOT NULL, "
					+ "NUMSPINS integer NOT NULL, "
					+ "NUMFREESPINS integer NOT NULL, "
					+ "STREAKLENGTH_SD double NOT NULL, "
					+ "STREAKLENGTH integer NOT NULL, " 
					+ "NUMSTREAKS integer NOT NULL) ";
			Database.createTable(LWName, query);
		}
		
		String LLName = "DOLPHIN_TREASURE_LOSING_STREAKS"
				+"_LDW_AS_LOSSES_"
				+ tablePrefix.substring(24);
		
		if (!Database.doesTableExist(LLName)) {
			String query = "create table " + LLName + " "
					+ "(BLOCKID bigint NOT NULL, "
					+ "NUMLINES integer NOT NULL, "
					+ "NUMSPINS integer NOT NULL, "
					+ "NUMFREESPINS integer NOT NULL, "
					+ "STREAKLENGTH integer NOT NULL, " 
					+ "NUMSTREAKS integer NOT NULL) ";
			Database.createTable(LLName, query);
		}
		
		// Otherwise, insert winning streaks (LDW as wins) table
		String insert = "insert into " + WWName
				+ "(BLOCKID, NUMLINES, NUMSPINS, NUMFREESPINS, STREAKLENGTH, NUMSTREAKS) "
				+ "values(?,?,?,?,?,?)";
		
		try {
			if (st == null)
				st = conn.prepareStatement(insert);
			
			Iterator<Entry<Integer, Integer>> streaks = se.getWinningStreaks_LDWasWins().entrySet().iterator();
			while (streaks.hasNext()) {
				Entry<Integer, Integer> streak = streaks.next();
	
				st.setLong(1, se.getBlockId());
				st.setInt(2, se.getNumLines());
				st.setInt(3, se.getNumSpins());
				st.setInt(4, se.getNumFreeSpins());
				st.setInt(5, (int)(streak.getKey()));
				st.setInt(6, (int)(streak.getValue()));
				
				st.addBatch();
				batchRequests ++;
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			flushBatch();
		}
		
		// Insert winning streaks (LDW as losses) table
		insert = "insert into " + WLName
				+ "(BLOCKID, NUMLINES, NUMSPINS, NUMFREESPINS, STREAKLENGTH, NUMSTREAKS) "
				+ "values(?,?,?,?,?,?)";
		
		try {
			if (st == null)
				st = conn.prepareStatement(insert);
			
			Iterator<Entry<Integer, Integer>> streaks = se.getWinningStreaks_LDWasLosses().entrySet().iterator();
			while (streaks.hasNext()) {
				Entry<Integer, Integer> streak = streaks.next();
	
				st.setLong(1, se.getBlockId());
				st.setInt(2, se.getNumLines());
				st.setInt(3, se.getNumSpins());
				st.setInt(4, se.getNumFreeSpins());
				st.setInt(5, (int)(streak.getKey()));
				st.setInt(6, (int)(streak.getValue()));
				
				st.addBatch();
				batchRequests ++;
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			flushBatch();
		}
		
		// Insert losing streaks (LDW as wins) table
		insert = "insert into " + LWName
				+ "(BLOCKID, NUMLINES, NUMSPINS, NUMFREESPINS, STREAKLENGTH_SD, STREAKLENGTH, NUMSTREAKS) "
				+ "values(?,?,?,?,?,?,?)";
		
		try {
			if (st == null)
				st = conn.prepareStatement(insert);
			
			Iterator<Entry<Integer, Integer>> streaks = se.getLosingStreaks_LDWasWins().entrySet().iterator();
			while (streaks.hasNext()) {
				Entry<Integer, Integer> streak = streaks.next();
	
				st.setLong(1, se.getBlockId());
				st.setInt(2, se.getNumLines());
				st.setInt(3, se.getNumSpins());
				st.setInt(4, se.getNumFreeSpins());
				st.setDouble(5, se.getMeanStreakSD());
				st.setInt(6, (int)(streak.getKey()));
				st.setInt(7, (int)(streak.getValue()));
				
				st.addBatch();
				batchRequests ++;
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			flushBatch();
		}
		
		// Insert losing streaks (LDW as losses) table
		insert = "insert into " + LLName
				+ "(BLOCKID, NUMLINES, NUMSPINS, NUMFREESPINS, STREAKLENGTH, NUMSTREAKS) "
				+ "values(?,?,?,?,?,?)";
		
		try {
			if (st == null)
				st = conn.prepareStatement(insert);
			
			Iterator<Entry<Integer, Integer>> streaks = se.getLosingStreaks_LDWasLosses().entrySet().iterator();
			while (streaks.hasNext()) {
				Entry<Integer, Integer> streak = streaks.next();
	
				st.setLong(1, se.getBlockId());
				st.setInt(2, se.getNumLines());
				st.setInt(3, se.getNumSpins());
				st.setInt(4, se.getNumFreeSpins());
				st.setInt(5, (int)(streak.getKey()));
				st.setInt(6, (int)(streak.getValue()));
				
				st.addBatch();
				batchRequests ++;
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			flushBatch();
		}
	}
	
	public static void insertIntoTable(String tableName,
			BasicInfoEntry bie) throws SQLException {
		tableName = tableName.toUpperCase();
		
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName + " "
					+ "(BLOCKID bigint NOT NULL, "
					+ "NUMLINES integer NOT NULL, "
					+ "NUMSPINS integer NOT NULL, "
					+ "NUMFREESPINS integer NOT NULL, "
					+ "BONUSINITIALIZATIONS integer NOT NULL, "
					+ "BONUSRETRIGGERING integer NOT NULL, "
					+ "WINS integer NOT NULL, "
					+ "LOSSES integer NOT NULL, "
					+ "LDWS integer NOT NULL, "
					+ "BONUSWINS integer NOT NULL, " 
					+ "BASEPAYOUT bigint NOT NULL, "
					+ "BONUSPAYOUT bigint NOT NULL) ";
			Database.createTable(tableName, query);
		}

		// Otherwise, add it to DB.
		String query = "insert into " + tableName
				+ "(BLOCKID, NUMLINES, NUMSPINS, NUMFREESPINS, BONUSINITIALIZATIONS, " 
				+ "BONUSRETRIGGERING, WINS, LOSSES, LDWS, BONUSWINS, BASEPAYOUT, BONUSPAYOUT) "
				+ "values(?,?,?,?,?,?,?,?,?,?,?,?)";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			
			st.setLong(1, bie.getBlockID());
			st.setInt(2, bie.getNumLines());
			st.setInt(3, bie.getNumSpins());
			st.setInt(4, bie.getNumFreeSpins());
			st.setInt(5, bie.getNumBI());
			st.setInt(6, bie.getNumBR());
			st.setInt(7, bie.getBaseWins());
			st.setInt(8, bie.getBaseLosses());
			st.setInt(9, bie.getBaseLDWs());
			st.setInt(10, bie.getBonusWins());
			st.setLong(11, bie.getBasePayout());
			st.setLong(12, bie.getBonusPayout());
			
			st.addBatch();
			batchRequests++;
			

			if (batchRequests >= 1000) {
				batchRequests = 0;
				st.executeBatch();
			}
		} catch (SQLException e) {
			throw e;
		}
	}
	
	public static void insertIntoTable(String tableName,
			BettingStrategyEntry bse) throws SQLException {
		tableName = tableName.toUpperCase();
		
		String strategies = "";
		String strategiesI = "";
		String strategiesQ = "";
		
		for (int i = 0; i < bse.getStrategies().size(); i++) {
			strategies += ", WINS_" + i + " integer NOT NULL, "
					+ "LDWS_" + i + " integer NOT NULL, "
					+ "LOSSES_" + i + " integer NOT NULL, "
					+ "CREDITSWON_" + i + " bigint NOT NULL, "
					+ "TOTAL_WAGER_" + i + " bigint NOT NULL, "
					+ "BONUSPAYOUT_" + i + " bigint NOT NULL";
			
			strategiesI += ", WINS_" + i
					+ ", LDWS_" + i
					+ ", LOSSES_" + i
					+ ", CREDITSWON_" + i
					+ ", TOTAL_WAGER_" + i
					+ ", BONUSPAYOUT_" + i;
			
			strategiesQ += ",?,?,?,?,?,?";
		}
		
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName
							+ " (BLOCKID bigint NOT NULL, "
							+ "NUMSPINS integer NOT NULL, "
							+ "NUMFREESPINS integer NOT NULL"
							+ strategies +") ";
					Database.createTable(tableName, query);
				}

				// Otherwise, add it to DB.
				String query = "insert into " + tableName
						+ "(BLOCKID, NUMSPINS, NUMFREESPINS" + strategiesI + ") "
						+ "values(?,?,?" + strategiesQ + ")";
				try {
					if (st == null)
						st = conn.prepareStatement(query);
					
					st.setLong(1, bse.getBlockID());
					st.setInt(2, bse.getNumSpins());
					st.setInt(3, bse.getNumFreeSpins());
					int index = 4;
					
					for (int i = 0; i < bse.getStrategies().size(); i++) {
						st.setInt(index, bse.getWin(i));
						st.setInt(++index, bse.getLDW(i));
						st.setInt(++index, bse.getLoss(i));
						st.setLong(++index, bse.getCreditWin(i));
						st.setLong(++index, bse.getWager(i));
						st.setLong(++index, bse.getBonusPayout(i));
						index++;
					}
					
					st.addBatch();
					batchRequests++;
					

					if (batchRequests >= 1000) {
						batchRequests = 0;
						st.executeBatch();
					}
				} catch (SQLException e) {
					throw e;
				}
		
	}
	
	/** Dolphin Treasure only database tables **/
	public static void insertIntoDTTable(String tableName,
			PaytableEntry paytableEntry) throws SQLException {
		tableName = tableName.toUpperCase();
		
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName + " "
					+ "(ENTRYID bigint NOT NULL, "
					+ "WINCODE varchar(10) NOT NULL, "
					+ "SEQUENCE varchar(10) NOT NULL, "
					+ "PAYOUT integer NOT NULL, "
					+ "TYPE varchar(10) NOT NULL) ";
			Database.createTable(tableName, query);
		}

		// Otherwise, add it to DB.
		String query = "insert into " + tableName
				+ "(ENTRYID,WINCODE,SEQUENCE,PAYOUT,TYPE) "
				+ "values(?,?,?,?,?)";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			
			st.setLong(1, paytableEntry.getEntryID());
			st.setString(2, paytableEntry.getWinCode());
			st.setString(3, paytableEntry.getSequence());
			st.setInt(4, paytableEntry.getPayout());
			st.setString(5, paytableEntry.getType().toString());
			
			st.addBatch();
			batchRequests++;
			

			if (batchRequests >= 1000) {
				batchRequests = 0;
				st.executeBatch();
			}
		} catch (SQLException e) {
			throw e;
		}
	}
	
	
	public static void insertIntoTable(String tableName,
			HashMap<SimpleEntry<String, Integer>, Integer> hittable) throws SQLException {
		tableName = tableName.toUpperCase();
	
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName + " "
					+ "(ENTRYID bigint NOT NULL, "
					+ "SEQUENCE varchar(10) NOT NULL, "
					+ "PAYOUT integer NOT NULL, "
					+ "HITS bigint NOT NULL)";
			Database.createTable(tableName, query);
		}

		// Otherwise, add it to DB.
		String query = "insert into " + tableName
				+ "(ENTRYID,SEQUENCE,PAYOUT,HITS) "
				+ "values(?,?,?,?)";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			
			Iterator<Map.Entry<SimpleEntry<String, Integer>, Integer>> entries = hittable.entrySet().iterator();
			long id = 0;
			
			while (entries.hasNext()) { 
				Map.Entry<SimpleEntry<String, Integer>, Integer> entry = entries.next();
				
				st.setLong(1, id);
				st.setString(2, entry.getKey().getKey());
				st.setInt(3, entry.getKey().getValue());
				st.setLong(4, entry.getValue());
				
				id++;
				st.addBatch();
				batchRequests++;
	
				if (batchRequests >= 1000) {
					batchRequests = 0;
					st.executeBatch();
				}
			}
		} catch (SQLException e) {
			throw e;
		}
	}
	
	// Inserts a result into the Sands of Splendor spin results table; 
	// if the table does not exist it will be created first.
	public static void insertIntoSoSTable(String tableName, Result result)
			throws SQLException {
		tableName = tableName.toUpperCase();
		
		String lineWins = "";
		String lineWinsI = "";
		String lineWinsQ = "";
		
		for (int i = 0; i < maxLines; i++) {
			lineWins += ", L" + (i+1) + "_CR smallint NOT NULL"
					+ ", L" + (i+1) + "_NAME varchar(10) NOT NULL";
			
			lineWinsI += ", L" + (i+1) + "_CR"
					+ ", L" + (i+1) + "_NAME";
			
			lineWinsQ += ",?,?";
		}
		
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName 
					+ " (RECORDNUMBER bigint NOT NULL, "
					+ "BLOCKNUMBER bigint NOT NULL, "
					+ "REPEATNUMBER integer NOT NULL, "
					+ "REELSTOP1 smallint NOT NULL, "
					+ "REELSTOP2 smallint NOT NULL, "
					+ "REELSTOP3 smallint NOT NULL, "
					+ "REELSTOP4 smallint NOT NULL, "
					+ "REELSTOP5 smallint NOT NULL, "
					+ "LINES smallint NOT NULL, "
					+ "LINEBET smallint NOT NULL, "
					+ "DENOMINATION float NOT NULL, "
					+ "CREDITSWON integer NOT NULL, "
					+ "LINESWON smallint NOT NULL, "
					+ "SCATTER int NOT NULL, "
					+ "BONUSACTIVATED boolean NOT NULL, "
					+ "BONUSSPIN boolean NOT NULL, "
					+ "FREESPINSAWARDED smallint NOT NULL" 
					+ lineWins + ")";
			Database.createTable(tableName, query);
		}
		
		
		// Otherwise, add it to DB.
		String query = "insert into "
				+ tableName
				+ " (RECORDNUMBER, BLOCKNUMBER, REPEATNUMBER, REELSTOP1, REELSTOP2, REELSTOP3, REELSTOP4, REELSTOP5, LINES, " 
				+ "LINEBET, DENOMINATION, CREDITSWON, LINESWON, SCATTER, BONUSACTIVATED, BONUSSPIN, FREESPINSAWARDED"
				+ lineWinsI + ") " + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?" + lineWinsQ + ")";
		
		try {
			if ( st == null)
				st = conn.prepareStatement(query);
			
			st.setLong(1, result.getRecordNumber());
			st.setLong(2, result.getBlockNumber());
			st.setInt(3, result.getRepeatNumber());
			st.setShort(4, result.getReelStop1());
			st.setShort(5, result.getReelStop2());
			st.setShort(6, result.getReelStop3());
			st.setShort(7, result.getReelStop4());
			st.setShort(8, result.getReelStop5());
			st.setShort(9, result.getNumLines());
			st.setShort(10, result.getLineBet());
			st.setDouble(11, result.getFormattedDenomination());
			st.setInt(12, result.getCreditsWon());
			st.setShort(13, result.getLinesWon());
			st.setInt(14, result.getScatter());
			st.setBoolean(15, result.getBonusActivated());
			st.setBoolean(16, result.getBonusSpin());
			st.setShort(17, result.getFreeSpinsAwarded());

			int index = 17;
			
			for (int i = 0; i < result.getLineCreditWinAmounts().size(); i++) {	
				st.setInt(++index, result.getLineCreditWinAmount(i));
				st.setString(++index, result.getLineWinName(i));
			}
			

			st.addBatch();
			batchRequests++;
			if (batchRequests >= 1000) {
				batchRequests = 0;
				st.executeBatch();
			}
		} catch (SQLException e) {
			throw e;
		}
	}
	

	public static void flushBatch() throws SQLException {
		if (batchRequests > 0) {
			batchRequests = 0;
			
			try {
				st.executeBatch();
				st = null;
			} catch (SQLException e) {
				throw e;
			}
			
		} else {
			st = null;
		}
	}

	// Checks if the table exists in our DB
	private static boolean doesTableExist(String tableName) throws SQLException {
		if (listOfTables == null) {
			Database.popuplateListOfTables();
		}
		return listOfTables.contains(tableName);
	}

	// Creates a table in the DB if it doesn't exist already
	private static void createTable(String tableName, String query)
			throws SQLException {
		tableName = tableName.toUpperCase();
		// If exists, do nothing
		if (Database.doesTableExist(tableName)) {
			return;
		}
		// Otherwise, add it to DB.
		Statement st = null;

		try {
			st = conn.createStatement();
			st.executeUpdate(query);
		} catch (SQLException e) {
			throw e;
		} finally {
			if (st != null) {
				st.close();
				// Add to list of tables
				listOfTables.add(tableName);
			}
		}
	}

	// Drops the table from the DB
	public static void dropTable(String tableName) throws SQLException {
		tableName = tableName.toUpperCase();
		// If does not exist, do nothing
		if (!Database.doesTableExist(tableName)) {
			return;
		}
		// Otherwise, drop it from DB.
		Statement st = null;
		String query = "drop table " + tableName;
		try {
			st = conn.createStatement();
			st.executeUpdate(query);
		} catch (SQLException e) {
			throw e;
		} finally {
			if (st != null) {
				st.close();
				// Add to list of tables
				listOfTables.remove(tableName);
			}
		}
	}

	// Creates a database connection by implicitly loading the derby driver and
	// connecting to the database.
	public static void createConnection() throws Exception {
		String driver = "org.apache.derby.jdbc.EmbeddedDriver";
		Class.forName(driver).newInstance();
		conn = DriverManager.getConnection(Database.URL);
		conn.setAutoCommit(false);
	}

	// Always shutdown the database when done - improves loading time for next
	// startup.
	public static void shutdownConnection() throws SQLException {
		try {
			// Close all statements when shutting down the application
			if (st != null) {
				st.close();
				st = null;
			}
			
			if (lps != null) {
				lps.close();
				lps = null;
			}
			
			if (grs != null) {
				grs.close();
				grs = null;
			}
			
			if (pss != null) {
				pss.close();
				pss = null;
			}
			
			if (ffss != null) {
				ffss.close();
				ffss = null;
			}
			
			if (soss != null) {
				soss.close();
				soss = null;
			}
			
			conn.commit();
			DriverManager.getConnection(URL_SHUTDOWN);
		} catch (SQLException se) {
			if (!se.getSQLState().equals("XJ015")) {
				throw se;
			}
		}
	}
	
	public static String convertToPARSequence(String sequence) {
		String parSequence = "";
		
		for (int i=0; i < sequence.length(); i++) {
			if (sequence.charAt(i) != '#') {
				parSequence += PAR_MAPPING.get(String.valueOf(sequence.charAt(i)));
			} else 
				parSequence += "#";
		}
		
		return parSequence;
	}

	public static boolean isConnected() {
		if (conn == null)
			return false;
		
		try {
			return conn.isValid(0);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}


}
