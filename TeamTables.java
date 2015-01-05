import java.io.*;
import java.net.*;

/*
 * Reads through a file containing event data from one or more games.
 * Appends the numbers of the players on ice for each event at the end of each line.
 * The PRIMARYTEAM's players occupy the first six boxes, the other team's players the next six.
 * You should only ever need to change the fields PRIMARYTEAM, FILENAME,
 * FILENAMEOUT, FIRSTLINE, PERCOLUMN, MAXTIME, and SEASON.
 * */
public class TeamTables {
	private static final String PRIMARYTEAM = "EDMONTON OILERS";
	/*
	 * For example; not case sensitive
	 */
	private static final String FILENAME = "/Users/Me/Desktop/Code/OilersSeasonEntries.csv";
	/*
	 * Where is the file?
	 */
	private static final String FILENAMEOUT = "/Users/Me/Desktop/Code/OilersSeasonEntriesOut.csv";
	/*
	 * Output file name.
	 */
	private static final int FIRSTLINE = 2;
	/*
	 * e.g. if you have one line of header and the entries start on the second
	 * line of the spreadsheet, this will be equal to two.
	 */
	private static final int PERCOLUMN = 2;
	/*
	 * in your spreadsheet, is the period number in the 1st column or 2nd (etc)
	 * ? I assume the time in min:sec format is in the next one
	 */
	private static final int MAXTIME = 3901;
    
	/*
	 * set to 60secs x (60min of regulation + 5min of OT) = 3900 for regular
	 * season. For playoffs, set to, e.g. 60 x 60 = 3600 if ends in regulation,
	 * 60 x (60+20) = 4800 if ends in 1st OT, etc.
	 */
	private static final String SEASON = "20122013";
    
	/*
	 * Make sure to change this to the right season
	 */
    
	/*
	 * Note: I am assuming that the first column is GameID, the second is
	 * period, and the third is time in Min:Sec. 
	 */
	public static void main(String[] args) {
        
		/*
		 * Create matrix[time][numbers of 12 players on ice] for current game
		 * Read through file. On each line, first check GameID. If I have
		 * already read TOI data for that game, it will be in my list, and I can
		 * just add the players on ice. If not, need to read the TOI file.
		 */
        
		BufferedReader in = null;
		String[] info;
		int gamenum;
		BufferedReader f;
		URL visitorTOIurl;
		URL homeTOIurl;
		int[][] temp = null;
		BufferedWriter out = null;
		int currentgame = 0;
        
		try {
			out = new BufferedWriter(new FileWriter(new File(FILENAMEOUT)));
			try {
				in = new BufferedReader(new FileReader(FILENAME));
				// Skip lines of header
				for (int k = 1; k <= FIRSTLINE - 1; k++)
					out.write(in.readLine() + "\n");
			} catch (FileNotFoundException fnfe) {
				System.out.println("Entries input file not found.");
				System.exit(0);
			} catch (IOException ioe) {
				System.out.println("Unexpected input format.");
				System.exit(0);
			}
            
		} catch (Exception e) {
			System.out.println("File creation failed.");
			System.exit(0);
		}
		while (true)
			try {
				info = in.readLine().split(",");
				gamenum = Integer.parseInt(info[0]);
				if (gamenum != currentgame)
				/*
				 * If these two don't match, my toiMatrix is not up to date.
				 * Need to update.
				 */
				{
					currentgame = gamenum;
                    
					visitorTOIurl = new URL(
                                            "http://www.nhl.com/scores/htmlreports/" + SEASON
                                            + "/TV0" + gamenum + ".HTM");
					homeTOIurl = new URL(
                                         "http://www.nhl.com/scores/htmlreports/" + SEASON
                                         + "/TH0" + gamenum + ".HTM");
					try {
						temp = new int[MAXTIME][12];
						for (int r = 0; r < temp.length; r++)
							for (int c = 0; c < temp[0].length; c++)
								temp[r][c] = 0;
                        
						f = new BufferedReader(new InputStreamReader(
                                                                     homeTOIurl.openStream()));
						readShiftData(f, "home", temp, PRIMARYTEAM);
                        
						f = new BufferedReader(new InputStreamReader(
                                                                     visitorTOIurl.openStream()));
						readShiftData(f, "visitor", temp, PRIMARYTEAM);
                        
					} catch (Exception ex) {
						System.out.println("Error:\t" + ex);
					}
				}
				out.write(addPlayersOnIceToLine(info, temp, PERCOLUMN) + "\n");
				out.flush();
			} catch (Exception e) {
				System.out.println(e);
				break;
			}
		try {
			out.close();
		} catch (IOException ioe) {
			System.exit(1);
		}
	}
    
	public static String addPlayersOnIceToLine(String[] info,
                                               int[][] playersOnIce, int percolumn) throws Exception {
		String toadd = "";
		int totTimeElapsed;
        
		// GameID, Period, Time...
		int period = Integer.parseInt(info[percolumn - 1].trim());
		int minTime = Integer.parseInt(info[percolumn].substring(0,
                                                                 info[percolumn].indexOf(":")).trim());
		int secTime = Integer.parseInt(info[percolumn].substring(
                                                                 info[percolumn].indexOf(":") + 1).trim());
		if (period == 4 && info[0].substring(0, 1).equals("2"))
        /*
         * regular season OT is 5 mins, not 20
         */
            
			totTimeElapsed = 60 * 20 * (period - 1) + 300
            - (60 * minTime + secTime);
		else
			totTimeElapsed = 60 * 20 * (period - 1) + 1200
            - (60 * minTime + secTime);
        
		for (int player = 0; player < 12; player++)
			if (playersOnIce[totTimeElapsed][player] != 0)
				toadd = toadd + "," + playersOnIce[totTimeElapsed][player];
			else
				toadd = toadd + ",";
        
		String newline = "";
		for (int i = 0; i < info.length; i++)
			newline = newline + info[i] + ",";
		newline = newline + toadd;
		return newline;
	}
    
	public static void readShiftData(BufferedReader f, String homeOrVisitor,
                                     int[][] playersOnIce, String teamoffocus) {
        
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
                
				// Go to player
				while (!line.contains("playerHeading"))
					line = f.readLine();
                
				// Get player name and number, add to Map.
				line = line.substring(line.indexOf(">") + 1, line.length() - 5);
				try {
					currentPlayerNum = Integer.parseInt(line.substring(0, 2)
                                                        .trim());
					// System.out.println(currentPlayerNum);
				} catch (NumberFormatException nfe) {
					do {
						line = f.readLine();
					} while (!line.contains("playerHeading"));
					line = line.substring(line.indexOf(">") + 1,
                                          line.length() - 5);
					currentPlayerNum = Integer.parseInt(line.substring(0, 2)
                                                        .trim());
				}
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
								start = 6;
							int add = 0;
							while (playersOnIce[r][start + add] != 0) {
								add++;
                                
								/*
								 * If a team has too many men on the ice and the
								 * other team makes an entry during this time,
								 * instead of recording seven players for the
								 * team with TMM, I'll just overwrite the final
								 * player.
								 */
								if (add >= 6) {
									add = 5;
									break;
								}
							}
                            
							playersOnIce[r][start + add] = currentPlayerNum;
						}
					}
				}
			} catch (IOException ioe) {
				break;
			} catch (NullPointerException npe) {
				break;
			}
		try {
			f.close();
		} catch (IOException ioe) {
			System.out.println("Huh? This shouldn't have happened...");
		} catch (Exception sioobe) {
			System.out.println(sioobe + "\n" + line);
		}
	}
}
