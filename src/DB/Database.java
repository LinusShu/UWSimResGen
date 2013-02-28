package DB;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import uwsimresgen.model.ResultsModel;
import uwsimresgen.model.ResultsModel.Block;
import uwsimresgen.model.ResultsModel.BonusSpinOdd;
import uwsimresgen.model.ResultsModel.Payline;
import uwsimresgen.model.ResultsModel.PaytableEntry;
import uwsimresgen.model.ResultsModel.Result;
import uwsimresgen.model.ResultsModel.Symbol;

public class Database {

	// public static void main(String[] args) {
	// try {
	// String tableName = "TABLENAME";
	// Database.createConnection();
	// // Database.createTable(tableName);
	// // Database.dropTable(tableName);
	// Database.createTable(tableName);
	// Result result = new ResultsModel().new Result();
	// result.setReelStop1(1);
	// result.setReelStop2(2);
	// result.setReelStop3(3);
	// result.setReelStop4(4);
	// result.setReelStop5(5);
	// result.setNumLines(6);
	// result.setLineBet(7);
	// result.setDenomination(8.5);
	// result.setDollarsWon(9.4);
	// result.setCreditsWon(10.10);
	// result.setLinesWon(11);
	// result.setScatter(true);
	// result.setBonusActivated(true);
	// result.setBonusSpin(false);
	//
	// Database.insertIntoTable(tableName, result);
	//
	// Database.shutdownConnection();
	// } catch (Exception e) {
	// if (e instanceof SQLException) {
	// e.printStackTrace();
	// } else {
	// e.printStackTrace();
	// }
	// }
	// }

	public static String DEFAULT_DB_NAME = "UWResGenDB";

	// Change me to modify the database name.
	private static String dbName = DEFAULT_DB_NAME;
	private static String URL = "jdbc:derby:" + dbName + ";create=true";
	private static final String URL_SHUTDOWN = "jdbc:derby:;shutdown=true";

	private static Connection conn = null;
	private static ArrayList<String> listOfTables;

	private static int maxLines;

	public static void setMaxLines(int maxLines) {
		Database.maxLines = maxLines;
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

	static PreparedStatement st = null;

	static int batchRequests = 0;

	public static void insertIntoTable(String tableName, BonusSpinOdd spinOdd)
			throws SQLException {
		tableName = tableName.toUpperCase();
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName + " "
					+ "(SPINID bigint NOT NULL, " + "SLICE integer NOT NULL, "
					+ "SPINSAWARDED smallint NOT NULL)";
			Database.createTable(tableName, query);
		}

		// Otherwise, add it to DB.
		String query = "insert into " + tableName
				+ "(SPINID,SLICE,SPINSAWARDED) " + "values(?,?,?)";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			st.setLong(1, spinOdd.getSpinID());
			st.setInt(2, spinOdd.getSlice());
			st.setShort(3, spinOdd.getSpinsAwarded());

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
			PaytableEntry paytableEntry) throws SQLException {
		tableName = tableName.toUpperCase();
		// If does not exist, create the table
		if (!Database.doesTableExist(tableName)) {
			String query = "create table " + tableName + " "
					+ "(ENTRYID bigint NOT NULL, "
					+ "WINCODE varchar(10) NOT NULL, "
					+ "SEQUENCE varchar(10) NOT NULL, "
					+ "PAYOUT integer NOT NULL, "
					+ "TYPE varchar(10) NOT NULL)";
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
					+ "NUMOFSPINS bigint NOT NULL)";
			Database.createTable(tableName, query);
		}

		// Otherwise, add it to DB.
		String query = "insert into "
				+ tableName
				+ "(BLOCKNUMBER, NUMOFLINES, LINEBET, DENOMINATION, FORMATTEDDENOMINATION, NUMOFSPINS) "
				+ "values(?,?,?,?,?,?)";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			st.setLong(1, block.getBlockNumber());
			st.setShort(2, block.getNumLines());
			st.setShort(3, block.getLineBet());
			st.setShort(4, block.getDenomination());
			st.setDouble(5, block.getFormattedDenomination());
			st.setLong(6, block.getNumSpins());

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
			String lineWins_DOLLARS = "";
			for (int i = 0; i < maxLines; i++) {
				lineWins_DOLLARS += ", LINE" + i + "WIN_DOLLARS float NOT NULL"; //change this to SCATTER_CREDITS
			}
			String lineWins_CREDITS = "";
			for (int i = 0; i < maxLines; i++) {
				lineWins_CREDITS += ", LINE" + i
						+ "WIN_CREDITS smallint NOT NULL";
			}
			
			String wbBonus_CREDITS = "";
			for (int i=0; i < 3; i++) {
				wbBonus_CREDITS += ", WBBONUS" + i
						+ "WIN_CREDITS smallint NOT NULL";
			}

			String query = "create table " + tableName + " "
					+ "(RECORDNUMBER bigint NOT NULL, "
					+ "BLOCKNUMBER bigint NOT NULL, "
					+ "REELSTOP1 smallint NOT NULL, "
					+ "REELSTOP2 smallint NOT NULL, "
					+ "REELSTOP3 smallint NOT NULL, "
					+ "REELSTOP4 smallint NOT NULL, "
					+ "REELSTOP5 smallint NOT NULL, "
					+ "NUMOFLINES smallint NOT NULL, "
					+ "LINEBET smallint NOT NULL, "
					+ "DENOMINATION float NOT NULL, "
					+ "DOLLARSWON float NOT NULL, "
					+ "CREDITSWON integer NOT NULL, "
					+ "LINESWON smallint NOT NULL, "
					+ "SCATTER boolean NOT NULL, "
					+ "BONUSACTIVATED boolean NOT NULL, "
					+ "BONUSSPIN boolean NOT NULL, "
					+ "FREESPINSAWARDED smallint NOT NULL" 
					+ wbBonus_CREDITS
					+ lineWins_DOLLARS
					+ lineWins_CREDITS + ")";
			Database.createTable(tableName, query);
		}

