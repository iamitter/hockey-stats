/*
 * Gets play by play and time on ice information for games specified.
 * */

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.JOptionPane;

public class DownloadPlayByPlay {
    
	public static final String FOLDERPATHPBP = "/Users/Matt/Desktop/NHL/PlaybyPlay/";
	/*
	 * The folder for all the play by play information. You must have already
	 * created the NHL directory; it will create the final directory if need be.
	 */
	public static final String FOLDERPATHTOI = "/Users/Matt/Desktop/NHL/TOI/";
	/*
	 * The folder for all the shift information. You must have already created
	 * the NHL directory; it will create the final directory if need be.
	 */
	public static final int SEASON_MAX = 2013;
	/* The max permissible season a user can ask for data from. */
	private static final Map<String, String> ABBREVS = getTeamToAbbrevMap();
	/*
	 * A Map of team names to three-letter abbreviations, and vice-versa (e.g.
	 * Washington Capitals<-->WSH. The method getTeamToAbbrevMap has public
	 * access for convenience.
	 */
	private static Map<String, Character> playerNameToPos;
    
	/*
	 * A Map of player names to positions, to be created later depending on
	 * season(s) being looked at.
	 */
    
	public static void main(String[] args) {
		/* See if FOLDERPATHs are created. If not, create them. */
		File f = new File(FOLDERPATHPBP);
		if (!f.exists())
			f.mkdir();
		f = new File(FOLDERPATHTOI);
		if (!f.exists())
			f.mkdir();
		int n;
		while (true)
			try {
				/* Ask what the user wants to do. */
				n = Integer
                .parseInt(JOptionPane
                          .showInputDialog("Enter 1 to auto-update. Enter 2 to read plays and toi. Enter 3 to read only pbp. Enter 4 to read only toi. Enter -1 to exit."));
				if (n == -1)
					System.exit(0);
				else if (n != 1 && n != 2 && n != 3 && n != 4)
					throw new NumberFormatException();
				else
					break;
			} catch (NumberFormatException nfe) {
				JOptionPane
                .showMessageDialog(null, "Invalid entry. Try again.");
			}
		if (n == 1) {
			int startseason = getStartSeason();
			int endseason = getEndSeason(startseason);
			autoupdate(startseason, endseason);
		} else if (n != -1) {
			int startseason = getStartSeason();
			int endseason = getEndSeason(startseason);
			int startgame = getStartGame(startseason);
			int endgame = getEndGame(endseason);
			if (n == 2 || n == 3)
				read(startseason, endseason, startgame, endgame);
			if (n == 2 || n == 4) {
				getMaxGameSoFar(endseason);
				readTOI(startseason, endseason, startgame, endgame);
			}
		}
		// else edit(); /*In a future update, to correct lines without standard
		// formatting or missing info*/
		System.out.println("Done");
		System.exit(0);
	}
    
	public static void autoupdate(int startseason, int endseason) {
		File[] flist;
		int maxread;
		int thisgamenum;
		int maxgamesofar;
		for (int season = startseason; season <= endseason; season++) {
			flist = new File(FOLDERPATHPBP.toString() + "/" + season + "/")
            .listFiles();
			maxread = 20000;
			if (flist != null)
				for (int i = 0; i < flist.length; i++)
					try {
						thisgamenum = Integer
                        .parseInt(flist[i].toString()
                                  .substring(
                                             flist[i].toString()
                                             .lastIndexOf("/") + 1,
                                             flist[i].toString()
                                             .lastIndexOf(".")));
						if (thisgamenum > maxread)
							maxread = thisgamenum;
					} catch (Exception e) {
                        
					}
			if (!(maxread == 21230 || maxread == 20720 && season == 2012)) {
				maxgamesofar = getMaxGameSoFar(season);
				if (maxgamesofar > maxread)
					read(season, season, maxread + 1, maxgamesofar);
			}
			flist = new File(FOLDERPATHTOI.toString() + "/" + season + "/")
            .listFiles();
			maxread = 20000;
			if (flist != null)
				for (int i = 0; i < flist.length; i++)
					try {
						thisgamenum = Integer
                        .parseInt(flist[i].toString()
                                  .substring(
                                             flist[i].toString()
                                             .lastIndexOf("/") + 1,
                                             flist[i].toString()
                                             .lastIndexOf(" ")));
						if (thisgamenum > maxread)
							maxread = thisgamenum;
					} catch (Exception e) {
                        
					}
			if (!(maxread == 21230 || maxread == 20720 && season == 2012)) {
				maxgamesofar = getMaxGameSoFar(season);
				if (maxgamesofar > maxread)
					readTOI(season, season, maxread + 1, maxgamesofar);
			}
		}
	}
    
	private static int getMaxGameSoFar(int season) {
		URL url = null;
		BufferedWriter schedulewriter = null;
		String date;
		String road;
		String home;
		int id;
		int homescore;
		int roadscore;
		String extra;
		int max = 0;
		Map<String, String> abbrevs2 = getScheduleAbbreviationsMap();
		try {
			url = new URL("http://www.nhl.com/ice/schedulebyseason.htm?season="
                          + season + (season + 1)
                          + "&gameType=2&team=&network=&venue=");
		} catch (MalformedURLException murle) {
			return 0;
		}
		BufferedReader f = null;
		String line = "";
		Map<Integer, String> gamenumToScheduleInfo = new TreeMap<Integer, String>();
		try {
			try {
				f = new BufferedReader(new InputStreamReader(url.openStream()));
				while (!line.contains("FINAL GAMES"))
					line = f.readLine();
				while (true) {
					// games yet to be played
					if (line.contains("skedStartDateSite")) {
						line = line
                        .substring(line.indexOf("skedStartDateSite"));
						date = line.substring(line.indexOf(">") + 1,
                                              line.indexOf("<"));
						line = f.readLine();
						line = f.readLine();
						road = abbrevs2.get(line.trim());
						line = f.readLine();
						roadscore = Integer.parseInt(line.substring(
                                                                    line.indexOf("(") + 1, line.indexOf(")")));
						line = f.readLine();
						home = abbrevs2.get(line
                                            .substring(0, line.indexOf("(")).trim());
						homescore = Integer.parseInt(line.substring(
                                                                    line.indexOf("(") + 1, line.indexOf(")")));
						line = f.readLine();
						if (line.charAt(0) != '<')
							extra = line.substring(0, line.indexOf("<"));
						else
							extra = "";
						line = line
                        .substring(line
                                   .indexOf("http://www.nhl.com/gamecenter/en/recap?id="));
						line = line.substring(line.indexOf("=") + 6);
						id = Integer.parseInt(line.substring(0, 5));
						gamenumToScheduleInfo.put(id, id + "," + road + ","
                                                  + roadscore + ",@," + home + "," + homescore
                                                  + "," + extra + "," + date);
						/*
						 * System.out.println(season + "," + id + "," + road +
						 * "," + roadscore + ",@," + home + "," + homescore +
						 * "," + extra + "," + date);
						 */
						if (id > max && id <= 21230)
							max = id;
					} else
						line = f.readLine();
				}
			} catch (NullPointerException npe) {
				f.close();
			}
		} catch (UnknownHostException uhe) {
			System.out.println("Not connected to internet: " + uhe);
			System.exit(0);
		} catch (IOException ioe) {
			System.out.println("When trying to read " + season
                               + " season schedule from NHL.com:\t" + ioe);
		}
		try {
			schedulewriter = new BufferedWriter(new FileWriter(new File(
                                                                        FOLDERPATHPBP + season + " schedule.csv")));
			schedulewriter
            .write("Game ID,Road Team,Score,@,Home Team,Score,OT/SO,Date\n");
			for (Integer game : gamenumToScheduleInfo.keySet())
				schedulewriter.write(gamenumToScheduleInfo.get(game) + "\n");
			schedulewriter.close();
		} catch (IOException ioe) {
			System.out.println("When trying to write " + season
                               + " season schedule file:\t" + ioe);
		}
		return max;
	}
    
	/* Asks the user which season to start pulling data from. */
	public static int getStartSeason() {
		int startseason = 0;
		while (true)
			try {
				startseason = Integer
                .parseInt(JOptionPane
                          .showInputDialog("Enter start season (e.g. 2007 for 07-08; enter -1 to exit)"));
				if (startseason == -1)
					System.exit(0);
				if (startseason < 2007 || startseason > SEASON_MAX)
					throw new NumberFormatException();
				else
					break;
			} catch (NumberFormatException nfe) {
				JOptionPane
                .showMessageDialog(null, "Invalid entry. Try again.");
			}
		return startseason;
	}
    
	/* Asks the user which season to stop pulling data from. */
	public static int getEndSeason(int startseason) {
		int endseason = 0;
		while (true)
			try {
				endseason = Integer
                .parseInt(JOptionPane
                          .showInputDialog("Enter end season (e.g. 2012 for 12-13; enter -1 to exit)"));
				if (endseason == -1)
					System.exit(0);
				if (endseason < startseason || endseason > SEASON_MAX)
					throw new NumberFormatException();
				else
					break;
			} catch (NumberFormatException nfe) {
				JOptionPane
                .showMessageDialog(null, "Invalid entry. Try again.");
			}
		return endseason;
	}
    
	/* Asks the user which game in starting season to start pulling data from. */
	public static int getStartGame(int startseason) {
		int startgame = 0;
		while (true)
			try {
				startgame = Integer.parseInt(JOptionPane
                                             .showInputDialog("Enter start game in " + startseason
                                                              + " (e.g. 20001)"));
                
				if (startgame < 20001 || startgame > 21230
                    || (startgame > 20720 && startseason == 2012))
					throw new NumberFormatException();
				else
					break;
			} catch (NumberFormatException nfe) {
				JOptionPane
                .showMessageDialog(null, "Invalid entry. Try again.");
			}
		return startgame;
	}
    