		String lineWins_Dollars = "";
		String lineWins_DollarsQ = "";
		if (result.getLineDollarWinAmounts() != null
				&& result.getLineDollarWinAmounts().size() > 0) {
			for (int i = 0; i < result.getLineDollarWinAmounts().size(); i++) {
				lineWins_Dollars += ", LINE" + i + "WIN_DOLLARS";
				lineWins_DollarsQ += ",?";
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
		
		String wbbWins_Credits = "";
		String wbbWins_CreditsQ = "";
		if (result.getWBBonusCreditWin() != null
				&& result.getWBBonusCreditWin().size() > 0) {
			for (int i = 0; i < result.getWBBonusCreditWin().size(); i++) {
				wbbWins_Credits += ", WBBONUS" + i + "WIN_CREDITS";
				wbbWins_CreditsQ += ",?";
			}
		}
		
		// Otherwise, add it to DB.
		String query = "insert into "
				+ tableName
				+ "(RECORDNUMBER, BLOCKNUMBER, REELSTOP1, REELSTOP2, REELSTOP3, REELSTOP4, REELSTOP5, NUMOfLINES, LINEBET, DENOMINATION, DOLLARSWON, CREDITSWON, LINESWON, SCATTER, BONUSACTIVATED, BONUSSPIN, FREESPINSAWARDED"
				+ wbbWins_Credits + lineWins_Dollars + lineWins_Credits + ") "  //TODO change lineWin_Dollars
				+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?"
				+ wbbWins_CreditsQ + lineWins_DollarsQ + lineWins_CreditsQ + ")";
		try {
			if (st == null)
				st = conn.prepareStatement(query);
			st.setLong(1, result.getRecordNumber());
			st.setLong(2, result.getBlockNumber());
			st.setShort(3, result.getReelStop1());
			st.setShort(4, result.getReelStop2());
			st.setShort(5, result.getReelStop3());
			st.setShort(6, result.getReelStop4());
			st.setShort(7, result.getReelStop5());
			st.setShort(8, result.getNumLines());
			st.setShort(9, result.getLineBet());
			st.setDouble(10, result.getFormattedDenomination());
			st.setDouble(11, result.getFormattedDollarsWon());
			st.setInt(12, result.getCreditsWon());
			st.setShort(13, result.getLinesWon());
			st.setBoolean(14, result.getScatter());
			st.setBoolean(15, result.getBonusActivated());
			st.setBoolean(16, result.getBonusSpin());
			st.setShort(17, result.getFreeSpinsAwarded());

			int startingIndex = 18;

			if (result.getWBBonusCreditWin() != null
					&& result.getWBBonusCreditWin().size() > 0) {
				for (int i = 0; i < result.getWBBonusCreditWin().size(); i++) {
					st.setDouble(startingIndex, result.getWBBonusCreditWinOn(i));
					startingIndex++;
				}
			}
			
			if (result.getLineDollarWinAmounts() != null
					&& result.getLineDollarWinAmounts().size() > 0) {
				for (int i = 0; i < result.getLineDollarWinAmounts().size(); i++) {
					st.setDouble(startingIndex,
							result.getFormattedLineDollarWinAmount(i));
					startingIndex++;
				}
			}

			if (result.getLineCreditWinAmounts() != null
					&& result.getLineCreditWinAmounts().size() > 0) {
				for (int i = 0; i < result.getLineCreditWinAmounts().size(); i++) {
					st.setDouble(startingIndex,
							result.getLineCreditWinAmount(i));
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

	public static void flushBatch() throws SQLException {
		if (batchRequests > 0) {
			batchRequests = 0;
			try {
				st.executeBatch();
				st = null;
			} catch (SQLException e) {
				throw e;
			}
			;
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
			if (st != null) {
				st.close();
				st = null;
			}
			conn.commit();
			DriverManager.getConnection(URL_SHUTDOWN);
		} catch (SQLException se) {
			if (!se.getSQLState().equals("XJ015")) {
				throw se;
			}
		}
	}

}