	/* Asks the user which game in ending season to stop pulling data from. */
	public static int getEndGame(int endseason) {
		int endgame = 0;
		while (true)
			try {
				endgame = Integer.parseInt(JOptionPane
                                           .showInputDialog("Enter end game in " + endseason
                                                            + " (e.g. 21230, or 20720 for 2012)"));
				if (endgame < 20001 || endgame > 21230
                    || (endgame > 20720 && endseason == 2012))
					throw new NumberFormatException();
				else
					break;
			} catch (NumberFormatException nfe) {
				JOptionPane
                .showMessageDialog(null, "Invalid entry. Try again.");
			}
		return endgame;
	}
    
	/* This method coordinates reading play by play for an entire season. */
	private static void read(int startseason, int endseason, int startgame,
                             int endgame) {
		BufferedReader pbpin;
		BufferedWriter pbpout;
		BufferedWriter errorw = null;
		String url;
		URL pbp;
		File f;
		for (int season = startseason; season <= endseason; season++) {
			/* Creates a folder for this season if necessary */
			f = new File(FOLDERPATHPBP + season);
			if (!f.exists())
				if (!f.mkdir()) {
					System.out
                    .println("Couldn't create pbp folder for this season. Did you move or rename folders around on your computer?");
				}
			url = "http://www.nhl.com/scores/htmlreports/" + season
            + (season + 1) + "/PL0";
			int limit = 21230;
			if (season == 2012)
				limit = 20720;
			if (season == endseason)
				limit = endgame;
			for (int game = startgame; game <= limit; game++)
			/* if (!(new File(f.toString() + "/" + game + ".csv").exists())) */{
				try {
					/* Access pbp and create output file in season's folder. */
					pbp = new URL(url + game + ".HTM");
					pbpin = new BufferedReader(new InputStreamReader(
                                                                     pbp.openStream()));
					pbpout = new BufferedWriter(new FileWriter(new File(
                                                                        f.toString() + "/" + game + ".csv")));
					/*
					 * errorw = new BufferedWriter(new FileWriter(new File(
					 * FOLDERPATHPBP + season + " errors.csv")));
					 */
					errorw = null;
					/*
					 * In the future, will have error writer write events that
					 * are 5v0, 1v0, etc (excluding shootouts), since those are
					 * not correctly formatted, obviously. Will also note other
					 * mistakes.
					 */
					// errorw.write("Errors--please fix by hand\n");
					ReadPlayByPlay(pbpin, pbpout, season, game, errorw);
					pbpin.close();
					pbpout.close();
					// errorw.close();
				} catch (MalformedURLException murle) {
					System.out.println("Malformed url for " + season + " "
                                       + game);
					System.exit(0);
				} catch (IOException ioe) {
					System.out.println(season + "\t" + game + "\t" + ioe);
				}
				if (game % 100 == 0)
					System.out.println("Done with pbp for " + season
                                       + " through " + game);
			}
			try {
				generateTeamPages(season, limit);
			} catch (IOException ioe) {
				System.out.println("Trouble making team pages...???");
			}
			System.out.println("Done with pbp for " + season);
		}
        
	}
    
	/*
	 * If play by play is not published, will write game information in schedule
	 * nonetheless. Only a few games were like this.
	 */
	private static void writeMissingGame(int season, int game,
                                         BufferedWriter out) {
		try {
			if (season == 2008) {
				if (game == 20259) // 2008
					out.write(game
                              + ","
                              + "BOSTON BRUINS,@,TORONTO MAPLE LEAFS,Monday,November 17,2008");
				else if (game == 20409) // 2008
					out.write(game
                              + ","
                              + "CALGARY FLAMES,@,DETROIT RED WINGS,Wednesday,December 10,2008");
				else if (game == 21077) // 2008
					out.write(game
                              + ","
                              + "VANCOUVER CANUCKS,@,PHOENIX COYOTES,Saturday,March 21,2009");
			} else if (season == 2009) {
				if (game == 20827) // 2009
					out.write(game
                              + ","
                              + "WASHINGTON CAPITALS,@,BOSTON BRUINS,Tuesday,February 2,2010");
				else if (game == 20836) // 2009
					out.write(game
                              + ","
                              + "OTTAWA SENATORS,@,BUFFALO SABRES,Wednesday,February 3,2010");
				else if (game == 20857) // 2009
					out.write(game
                              + ","
                              + "DETROIT RED WINGS,@,LOS ANGELES KINGS,Saturday,February 6,2010");
				else if (game == 20863) // 2009
					out.write(game
                              + ","
                              + "CALGARY FLAMES,@,TAMPA BAY LIGHTNING,Saturday,February 6,2010");
				else if (game == 20874) // 2009
					out.write(game
                              + ","
                              + "EDMONTON OILERS,@,PHOENIX COYOTES,Monday,February 8,2010");
			} else if (season == 2010) {
				if (game == 20124) // 2010
					out.write(game
                              + ","
                              + "WASHINGTON CAPITALS,@,CAROLINA HURRICANES,Wednesday,October 27,2010");
				else if (game == 20429) // 2010
					out.write(game
                              + ","
                              + "ATLANTA THRASHERS,@,NEW YORK ISLANDERS,Saturday,December 11,2010");
			} else if (season == 2011) {
				if (game == 20259)
					out.write(game
                              + ","
                              + "ANAHEIM DUCKS,@,LOS ANGELES KINGS,Wednesday,November 16,2011");
			} else if (season == 2013) {
				if (game == 20647)
					out.write(game
                              + ","
                              + "CAROLINA HURRICANES,@,BUFFALO SABRES,Tuesday,February 25,2014");
			} else
				throw new IOException();
			out.newLine();
		} catch (IOException ioe) {
			System.out.println("Edit schedule file for " + game);
		}
	}
    
	/*
	 * Creates team roster file (in TOI folder) and a team's full-season pbp log
	 * (in PbP/season/) folder for convenience. I find it easier to read from
	 * the team pbp logs than from the actual game logs
	 */
	private static void generateTeamPages(int season, int limit)
    throws IOException {
		Map<String, BufferedWriter> teamToPbPWriter = new HashMap<String, BufferedWriter>();
		Map<String, Set<String>> teamToPlayerNamesAndNums = new HashMap<String, Set<String>>();
		/*
		 * Create all team season log writers at once. Call the required ones as
		 * needed.
		 */
		for (String team : ABBREVS.keySet())
			if (team.length() == 3) {
				teamToPlayerNamesAndNums.put(ABBREVS.get(team),
                                             new HashSet<String>());
				teamToPbPWriter
                .put(team, new BufferedWriter(new FileWriter(new File(
                                                                      FOLDERPATHPBP + season + "/" + team + ".csv"))));
				teamToPbPWriter.get(team).write(
                                                season + " log for " + ABBREVS.get(team) + "\n");
				teamToPbPWriter
                .get(team)
                .write("Game,Opponent,Period,Strength,Time,Score,Event,Team,Zone,Actor,Recipient,Note,");
				teamToPbPWriter.get(team).write(
                                                "Opp1,Opp1Name,Opp2,Opp2Name,Opp3,Opp3Name,");
				teamToPbPWriter.get(team).write(
                                                "Opp4,Opp4Name,Opp5,Opp5Name,Opp6,Opp6Name,");
				teamToPbPWriter.get(team).write(
                                                team + "1," + team + "1Name," + team + "2," + team
                                                + "2Name," + team + "3," + team + "3Name,");
				teamToPbPWriter.get(team).write(
                                                team + "4," + team + "4Name," + team + "5," + team
                                                + "5Name," + team + "6," + team + "6Name\n");
			}
		BufferedReader in = null;
		String[] s;
		String line;
		BufferedWriter bw1;
		BufferedWriter bw2;
		String homename;
		String roadname;
		String pref1;
		String pref2;
		String[] other;
		for (int g = 20001; g <= limit; g++)
			try {
				other = null;
				in = new BufferedReader(new FileReader(FOLDERPATHPBP + season
                                                       + "/" + g + ".csv"));
				s = in.readLine().split("@");
				homename = s[1].trim();
				/* Montreal's name is sometimes reversed. */
				if (homename.equals("CANADIENS MONTREAL"))
					homename = "MONTREAL CANADIENS";
				roadname = s[0].substring(s[0].indexOf(":") + 1).trim();
				if (roadname.equals("CANADIENS MONTREAL"))
					roadname = "MONTREAL CANADIENS";
				bw1 = teamToPbPWriter.get(ABBREVS.get(homename));
				bw2 = teamToPbPWriter.get(ABBREVS.get(roadname));
				/* Every line starts with game number and opponent */
				pref1 = g + "," + ABBREVS.get(roadname) + ",";
				pref2 = g + ",@" + ABBREVS.get(homename) + ",";
				do {
					line = in.readLine();
				} while (!line.contains("Period"));
				while (true)
					try {
						other = null;
						line = in.readLine();
						s = line.split(",");
						for (int i = 10; i <= 21; i++)
							if (isPlayerNumber(s[i]) && !s[1].contains("0")
                                && !s[1].contains("1"))
								teamToPlayerNamesAndNums.get(roadname).add(
                                                                           s[i] + " " + s[i + 1]);
                        
						for (int i = 22; i < s.length - 1; i++)
							if (isPlayerNumber(s[i]) && !s[1].contains("0")
                                && !s[1].contains("1"))
								teamToPlayerNamesAndNums.get(homename).add(
                                                                           s[i] + " " + s[i + 1]);
						if (s[6].equals("HOME"))
							s[6] = ABBREVS.get(homename);
						else if (s[6].equals("ROAD"))
							s[6] = ABBREVS.get(roadname);
						// System.out.print(pref1);
						/*
						 * If this is WSH's log, I want to list WSH players
						 * first; normally, road players listed first, then
						 * home. Need to switch order for home team.
						 */
                        
						bw1.write(pref1);
						bw2.write(pref2);
						other = new String[s.length];
						for (int i = 0; i <= 2; i++)
							other[i] = s[i];
						other[3] = s[3].substring(s[3].indexOf("-") + 1) + "-"
                        + s[3].substring(0, s[3].indexOf("-"));
						for (int i = 3; i <= 9; i++)
							other[i] = s[i];
						for (int i = 10; i <= 21; i++)
							other[i] = s[i + 12];
						for (int i = 22; i <= 33; i++)
							other[i] = s[i - 12];
						for (int i = 0; i < s.length; i++) {
							bw1.write(s[i] + ",");
							// System.out.print(s[i] + ",");
							bw2.write(other[i] + ",");
						}
						bw1.newLine();
						bw2.newLine();
						// System.out.println();
					} catch (NullPointerException npe2) {
						in.close();
						bw1.flush();
						bw2.flush();
						break;
					} catch (ArrayIndexOutOfBoundsException aoobe) {
						if (other == null) {
							bw1.write(pref1);
							bw2.write(pref2);
						}
						for (int i = 0; i < s.length; i++) {
							bw1.write(s[i] + ",");
							if (other != null)
								bw2.write(other[i] + ",");
							else
								bw2.write(s[i] + ",");
						}
						bw1.newLine();
						bw2.newLine();
					}
			} catch (FileNotFoundException fnfe) {
				System.out.println("No play by play for " + season + " " + g);
			} catch (NullPointerException npe) {
				System.out.println("Trouble reading " + g);
				in.close();
			}
        
		for (String st : teamToPbPWriter.keySet())
			teamToPbPWriter.get(st).close();
        
		BufferedWriter teamrostersout = null;
        
		/*
		 * The TreeSet will sort team names alphabetically. For performance
		 * reasons, used HashMap (which doesn't store in order) earlier.
		 */
		Set<String> set = new TreeSet<String>();
		for (String s2 : teamToPlayerNamesAndNums.keySet())
			set.add(s2);
		List<String> set2 = null;
		try {
			teamrostersout = new BufferedWriter(new FileWriter(new File(
                                                                        FOLDERPATHTOI + season + " rosters.txt")));
			for (String team : set) {
				/*
				 * Add all player names to a list, sort the list by player
				 * number, print the list
				 */
				set2 = new ArrayList<String>();
				for (String s2 : teamToPlayerNamesAndNums.get(team))
					set2.add(s2);
				set2 = sortNums(set2);
				teamrostersout.write(team + " (" + ABBREVS.get(team) + ")\n");
				for (int i = 0; i < set2.size(); i++) {
					line = set2.get(i);
					pref1 = line.substring(0, line.indexOf(" "));
					line = line.substring(line.indexOf(" ") + 1);
					pref2 = line.substring(0, line.indexOf(" "));
					line = line.substring(line.indexOf(" ") + 1);
					teamrostersout.write(pref1 + "\t" + pref2 + "\t" + line
                                         + "\n");
				}
				teamrostersout.newLine();
			}
			teamrostersout.close();
		} catch (IOException ioe) {
			System.out.println("Trouble creating team roster pages.");
		}
		System.out.println("Done with team pages for " + season);
	}
    
	/* This method coordinates reading single game files */
	private static void ReadPlayByPlay(BufferedReader f, BufferedWriter out,
                                       int season, int gamenum, BufferedWriter errorwriter)
    throws IOException {
		String roadname = "";
		try {
			roadname = getRoadName(f);
		} catch (NullPointerException npe) {
			System.out.println("Couldn't find play-by-play for " + gamenum);
			f.close();
			out.close();
			return;
		}
		String date = getGameDate(f);
		String attendance = "";
		/* Some corrections for missing information */
		if (gamenum == 20022 && roadname.equals("CANADIENS MONTREAL")
            && date.equals("Saturday, October 6, 2007"))
			attendance = "Attendance n/a";
		else if (gamenum == 21178 && roadname.equals("BOSTON BRUINS")
                 && date.equals("Sunday, March 30, 2008"))
			attendance = "Attendance n/a";
		else if (gamenum == 20081 && roadname.equals("PITTSBURGH PENGUINS")
                 && date.equals("Thursday, July 29, 2010"))
			attendance = "Attendance n/a";
		else
			attendance = getAttendance(f);
		String startandendtime = "";
		if (gamenum == 21178 && roadname.equals("BOSTON BRUINS")
            && date.equals("Sunday, March 30, 2008"))
			startandendtime = "Start 6:09 End n/a";
		else if (gamenum == 20081 && roadname.equals("PITTSBURGH PENGUINS")
                 && date.equals("Thursday, July 29, 2010"))
			startandendtime = "Start 3:57 End n/a";
		else
			startandendtime = getStartAndEndTime(f);
		String homename = getHomeName(f);
		/* Write headers in output file */
		out.write("Game " + gamenum + ": " + roadname + "@" + homename);
		out.newLine();
		out.write(date);
		out.newLine();
		out.write(startandendtime);
		out.newLine();
		out.write(attendance);
		out.newLine();
		out.write("Period,Strength,Time,Score,Event,Team,Zone,Actor,Recipient,Note,");
		out.write("Road1,Road1Name,Road2,Road2Name,Road3,Road3Name,");
		out.write("Home4,Road4Name,Road5,Road5Name,Road6,Road6Name,");
		out.write("Home1,Home1Name,Home2,Home2Name,Home3,Home3Name,");
		out.write("Home4,Home4Name,Home5,Home5Name,Home6,Home6Name,");
		out.newLine();
		/*
		 * errorwriter .write(
		 * "Game,Period,Strength,Time,Score,Event,Team,Zone,Actor,Recipient,Note,"
		 * );
		 * errorwriter.write("Road1,Road1Name,Road2,Road2Name,Road3,Road3Name,"
		 * );
		 * errorwriter.write("Home4,Road4Name,Road5,Road5Name,Road6,Road6Name,"
		 * );
		 * errorwriter.write("Home1,Home1Name,Home2,Home2Name,Home3,Home3Name,"
		 * );
		 * errorwriter.write("Home4,Home4Name,Home5,Home5Name,Home6,Home6Name,"
		 * );
		 */
		readPlays(f, out, homename, gamenum, errorwriter);
		// errorwriter.close();
	}
    
	/*
	 * Reads the file until it comes across a team name. Should be the road team
	 * name.
	 */
	private static String getRoadName(BufferedReader f) throws IOException,
    NullPointerException {
		String s;
		do {
			s = f.readLine();
		} while (!s.contains("Game "));
		s = s.substring(s.indexOf(">") + 1);
		return s.substring(0, s.indexOf("<"));
	}
    
	/* Reads the file until it comes across game date. */
	private static String getGameDate(BufferedReader f) throws IOException {
		String s;
		do {
			s = f.readLine();
		} while (!s.contains(" 20"));
		s = s.substring(s.indexOf(">") + 1);
		return s.substring(0, s.indexOf("<"));
	}
    
	/*
	 * Reads the file until it comes across attendance.
	 */
	private static String getAttendance(BufferedReader f) throws IOException,
    NullPointerException {
		String s;
		do {
			s = f.readLine();
		} while (!s.contains("Att"));
		s = s.substring(s.indexOf(">") + 1);
		return s.substring(0, s.indexOf("<"));
	}
    
	/*
	 * Reads the file until it comes across game start and end times.
	 */
	private static String getStartAndEndTime(BufferedReader f)
    throws IOException {
		String s;
		String[] times = new String[2];
		do {
			s = f.readLine();
		} while (!s.contains("Start"));
		s = s.substring(s.indexOf("Start"));
		s = s.substring(s.indexOf(";") + 1);
		times[0] = "Start " + s.substring(0, s.indexOf("&"));
		while (!s.contains("End"))
			s = f.readLine();
		s = s.substring(s.indexOf("End"));
		s = s.substring(s.indexOf(";") + 1);
		times[1] = "End " + s.substring(0, s.indexOf("&"));
		return times[0] + " " + times[1];
	}
    
	/*
	 * Reads the file until it comes across a team name. Should be the home team
	 * name.
	 */
	private static String getHomeName(BufferedReader f) throws IOException {
		String s;
		do {
			s = f.readLine();
		} while (!s.contains("Game "));
		do {
			s = f.readLine();
		} while (!s.contains("Game "));
		s = s.substring(s.indexOf(">") + 1);
		return s.substring(0, s.indexOf("<"));
	}
    
	/* Reads play by play events */
	private static void readPlays(BufferedReader f, BufferedWriter out,
                                  String homename, int gamenum, BufferedWriter errorwriter)
    throws IOException {
		String s;
		String toadd = null;
		int homescore = 0;
		int roadscore = 0;
		do {
			s = f.readLine();
		} while (!s.contains("<tr class=" + '"'));
		/*
		 * evenColor or oddColor; signal for beginning of play by play
		 */
		while (true)
			try {
				toadd = readNextPlay(f, s, homename, homescore, roadscore);
				if (toadd.equals("-1"))
					return;
				if (toadd.contains("GOAL") && toadd.contains("HOME"))
					homescore++;
				else if (toadd.contains("GOAL") && toadd.contains("ROAD"))
					roadscore++;
				out.write(toadd);
				/*
				 * if (strength.contains("0") && !period.equals("5"))
				 * errorwriter.write(gamenum + "," + toadd + "\n");
				 */
				out.newLine();
			} catch (NullPointerException npe) {
				out.flush();
				return;
			}
	}
    
	public static String readNextPlay(BufferedReader f, String s,
                                      String homename, int homescore, int roadscore) throws IOException {
		String period;
		String strength; // will calculate from array. Actor's perspective
		String time;
		String event;
		String team;
		String zone;
		String actor;
		String recipient;
		String note;
		String currentHomeGoalie = "";
		String currentRoadGoalie = "";
		int curHomeScore = homescore;
		int curRoadScore = roadscore;
		int teamacting = 0; // 1 if home, -1 if road
		String toadd;
		String[] players;
        
		if (s.contains("pageBreakBefore"))
			s = f.readLine();
		while (!s.contains("<tr class=" + '"'))
			s = f.readLine();
		f.readLine(); // event number
        
		s = f.readLine(); // period number
		s = s.substring(s.indexOf(">") + 1);
        
		try {
			period = s.substring(0, s.indexOf("<"));
		} catch (StringIndexOutOfBoundsException sioobe) {
			while (!s.contains("evenColor") && !s.contains("oddColor"))
				s = f.readLine();
			s = f.readLine(); // event number
			s = f.readLine(); // period number
			s = s.substring(s.indexOf(">") + 1);
			period = s.substring(0, s.indexOf("<"));
		}
		f.readLine();
        
		s = f.readLine(); // event time
		s = s.substring(s.indexOf(">") + 1);
		time = s.substring(0, s.indexOf("<"));
        
		s = f.readLine(); // event type
		s = s.substring(s.indexOf(">") + 1);
		event = s.substring(0, s.indexOf("<"));
        
		team = "";
		zone = "";
		actor = "";
		recipient = "";
		note = "";
        
		if (event.equals("STOP")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			note = s.substring(0, s.indexOf("<"));
			actor = "";
			recipient = "";
			team = "";
			zone = "";
		} else if (event.equals("PSTR")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			note = s.substring(0, s.indexOf("<"));
			actor = "";
			recipient = "";
			team = "";
			zone = "";
		} else if (event.equals("FAC")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			note = "";
			if (ABBREVS.get(homename).equals(s.substring(0, 3))) {
				teamacting = 1;
				s = s.substring(1);
			} else if (ABBREVS.containsKey(s.substring(0, 3))) {
				teamacting = -1;
				s = s.substring(1);
			}
			if (teamacting == 1) {
				recipient = s.substring(s.indexOf("#") + 1, s.indexOf("#") + 3)
                .trim();
				String s3 = s.substring(s.indexOf(" vs "));
				actor = s3.substring(
                                     s3.indexOf(" " + ABBREVS.get(homename) + " ") + 6,
                                     s3.indexOf(" " + ABBREVS.get(homename) + " ") + 8)
                .trim();
				if (actor.charAt(0) == '#')
					actor = s3.substring(
                                         s3.indexOf(" " + ABBREVS.get(homename) + " ") + 7,
                                         s3.indexOf(" " + ABBREVS.get(homename) + " ") + 9)
                    .trim();
				if (actor.equals(recipient))
					recipient = s.substring(s.lastIndexOf("#") + 1,
                                            s.lastIndexOf("#") + 3).trim();
				try {
					int n = Integer.parseInt(actor);
				} catch (NumberFormatException nfe) {
					String temp = s.substring(s.indexOf(" "
                                                        + ABBREVS.get(homename) + " ") + 1);
					actor = temp
                    .substring(
                               temp.indexOf(" " + ABBREVS.get(homename)
											+ " ") + 6,
                               temp.indexOf(" " + ABBREVS.get(homename)
											+ " ") + 8).trim();
					// System.out.println();
				}
				team = "HOME";
			} else if (teamacting == -1) {
				actor = s.substring(s.indexOf("#") + 1, s.indexOf("#") + 3)
                .trim();
				String s3 = s.substring(s.indexOf(" vs "));
				recipient = s3.substring(
                                         s3.indexOf(" " + ABBREVS.get(homename) + " ") + 6,
                                         s3.indexOf(" " + ABBREVS.get(homename) + " ") + 8)
                .trim();
				if (recipient.charAt(0) == '#')
					recipient = s3.substring(
                                             s3.indexOf(" " + ABBREVS.get(homename) + " ") + 7,
                                             s3.indexOf(" " + ABBREVS.get(homename) + " ") + 9)
                    .trim();
				if (actor.equals(recipient))
					actor = s.substring(s.lastIndexOf("#") + 1,
                                        s.lastIndexOf("#") + 3).trim();
				team = "ROAD";
			} else {
				actor = s.substring(s.indexOf("#") + 1, s.indexOf("#") + 3)
                .trim();
				recipient = s.substring(s.lastIndexOf("#") + 1,
                                        s.lastIndexOf("#") + 3).trim();
				zone = s.substring(0, 3);
			}
			s = s.substring(4);
			if (s.indexOf(".") != -1)
				zone = s.substring(s.indexOf(".") - 3, s.indexOf("."));
            
		} else if (event.equals("HIT")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			note = "";
			if (ABBREVS.get(homename).equals(s.substring(0, 3))) {
				team = "HOME";
				teamacting = 1;
			} else {
				team = "ROAD";
				teamacting = -1;
			}
			s = s.substring(s.indexOf("#") + 1);
			actor = s.substring(0, 2).trim();
			s = s.substring(s.indexOf("#") + 1);
			recipient = s.substring(0, 2).trim();
			if (s.indexOf("Zone") != -1)
				zone = s.substring(s.indexOf("Zone") - 5, s.indexOf("Zone") - 2);
            
		} else if (event.equals("GOAL")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			if (ABBREVS.get(homename).equals(s.substring(0, 3))) {
				team = "HOME";
				teamacting = 1;
				// if (!time.equals("0:00"))
				// curHomeScore++;
			} else {
				team = "ROAD";
				teamacting = -1;
				// if (!time.equals("0:00"))
				// curRoadScore++;
				// messes up close/tied calculations to update now.
			}
			s = s.substring(s.indexOf("#") + 1);
			actor = s.substring(0, 2).trim();
			if (team.equals("HOME"))
				recipient = currentRoadGoalie;
			else
				recipient = currentHomeGoalie;
			zone = "Off";
			s = s.substring(s.indexOf(",") + 1);
			if (s.indexOf(",") != -1)
				note = s.substring(0, s.indexOf(",")).trim();
			if (s.indexOf(",") != -1)
				s = s.substring(s.indexOf(",") + 1);
			if (s.indexOf(",") != -1)
				s = s.substring(s.indexOf(",") + 1);
			if (s.indexOf(".") != -1)
				note = note + " " + s.substring(0, s.indexOf(".")).trim();
			s = s.substring(s.indexOf(">") + 1);
			if (s.indexOf("<") != -1)
				note = note + " " + s.substring(0, s.indexOf("<"));
            
		} else if (event.equals("MISS") || event.equals("POST")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			actor = s.substring(s.indexOf("#") + 1, s.indexOf("#") + 3).trim();
			if (ABBREVS.get(homename).equals(s.substring(0, 3)))
				teamacting = 1;
			else
				teamacting = -1;
			if (teamacting == 1)
				team = "HOME";
			else
				team = "ROAD";
			if (team.equals("HOME"))
				recipient = currentRoadGoalie;
			else
				recipient = currentHomeGoalie;
			zone = "Off";
			s = s.substring(s.indexOf(",") + 1);
			if (s.indexOf(",") != -1)
				note = s.substring(0, s.indexOf(",")).trim() + " ";
			if (s.indexOf("Zone") != -1)
				s = s.substring(s.indexOf("Zone") + 5);
			note = note + s.substring(0, s.indexOf("<")).trim();
            
		} else if (event.equals("BLOCK")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			if (ABBREVS.get(homename).equals(s.substring(0, 3)))
				teamacting = +1;
			else
				teamacting = -1;
			if (teamacting == 1)
				team = "HOME";
			else
				team = "ROAD";
			s = s.substring(s.indexOf("#") + 1);
			actor = s.substring(0, 2).trim();
			s = s.substring(s.indexOf("#") + 1);
			recipient = s.substring(0, 2).trim();
			s = s.substring(s.indexOf(",") + 1);
			if (s.indexOf(",") != -1)
				note = s.substring(0, s.indexOf(",")).trim();
			zone = "OFF";
            
		} else if (event.equals("SHOT")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			if (!s.contains("#"))
				if (s.charAt(s.indexOf("-") + 3) == ' ')
					actor = s.substring(s.indexOf("-") + 1, s.indexOf("-") + 3)
                    .trim();
				else
					actor = s.substring(s.indexOf("-") + 1, s.indexOf("-") + 4)
                    .trim();
                else
                    actor = s.substring(s.indexOf("#") + 1, s.indexOf("#") + 3)
                    .trim();
			if (ABBREVS.get(homename).equals(s.substring(0, 3)))
				teamacting = 1;
			else
				teamacting = -1;
			if (teamacting == 1)
				team = "HOME";
			else
				team = "ROAD";
			if (team.equals("HOME"))
				recipient = currentRoadGoalie;
			else
				recipient = currentHomeGoalie;
			zone = "Off";
			s = s.substring(s.indexOf(",") + 1);
			if (s.contains("post"))
				event = "POST";
			note = s.substring(0, s.indexOf(",")).trim() + " ";
			s = s.substring(s.indexOf("Zone") + 5);
			note = note + s.substring(0, s.indexOf("<")).trim();
            
		} else if (event.equals("PENL")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			if (ABBREVS.get(homename).equals(s.substring(0, 3)))
				teamacting = 1;
			else
				teamacting = -1;
			if (teamacting == 1)
				team = "HOME";
			else
				team = "ROAD";
			actor = s.substring(s.indexOf("#") + 1, s.indexOf("#") + 3).trim();
			s = s.substring(s.indexOf(";") + 1);
			note = s.substring(0, s.indexOf(")") + 1);
			s = s.substring(s.indexOf(",") + 1);
			if (s.contains("Off."))
				zone = "Off";
			else if (s.contains("Neu."))
				zone = "Neu";
			if (s.contains("Def."))
				zone = "Def";
			else
				zone = "n/a";
			try {
				s = s.substring(s.indexOf(":"));
				recipient = s.substring(s.indexOf("#") + 1, s.indexOf("#") + 3)
                .trim();
			} catch (Exception e) {
				recipient = "";
			}
            
		} else if (event.equals("TAKE")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			if (ABBREVS.get(homename).equals(s.substring(0, 3)))
				teamacting = 1;
			else
				teamacting = -1;
			if (teamacting == 1)
				team = "HOME";
			else
				team = "ROAD";
			note = "";
			s = s.substring(s.indexOf("#") + 1);
			actor = s.substring(0, 2).trim();
			recipient = "";
			s = s.substring(s.indexOf(",") + 1);
			if (s.indexOf(".") != -1)
				zone = s.substring(0, s.indexOf(".")).trim();
			else
				zone = "";
            
		} else if (event.equals("GIVE")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			if (ABBREVS.get(homename).equals(s.substring(0, 3)))
				teamacting = 1;
			else
				teamacting = -1;
			if (teamacting == 1)
				team = "HOME";
			else
				team = "ROAD";
			note = "";
			s = s.substring(s.indexOf("#") + 1);
			actor = s.substring(0, 2).trim();
			recipient = "";
			s = s.substring(s.indexOf(",") + 1);
			if (s.indexOf(".") != -1)
				zone = s.substring(0, s.indexOf(".")).trim();
            
		} else if (event.equals("PEND")) {
			s = f.readLine();
			s = s.substring(s.indexOf(">") + 1);
			note = s.substring(0, s.indexOf("<"));
			actor = "";
			recipient = "";
			team = "";
			zone = "";
            
		} else if (event.equals("SOC") || event.equals("GEND")
                   || event.equals("GOFF")) {
			return "-1";
		} else if (event.equals("EISTR") || event.equals("EIEND")) {
			note = "Video review";
		} else {
			System.out.println(homename + " " + event + " " + period + " "
                               + time);
		}
		players = new String[24];
		for (int i = 0; i < players.length; i++)
			players[i] = " ";
        
		int i = 0;
		boolean homegoaliepulled = true;
		boolean roadgoaliepulled = true;
		String temp;
		while (!s.contains("<tr class=" + '"'))
			if (s.contains("<td class=") && i != 0 && i < 12)
				i = 12;
			else if (s.contains(" title=")) {
				s = s.substring(s.lastIndexOf("=") + 2);
				if (i >= players.length) {
					while (!s.contains("<tr class=" + '"'))
						s = f.readLine();
					break;
				}
				temp = s.substring(s.indexOf(">") + 1, s.indexOf("<"));
				String pos = s.substring(0, s.indexOf("-")).trim();
				/* Add player positions */
				if (pos.equalsIgnoreCase("Center"))
					players[i] = temp + " C";
				else if (pos.equalsIgnoreCase("Left Wing"))
					players[i] = temp + " L";
				if (pos.equalsIgnoreCase("Right wing"))
					players[i] = temp + " R";
				if (pos.equalsIgnoreCase("Defense"))
					players[i] = temp + " D";
				if (pos.equalsIgnoreCase("Goalie"))
					if (i <= 14) {
						players[10] = temp + " G";
						i = 10;
					} else {
						players[22] = temp + " G";
						i = 22;
					}
				players[i + 1] = s.substring(s.indexOf("-") + 1,
                                             s.indexOf(">") - 1).trim();
				if (s.contains("Goalie") && i <= 11) {
					roadgoaliepulled = false;
					currentRoadGoalie = players[i];
				} else if (s.contains("Goalie") && i > 11) {
					homegoaliepulled = false;
					currentHomeGoalie = players[i];
				}
				i += 2;
			} else
				s = f.readLine();
        
		int homestrength = 5;
		int roadstrength = 5;
		for (int k = 0; k < 6; k++)
			if (players[k * 2].equals(" "))
				roadstrength--;
		for (int k = 6; k < 12; k++)
			if (players[k * 2].equals(" "))
				homestrength--;
		if (homegoaliepulled)
			homestrength++;
		if (roadgoaliepulled)
			roadstrength++;
		if (teamacting >= 0)
			strength = homestrength + "v" + roadstrength;
		else
			strength = roadstrength + "v" + homestrength;
		if (event.equals("PSTR")) {
			players = new String[1];
			players[0] = "";
			strength = "0v0";
		}
		while (note.contains(","))
			note = note.substring(0, note.indexOf(","))
            + note.substring(note.indexOf(",") + 1, note.length());
		toadd = period + "," + strength + "," + time + "," + curHomeScore + "-"
        + curRoadScore + "," + event + "," + team + "," + zone + ","
        + actor + "," + recipient + "," + note;
		for (int k = 0; k < players.length; k++)
			toadd = toadd + "," + players[k];
		// System.out.println(toadd);
		return toadd;
	}
    
	/*
	 * Generates a map to convert abbreviations to full team names, and vice
	 * versa, save MTL-->CANADIENS MONTREAL (since MTL is already linked to
	 * MONTREAL CANADIENS).
	 */
	public static Map<String, String> getTeamToAbbrevMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("WSH", "WASHINGTON CAPITALS");
		map.put("ATL", "ATLANTA THRASHERS");
		map.put("CAR", "CAROLINA HURRICANES");
		map.put("T.B", "TAMPA BAY LIGHTNING");
		map.put("WPG", "WINNIPEG JETS");
		map.put("FLA", "FLORIDA PANTHERS");
		map.put("PIT", "PITTSBURGH PENGUINS");
		map.put("PHI", "PHILADELPHIA FLYERS");
		map.put("NYI", "NEW YORK ISLANDERS");
		map.put("NYR", "NEW YORK RANGERS");
		map.put("N.J", "NEW JERSEY DEVILS");
		map.put("BUF", "BUFFALO SABRES");
		map.put("TOR", "TORONTO MAPLE LEAFS");
		map.put("OTT", "OTTAWA SENATORS");
		map.put("MTL", "MONTREAL CANADIENS");
		map.put("BOS", "BOSTON BRUINS");
		map.put("CHI", "CHICAGO BLACKHAWKS");
		map.put("DET", "DETROIT RED WINGS");
		map.put("STL", "ST. LOUIS BLUES");
		map.put("NSH", "NASHVILLE PREDATORS");
		map.put("CBJ", "COLUMBUS BLUE JACKETS");
		map.put("EDM", "EDMONTON OILERS");
		map.put("CGY", "CALGARY FLAMES");
		map.put("VAN", "VANCOUVER CANUCKS");
		map.put("COL", "COLORADO AVALANCHE");
		map.put("MIN", "MINNESOTA WILD");
		map.put("L.A", "LOS ANGELES KINGS");
		map.put("DAL", "DALLAS STARS");
		map.put("PHX", "PHOENIX COYOTES");
		map.put("S.J", "SAN JOSE SHARKS");
		map.put("ANA", "ANAHEIM DUCKS");
		Map<String, String> map2 = new HashMap<String, String>();
		for (String s : map.keySet()) {
			map2.put(s, map.get(s));
			map2.put(map.get(s), s);
		}
		map2.put("CANADIENS MONTREAL", "MTL");
		return map2;
	}
    
	private static Map<String, String> getScheduleAbbreviationsMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("WSH", "WASHINGTON CAPITALS");
		map.put("ATL", "ATLANTA THRASHERS");
		map.put("CAR", "CAROLINA HURRICANES");
		map.put("T.B", "TAMPA BAY LIGHTNING");
		map.put("TBL", "TAMPA BAY LIGHTNING");
		map.put("WPG", "WINNIPEG JETS");
		map.put("FLA", "FLORIDA PANTHERS");
		map.put("PIT", "PITTSBURGH PENGUINS");
		map.put("PHI", "PHILADELPHIA FLYERS");
		map.put("NYI", "NEW YORK ISLANDERS");
		map.put("NYR", "NEW YORK RANGERS");
		map.put("N.J", "NEW JERSEY DEVILS");
		map.put("NJD", "NEW JERSEY DEVILS");
		map.put("BUF", "BUFFALO SABRES");
		map.put("TOR", "TORONTO MAPLE LEAFS");
		map.put("OTT", "OTTAWA SENATORS");
		map.put("MTL", "MONTREAL CANADIENS");
		map.put("BOS", "BOSTON BRUINS");
		map.put("CHI", "CHICAGO BLACKHAWKS");
		map.put("DET", "DETROIT RED WINGS");
		map.put("STL", "ST. LOUIS BLUES");
		map.put("NSH", "NASHVILLE PREDATORS");
		map.put("CBJ", "COLUMBUS BLUE JACKETS");
		map.put("EDM", "EDMONTON OILERS");
		map.put("CGY", "CALGARY FLAMES");
		map.put("VAN", "VANCOUVER CANUCKS");
		map.put("COL", "COLORADO AVALANCHE");
		map.put("MIN", "MINNESOTA WILD");
		map.put("L.A", "LOS ANGELES KINGS");
		map.put("LAK", "LOS ANGELES KINGS");
		map.put("DAL", "DALLAS STARS");
		map.put("PHX", "PHOENIX COYOTES");
		map.put("S.J", "SAN JOSE SHARKS");
		map.put("SJS", "SAN JOSE SHARKS");
		map.put("ANA", "ANAHEIM DUCKS");
		map.put("ASW", "WESTERN CONFERENCE ALL-STARS");
		map.put("ASE", "EASTERN CONFERENCE ALL-STARS");
		Map<String, String> map2 = new HashMap<String, String>();
		for (String s : map.keySet()) {
			map2.put(s, map.get(s));
			map2.put(map.get(s), s);
		}
		map2.put("CANADIENS MONTREAL", "MTL");
		return map2;
	}
    
	/* Checks if this string starts with a player number. */
	private static boolean isPlayerNumber(String s) {
		char st = 'a';
		if (s.length() < 3)
			return false;
		try {
			st = s.charAt(s.length() - 1);
		} catch (StringIndexOutOfBoundsException sioobe) {
			System.out.println(s);
		}
		if (st != 'G' && st != 'D' && st != 'L' && st != 'R' && st != 'C')
			return false;
		try {
			Integer.parseInt(s.substring(0, 2).trim());
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
    
	/* Sorts a List by the leading player number of each element in the list. */
	public static List<String> sortNums(List<String> list) {
		// selection sort
		int min;
		int imin;
		int x;
		String temp = null;
		for (int i = 0; i < list.size(); i++) {
			min = Integer.parseInt(list.get(i).substring(0,
                                                         list.get(i).indexOf(' ')));
			imin = i;
			for (int j = i + 1; j < list.size(); j++) {
				x = Integer.parseInt(list.get(j).substring(0,
                                                           list.get(j).indexOf(' ')));
				if (x < min) {
					imin = j;
					min = x;
				}
			}
			temp = list.get(i);
			list.set(i, list.get(imin));
			list.set(imin, temp);
		}
		return list;
	}
    
	/*
	 * Reads the roster file for the season to generate a Map of player names to
	 * positions.
	 */
	public static Map<Integer, String> readSchedule(int season) {
		Map<Integer, String> schedule = new HashMap<Integer, String>();
		BufferedReader f = null;
		String[] s;
		try {
			f = new BufferedReader(new FileReader(FOLDERPATHPBP + season
                                                  + " schedule.csv"));
			f.readLine();
			while (true)
				try {
					s = f.readLine().split(",");
					schedule.put(Integer.parseInt(s[0]), s[1] + s[3] + s[4]);
				} catch (NullPointerException npe) {
					f.close();
					break;
				}
		} catch (IOException ioe) {
			System.out.println("Trouble reading schedule");
		}
		return schedule;
	}
    
	private static void readTOI(int startseason, int endseason, int startgame,
                                int endgame) {
		URL visitorTOIurl = null;
		URL homeTOIurl = null;
		String[][] temp;
		int maxtime = 3901;
		BufferedReader f;
		BufferedWriter out;
		Map<Integer, String> schedule;
		String home;
		String road;
		String line;
		// Map<Integer, Map<String, Set<Integer>>> seasonToTeamToGoalies =
		// determineGoalies();
		File file;
		String[] s;
		for (int season = startseason; season <= endseason; season++) {
			playerNameToPos = getPlayerPositionMap(season);
			int limit = 21230;
			if (season == endseason)
				limit = endgame;
			if (season == 2012 && endgame > 720)
				limit = 20720;
			schedule = readSchedule(season);
			file = new File(FOLDERPATHTOI + season + "/");
			if (!file.exists())
				if (!file.mkdir()) {
					System.out
                    .println("Couldn't create toi folder for this season. Did you move or rename folders around on your computer?");
				}
			for (int gamenum = startgame; gamenum <= limit; gamenum++)
            /*
             * if (!(new File(file.toString() + "/" + gamenum +
             * " shifts.csv") .exists()))
             */
				try {
					try {
						visitorTOIurl = new URL(
                                                "http://www.nhl.com/scores/htmlreports/"
                                                + season + (season + 1) + "/TV0"
                                                + gamenum + ".HTM");
					} catch (MalformedURLException me) {
						System.out
                        .println("Couldn't find http://www.nhl.com/scores/htmlreports/"
                                 + season
                                 + (season + 1)
                                 + "/TV0"
                                 + gamenum + ".HTM");
					}
					try {
						homeTOIurl = new URL(
                                             "http://www.nhl.com/scores/htmlreports/"
                                             + season + (season + 1) + "/TH0"
                                             + gamenum + ".HTM");
					} catch (MalformedURLException me) {
						System.out
                        .println("Couldn't find http://www.nhl.com/scores/htmlreports/"
                                 + season
                                 + (season + 1)
                                 + "/TH0"
                                 + gamenum + ".HTM");
					}
                    
					temp = new String[maxtime][24];
					line = schedule.get(gamenum);
					home = ABBREVS.get(line.substring(line.indexOf("@") + 1));
					road = ABBREVS.get(line.substring(0, line.indexOf("@")));
					BufferedReader scorereader = null;
					List<String> scorebytime = new ArrayList<String>();
					scorebytime.add("0-0");
					scorebytime.add("0-0");
					try {
						try {
							scorereader = new BufferedReader(new FileReader(
                                                                            getGameFileNamePbP(season, gamenum)));
							line = scorereader.readLine();
							while (!line.contains("Period"))
								line = scorereader.readLine();
						} catch (NullPointerException npe) {
							scorebytime = new ArrayList<String>();
							while (scorebytime.size() <= 3601)
								scorebytime.add("n/a");
						} catch (FileNotFoundException fnfe) {
							scorebytime = new ArrayList<String>();
							while (scorebytime.size() <= 3601)
								scorebytime.add("n/a");
						}
						while (true)
							try {
								line = scorereader.readLine();
								s = line.split(",");
								int time = 0;
								try {
									int per = Integer.parseInt(s[0]);
									int min = Integer.parseInt(s[2].substring(
                                                                              0, s[2].indexOf(":")));
									int sec = Integer.parseInt(s[2]
                                                               .substring(s[2].indexOf(":") + 1));
									time = 1200 * (per - 1) + 60 * min + sec;
								} catch (NumberFormatException nfe) {
									time = 1200 * 3 + 301;
								}
								for (int t = scorebytime.size(); t <= time + 1; t++)
									scorebytime.add(s[3]);
							} catch (NullPointerException npe) {
								if (scorereader != null)
									scorereader.close();
								while (scorebytime.size() <= 3600)
									scorebytime.add(scorebytime.get(scorebytime
                                                                    .size() - 1));
								break;
							}
					} catch (IOException ioe) {
						System.out.println("error: " + season + "\t" + gamenum
                                           + "\t" + ioe);
					}
					for (int r = 0; r < temp.length; r++)
						for (int c = 0; c < temp[0].length; c++)
							temp[r][c] = "";
					try {
						f = new BufferedReader(new InputStreamReader(
                                                                     homeTOIurl.openStream()));
						readShiftDataNames(f, "home", temp, "road");
                        
						f = new BufferedReader(new InputStreamReader(
                                                                     visitorTOIurl.openStream()));
						readShiftDataNames(f, "visitor", temp, "road");
                        
						out = new BufferedWriter(new FileWriter(
                                                                new File(file.toString() + "/" + gamenum
                                                                         + " shifts.csv")));
						out.write("Time(sec),Score,Strength");
						for (int i = 1; i <= 6; i++)
							out.write("," + road + i + "," + road + i + "Name");
						for (int i = 1; i <= 6; i++)
							out.write("," + home + i + "," + home + i + "Name");
						out.newLine();
						for (int r = 1; r < temp.length; r++) {
							String toprint = "" + r + ",";
							try {
								toprint = "" + r + "," + scorebytime.get(r - 1);
							} catch (IndexOutOfBoundsException ioobe) {
								System.out.println();
							}
							if (temp[r][6].equals(""))
								break;
							boolean homegoaliein = false;
							boolean roadgoaliein = false;
							int homestrength = 6;
							int roadstrength = 6;
							int cutroad = 0;
							int cuthome = 0;
							for (int c = 0; c < temp[0].length; c++) {
								if (temp[r][c].length() > 2
                                    && temp[r][c].charAt(0) == 'G'
                                    && c <= 11)
									roadgoaliein = true;
								else if (temp[r][c].length() > 2
                                         && temp[r][c].charAt(0) == 'G'
                                         && c > 11)
									homegoaliein = true;
								else if (temp[r][c].equals("") && c <= 11)
									cutroad++;
								else if (temp[r][c].equals("") && c > 11)
									cuthome++;
							}
							if (homegoaliein)
								homestrength--;
							if (roadgoaliein)
								roadstrength--;
							homestrength = homestrength - cuthome / 2;
							roadstrength = roadstrength - cutroad / 2;
							toprint = toprint + "," + roadstrength + "v"
                            + homestrength;
							for (int c = 0; c < temp[0].length; c++)
								if (temp[r][c].equals(""))
									toprint = toprint + ",";
								else
									toprint = toprint + "," + temp[r][c];
							out.write(toprint);
							out.newLine();
						}
						out.close();
						if (gamenum % 100 == 0)
							System.out.println("Done with toi for " + season
                                               + " through " + gamenum);
					} catch (NullPointerException npe) {
						System.out.println("Trouble with " + season + " "
                                           + gamenum + " " + npe);
					} catch (FileNotFoundException fnfe) {
						System.out.println("Trouble with " + season + " "
                                           + gamenum + " " + fnfe);
					} catch (MalformedURLException murle) {
						System.out.println(murle);
					} catch (IOException ioe) {
						System.out.println(ioe);
					}
					// System.out.println("Done with " + season + " " +
					// gamenum);
				} catch (NullPointerException npee) {
					System.out.println(season + " " + gamenum + " not found");
				}
			try {
				generateTeamTOIPages(season, limit);
			} catch (IOException ioe) {
				System.out.println("Trouble creating team toi pages for "
                                   + season);
			}
		}
	}
    
	/* A method to determine goalies using rosters. Not used. */
    
	public static Map<Integer, Map<String, Set<Integer>>> determineGoalies() {
        
		Map<Integer, Map<String, Set<Integer>>> seasonToTeamToGoalies = new HashMap<Integer, Map<String, Set<Integer>>>();
		Map<String, Set<Integer>> thisseason;
		Set<Integer> currentteam = null;
		BufferedReader rosterreader = null;
		String line;
		String thisteam = null;
		for (int season = 2007; season <= 2012; season++)
			try {
				rosterreader = new BufferedReader(new FileReader(FOLDERPATHTOI
                                                                 + season + " rosters.txt"));
				thisseason = new HashMap<String, Set<Integer>>();
				while (true)
					try {
						line = rosterreader.readLine();
						if (line.contains("(")
                            && line.indexOf(")") - line.indexOf("(") == 4) {
							if (thisteam != null)
								thisseason.put(thisteam, currentteam);
							currentteam = new HashSet<Integer>();
							thisteam = line.substring(line.indexOf("(") + 1,
                                                      line.indexOf(")"));
						} else if (line.contains("\tG\t")) {
							currentteam.add(Integer.parseInt(line.substring(0,
                                                                            line.indexOf("\tG\t"))));
						}
                        
					} catch (NullPointerException npe) {
						rosterreader.close();
						break;
					} catch (IOException ioe) {
						System.out.println(ioe);
					}
				seasonToTeamToGoalies.put(season, thisseason);
                
			} catch (IOException ioe) {
				System.out.println(season + "\n" + ioe);
			}
		return seasonToTeamToGoalies;
	}
    
	/*
	 * Read players on ice for each second. Similar to readShiftData() in
	 * TeamTables, but this method reads player names as well
	 */
    
	public static void readShiftDataNames(BufferedReader f,
                                          String homeOrVisitor, String[][] playersOnIce, String teamoffocus) {
        
		String vName = null; // will read from TOI log
		String hName = null; // will read from TOI log
        
		boolean lookingAtHomeTeam = homeOrVisitor.equalsIgnoreCase("home");
		// Get road team name
		String line = "";
		try {
			while (!line.contains("Game"))
				line = f.readLine();
			vName = line.substring(line.indexOf('>') + 1,
                                   line.indexOf("Game") - 4);
			if (vName.contains("br>Ma"))
				vName = vName.substring(0, vName.length() - 6);
            
			// Get home team name
			line = f.readLine();
			while (!line.contains("Game"))
				// gets to "GameInfo"
				line = f.readLine();
			line = f.readLine();
			while (!line.contains("Game"))
				// gets to game number
				line = f.readLine();
			line = f.readLine();
			while (!line.contains("Game"))
				line = f.readLine();
			try {
				hName = line.substring(line.indexOf('>') + 1,
                                       line.indexOf("Game") - 4);
			} catch (StringIndexOutOfBoundsException sioobe) {
				do {
					line = f.readLine();
				} while (!line.contains("Game"));
				hName = line.substring(line.indexOf('>') + 1,
                                       line.indexOf("Game") - 4);
			}
			if (hName.contains("br>Ma"))
				hName = hName.substring(0, hName.length() - 6);
			line = f.readLine();
            
			while (!line.contains("playerHeading") && !line.contains(vName)
                   && !line.contains(hName))
				line = f.readLine();
		} catch (IOException ioe) {
			System.out.println("Unusual format in NHL.com TOI report");
			System.exit(0);
		}
        
		// cycle through players
		while (true)
			try {
				int currentPlayerNum;
				int period;
				int shiftStart;
				int shiftEnd;
				String currentplayername;
                
				// Go to player
				while (!line.contains("playerHeading"))
					line = f.readLine();
                
				// Get player name and number, add to Map.
                
				line = line.substring(line.indexOf(">") + 1, line.length() - 5);
				if (line.length() <= 5) {
					// blank player names, happened twice
					do {
						line = f.readLine();
					} while (!line.contains("playerHeading"));
					line = line.substring(line.indexOf(">") + 1,
                                          line.length() - 5);
				}
				try {
					currentPlayerNum = Integer.parseInt(line.substring(0, 2)
                                                        .trim());
					currentplayername = (line.substring(line.indexOf(",") + 1,
                                                        line.length()) + " " + line.substring(
                                                                                              line.indexOf(" ") + 1, line.indexOf(","))).trim();
					// System.out.println(currentPlayerNum);
				} catch (NumberFormatException nfe) {
					do {
						line = f.readLine();
					} while (!line.contains("playerHeading"));
					line = line.substring(line.indexOf(">") + 1,
                                          line.length() - 5);
					currentPlayerNum = Integer.parseInt(line.substring(0, 2)
                                                        .trim());
					currentplayername = line.substring(line.indexOf(","),
                                                       line.indexOf(" ")).trim();
				}
				if (playerNameToPos.containsKey(currentplayername))
					currentplayername = playerNameToPos.get(currentplayername)
                    + " " + currentplayername;
				// System.out.println(line);
                
				// Scan through shifts
				while (true) {
					line = f.readLine();
					if (line.contains("playerHeading"))
                    /*
                     * indicator for a new player
                     */
						break;
					else if (line.contains("SHF"))
                    /*
                     * indicator for total TOI summary for player, which we
                     * don't need and should skip
                     */
						break;
					else if (line.contains("oddColor")
                             || line.contains("evenColor")) {
						/*
						 * indicator for a new shift
						 */
                        
						f.readLine();
						/*
						 * this line contains the shift number
						 */
                        
						line = f.readLine();
						/*
						 * this line contains the period number
						 */
						String periodstring = line.substring(
                                                             line.indexOf(">") + 1, line.length() - 5);
						if (periodstring.equals("OT"))
							period = 4;
						else
							period = Integer.parseInt(periodstring);
						// get period number
                        
						line = f.readLine();
						/*
						 * this line contains the shift's starting time
						 */
						line = line.substring(line.indexOf(">") + 1); // shorten
                        // String
                        
						shiftStart = 1200
                        * (period - 1)
                        + 60
                        * Integer.parseInt(line.substring(0,
                                                          line.indexOf(":")).trim())
                        + Integer.parseInt(line.substring(
                                                          line.indexOf(":") + 1,
                                                          line.indexOf(":") + 3).trim());
                        
						shiftStart = shiftStart + 1; // so shift start/end times
                        // don't overlap
                        
						line = f.readLine();
						/*
						 * this line contains the shift's ending time
						 */
						line = line.substring(line.indexOf(">") + 1); // shorten
                        // String
						if (currentPlayerNum == 13 && shiftStart == 154)
                        /*
                         * Krystofer Barch in 2007 V20029 has no shift end
                         * time listed for first shift, but duration listed
                         * as 0:11
                         */
							shiftEnd = 153 + 11;
						else if (currentPlayerNum == 55 && shiftStart == 759)
                        /*
                         * Niklas Kronwall in 2007 V20097 has no shift end
                         * time listed for first shift, but duration listed
                         * as 1:25
                         */
							shiftEnd = 758 + 85;
						else
							shiftEnd = 1200
                            * (period - 1)
                            + 60
                            * Integer.parseInt(line.substring(0,
                                                              line.indexOf(":")).trim())
                            + Integer.parseInt(line.substring(
                                                              line.indexOf(":") + 1,
                                                              line.indexOf(":") + 3).trim());
						// shiftEnd = shiftEnd - 1; // so times don't overlap
                        
						f.readLine();
						/*
						 * this line contains the shift's duration. Not needed
						 */
                        
						/*
						 * mark this player down for being on ice between
						 * shiftStart and shiftEnd (rows of playersOnIce)
						 */
						for (int r = shiftStart; r <= shiftEnd; r++) {
							// check for open spot in matrix
							int start = 0;
							/*
							 * Primary team always has start=0 Four cases: 1)
							 * Primary team is "home", and we're looking at the
							 * home team, start=0; 2) Primary team is "road",
							 * and we're looking at the road team, start=0 3)
							 * We're looking at the home team, which is the
							 * primary team, start=0; 4) We're looking at the
							 * road team, which is the primary team, start=0. 5)
							 * Other case, start=6.
							 */
							if (lookingAtHomeTeam
                                && teamoffocus.equalsIgnoreCase("home"))
								start = 0;
							else if (!lookingAtHomeTeam
                                     && teamoffocus.equalsIgnoreCase("road"))
								start = 0;
							else if (hName.equalsIgnoreCase(teamoffocus)
                                     && lookingAtHomeTeam)
								start = 0;
							else if (vName.equalsIgnoreCase(teamoffocus)
                                     && !lookingAtHomeTeam)
								start = 0;
							else
								start = 12;
							int add = 0;
							while (playersOnIce[r][start + add].length() > 0) {
								add += 2;
                                
								/*
								 * If a team has too many men on the ice and the
								 * other team makes an entry during this time,
								 * instead of recording seven players for the
								 * team with TMM, I'll just overwrite the final
								 * player.
								 */
								if (add >= 12) {
									add = 10;
									break;
								}
							}
                            
							playersOnIce[r][start + add] = ""
                            + currentPlayerNum;
							playersOnIce[r][start + add + 1] = currentplayername;
						}
					}
				}
			} catch (IOException ioe) {
				break;
			} catch (NullPointerException npe) {
				break;
			} catch (StringIndexOutOfBoundsException e) {
				System.out.println();
			}
		try {
			f.close();
		} catch (IOException ioe) {
			System.out.println("Huh? This shouldn't have happened...");
		} catch (Exception sioobe) {
			System.out.println(sioobe + "\n" + line);
		}
	}
    
	/*
	 * Create team's season shift log. Will be very big (70 MB or more for full
	 * season), but like pbp season logs, handy.
	 */
    
	private static void generateTeamTOIPages(int season, int limit)
    throws IOException {
		Map<String, BufferedWriter> teamToTOIWriter = new HashMap<String, BufferedWriter>();
		Map<String, Set<String>> teamToPlayerNamesAndNums = new HashMap<String, Set<String>>();
		for (String team : ABBREVS.keySet())
			if (team.length() == 3) {
				teamToPlayerNamesAndNums.put(ABBREVS.get(team),
                                             new HashSet<String>());
                
				teamToTOIWriter
                .put(team, new BufferedWriter(new FileWriter(new File(
                                                                      FOLDERPATHTOI + season + "/" + team + ".csv"))));
				teamToTOIWriter.get(team).write(
                                                season + " TOI log for " + ABBREVS.get(team) + "\n");
				teamToTOIWriter.get(team).write(
                                                "Game,Opponent,Time,Score,Strength,");
				teamToTOIWriter.get(team).write(
                                                "Opp1,Opp1Name,Opp2,Opp2Name,Opp3,Opp3Name,");
				teamToTOIWriter.get(team).write(
                                                "Opp4,Opp4Name,Opp5,Opp5Name,Opp6,Opp6Name,");
				teamToTOIWriter.get(team).write(
                                                team + "1," + team + "1Name," + team + "2," + team
                                                + "2Name," + team + "3," + team + "3Name,");
				teamToTOIWriter.get(team).write(
                                                team + "4," + team + "4Name," + team + "5," + team
                                                + "5Name," + team + "6," + team + "6Name\n");
			}
		BufferedReader in = null;
		String[] s;
		String line;
		BufferedWriter bw1;
		BufferedWriter bw2;
		String homename;
		String roadname;
		String pref1;
		String pref2;
		String[] other;
		for (int g = 20001; g <= limit; g++)
			try {
				in = new BufferedReader(new FileReader(FOLDERPATHPBP + season
                                                       + "/" + g + ".csv"));
				s = in.readLine().split("@");
				homename = s[1].trim();
				if (homename.equals("CANADIENS MONTREAL"))
					homename = "MONTREAL CANADIENS";
				roadname = s[0].substring(s[0].indexOf(":") + 1).trim();
				if (roadname.equals("CANADIENS MONTREAL"))
					roadname = "MONTREAL CANADIENS";
				bw1 = teamToTOIWriter.get(ABBREVS.get(homename));
				bw2 = teamToTOIWriter.get(ABBREVS.get(roadname));
				pref1 = g + "," + ABBREVS.get(roadname) + ",";
				pref2 = g + ",@" + ABBREVS.get(homename) + ",";
				in.close();
                
				in = new BufferedReader(new FileReader(FOLDERPATHTOI + season
                                                       + "/" + g + " shifts.csv"));
				do {
					line = in.readLine();
				} while (!line.contains("Time(sec)"));
				while (true)
					try {
						line = in.readLine();
						s = line.split(",");
						bw1.write(pref1);
						bw2.write(pref2);
						other = new String[s.length];
						other[0] = s[0];
						line = s[1];
						try {
							other[1] = s[1].substring(s[1].indexOf("-") + 1)
                            + "-"
                            + s[1].substring(0, s[1].indexOf("-"));
							other[2] = s[2].substring(s[2].indexOf("v") + 1)
                            + "v"
                            + s[2].substring(0, s[2].indexOf("v"));
						} catch (StringIndexOutOfBoundsException sioobe) {
							other[1] = "n/a";
						}
                        
						for (int i = 3; i <= 14; i++)
							try {
								other[i] = s[i + 12];
							} catch (ArrayIndexOutOfBoundsException aio) {
								other[i] = "";
							}
						for (int i = 15; i < s.length; i++)
							other[i] = s[i - 12];
						for (int i = 0; i < s.length; i++) {
							bw1.write(s[i] + ",");
							bw2.write(other[i] + ",");
						}
						bw1.newLine();
						bw2.newLine();
					} catch (NullPointerException npe) {
						in.close();
						break;
					} catch (ArrayIndexOutOfBoundsException aoobe) {
						/*
						 * in.close(); System.out.println("Error reading " + g +
						 * "\n" + line + "\n" + s.length);
						 */
						bw1.newLine();
						bw2.newLine();
					}
			} catch (FileNotFoundException fnfe) {
				System.out.println("No play by play for " + season + " " + g);
			} catch (NullPointerException npe) {
				System.out.println("Trouble reading " + g);
				in.close();
			}
		for (String st : teamToTOIWriter.keySet())
			teamToTOIWriter.get(st).close();
	}
    
	/* Generates a map of player names to positions */
    
	public static Map<String, Character> getPlayerPositionMap(int season) {
		BufferedReader f = null;
		String line = null;
		Map<String, Character> map = new HashMap<String, Character>();
		try {
			f = new BufferedReader(new FileReader(FOLDERPATHTOI + season
                                                  + " rosters.txt"));
			while (true)
				try {
					line = f.readLine();
					if (line.length() >= 5
                        && (!line.contains("(") || line.indexOf(")")
                            - line.indexOf("(") != 4)) {
                            line = line.substring(line.indexOf("\t") + 1);
                            map.put(line.substring(line.indexOf("\t") + 1),
                                    line.charAt(0));
                        }
				} catch (NullPointerException npe) {
					f.close();
					break;
				}
		} catch (IOException ioe) {
			System.out.println("Error reading " + season + " roster");
		} catch (StringIndexOutOfBoundsException sioobe) {
			System.out.println(line);
		}
		return map;
	}
    
	public static String getGameFileNamePbP(int season, int game) {
		return FOLDERPATHPBP + season + "/" + game + ".csv";
	}
    
	public static BufferedReader getGameReaderPbP(int season, int game)
    throws Exception {
		BufferedReader f;
		try {
			f = new BufferedReader(new FileReader(getGameFileNamePbP(season,
                                                                     game)));
		} catch (FileNotFoundException fnfe) {
			System.out.println("File not found");
			return null;
		}
		String line;
		try {
			line = f.readLine();
			while (!line.contains("Period"))
				line = f.readLine();
		} catch (IOException ioe) {
			System.out.println("IOException while reading file");
			return null;
		}
		return f;
	}
    
	public static BufferedReader getGameReaderPbP(int season, String team)
    throws Exception {
		BufferedReader f;
		try {
			f = new BufferedReader(new FileReader(getGameFileNamePbP(season,
                                                                     team)));
		} catch (FileNotFoundException fnfe) {
			System.out.println("File not found");
			return null;
		}
		String line;
		try {
			line = f.readLine();
			while (!line.contains("Period"))
				line = f.readLine();
		} catch (IOException ioe) {
			System.out.println("IOException while reading file");
			return null;
		}
		return f;
	}
    
	public static BufferedReader getGameReaderTOI(int season, int game)
    throws Exception {
		BufferedReader f;
		try {
			f = new BufferedReader(new FileReader(getGameFileNameTOI(season,
                                                                     game)));
		} catch (FileNotFoundException fnfe) {
			System.out.println("File not found");
			return null;
		}
		String line;
		try {
			line = f.readLine();
			while (!line.contains("Time"))
				line = f.readLine();
		} catch (IOException ioe) {
			System.out.println("IOException while reading file");
			return null;
		}
		return f;
	}
    
	public static BufferedReader getGameReaderTOI(int season, String team)
    throws Exception {
		BufferedReader f;
		try {
			f = new BufferedReader(new FileReader(getGameFileNameTOI(season,
                                                                     team)));
		} catch (FileNotFoundException fnfe) {
			System.out.println("File not found");
			return null;
		}
		String line;
		try {
			line = f.readLine();
			while (!line.contains("Time"))
				line = f.readLine();
		} catch (IOException ioe) {
			System.out.println("IOException while reading file");
			return null;
		}
		return f;
	}
    
	public static String getGameFileNameTOI(int season, int game) {
		return FOLDERPATHTOI + season + "/" + game + " shifts.csv";
	}
    
	public static String getGameFileNamePbP(int season, String team) {
		return FOLDERPATHPBP + season + "/" + team + ".csv";
	}
    
	public static String getGameFileNameTOI(int season, String team) {
		return FOLDERPATHTOI + season + "/" + team + ".csv";
	}
    
	public static int getGameTimeSecRegSeason(int pd, int min, int sec,
                                              boolean elapsed) {
		if (elapsed)
			return (pd - 1) * 3600 + min * 60 + sec;
		else if (pd != 4)
			return (pd - 1) * 3600 + (19 - min) * 60 + (60 - sec);
		else
			return (pd - 1) * 3600 + (4 - min) * 60 + (60 - sec);
	}
    
	public static boolean scoreClose(String[] s) {
		for (int i = 0; i < s.length; i++)
			if (s[i].contains("-")) {
				int score1 = Integer.parseInt(s[i].substring(0,
                                                             s[i].indexOf("-")));
				int score2 = Integer
                .parseInt(s[i].substring(s[i].indexOf("-") + 1));
				try {
					int period = Integer.parseInt(s[i - 3]);
					if (period > 8) // got gm# instead
						period = Integer.parseInt(s[i - 1]) / 1200 + 1;
					if (score1 == score2
                        || (Math.abs(score1 - score2) <= 1 && period <= 2))
						return true;
					else
						return false;
				} catch (NumberFormatException nfe) {
					int time = Integer.parseInt(s[2]);
					if (score1 == score2
                        || (Math.abs(score1 - score2) <= 1 && time < 2400))
						return true;
					else
						return false;
				}
			}
		return false;
	}
    
	public static boolean scoreTied(String[] s) {
		for (int i = 0; i < s.length; i++)
			if (s[i].contains("-")) {
				int score1 = Integer.parseInt(s[i].substring(0,
                                                             s[i].indexOf("-")));
				int score2 = Integer
                .parseInt(s[i].substring(s[i].indexOf("-") + 1));
				return score1 == score2;
			}
		return false;
	}
    
	public static int getTime(String[] s) {
		int pd, min, sec;
		try {
			pd = Integer.parseInt(s[2]);
			min = Integer.parseInt(s[4].substring(0, s[4].indexOf(":")));
			sec = Integer.parseInt(s[4].substring(s[4].indexOf(":") + 1));
			return 20 * 60 * (pd - 1) + 60 * min + sec;
		} catch (StringIndexOutOfBoundsException sioobe) {
			return Integer.parseInt(s[2]);
		} catch (NumberFormatException nfe) {
			pd = Integer.parseInt(s[0]);
			min = Integer.parseInt(s[2].substring(0, s[2].indexOf(":")));
			sec = Integer.parseInt(s[2].substring(s[2].indexOf(":") + 1));
			return 20 * 60 * (pd - 1) + 60 * min + sec;
		}
	}
}