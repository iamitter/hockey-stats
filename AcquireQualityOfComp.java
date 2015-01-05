import java.util.*;
import java.io.*;
import javax.swing.JOptionPane;

public class AcquireQualityOfComp {
    
	private static Map<String, String> ABBREVS = DownloadPlayByPlay.getTeamToAbbrevMap();
	private static final String OUTPUTFOLDERPATH = "/Users/Matt/Desktop/NHL/Comp/";
    private static final boolean INCLUDEGOALIES = false;
	private static boolean printinsamefile = true;
	private static boolean useSavedFiles = true;
    
	public static void main(String[] args) {
		/*
		 * Ask which competition metric--TOI/60, GFON/60, GAON/60, 
         * SFON/60, SAON/60, FFON/60, FAON/60, CFON60, CAON60.
		 */
		int metric = askForMetric();
		/*
		 * Ask for which team(s) data is needed. Should all data be printed in
		 * one file?
		 */
		Set<String> teamsneeded = askForTeams();
		if (teamsneeded.size() > 1)
			printinsamefile = JOptionPane.showInputDialog(
                                                          "Print in same file? y/n").equalsIgnoreCase("y");
        
		List<Integer> seasons = askForSeasons(DownloadPlayByPlay.SEASON_MAX - 2007);
		int startX = DownloadPlayByPlay.getStartGame(seasons.get(0));
		int endX = DownloadPlayByPlay.getEndGame(seasons.get(seasons.size() - 1));
		int start;
		int end;
		BufferedWriter out = null;
		List<Map<String, String>> ptcmerged = new ArrayList<Map<String, String>>();
		ptcmerged.add(new HashMap<String, String>());
		ptcmerged.add(new HashMap<String, String>());
		Map<Integer, Map<String, Double>> seasonToPlayernameToMetric = new HashMap<Integer, Map<String, Double>>();
		Map<String, String> playerToTeam = new HashMap<String, String>();
		for (Integer season : seasons) {
			if (season == seasons.get(0))
				start = startX;
			else
				start = 20001;
			if (season == seasons.get(seasons.size() - 1))
				end = endX;
			else
				end = 21230;
            
			/* Read rosters, create a map of name-->team */
            
			Map<String, Map<String, String>> teamrosters = null;
			try {
				teamrosters = readRosters(season);
			} catch (IOException ioe) {
				System.out.println("Couldn't read roster info for " + season);
				System.exit(0);
			}
            
			for (String team : teamrosters.keySet())
				for (String playername : teamrosters.get(team).keySet())
					playerToTeam.put(playername, team);
            
			/*
			 * Calculate relevant metric for all players, store in Map--name to
			 * metric
			 */
			switch (metric) {
                case 1:
                    seasonToPlayernameToMetric.put(season, calculateGFON60(season));
                    break;
                case 2:
                    seasonToPlayernameToMetric.put(season, calculateGAON60(season));
                    break;
                case 3:
                    seasonToPlayernameToMetric.put(season, calculateSFON60(season));
                    break;
                case 4:
                    seasonToPlayernameToMetric.put(season, calculateSAON60(season));
                    break;
                case 5:
                    seasonToPlayernameToMetric.put(season, calculateFFON60(season));
                    break;
                case 6:
                    seasonToPlayernameToMetric.put(season, calculateFAON60(season));
                    break;
                case 7:
                    seasonToPlayernameToMetric.put(season, calculateCFON60(season));
                    break;
                case 8:
                    seasonToPlayernameToMetric.put(season, calculateCAON60(season));
                    break;
                    /*
                     * case 9: seasonToPlayernameToMetric.put(season,
                     * calculateFenwickON60(season)); break; case 10:
                     * seasonToPlayernameToMetric.put(season,
                     * calculateFenwickRelON60(season)); break; case 11:
                     * seasonToPlayernameToMetric.put(season,
                     * calculateCorsiON60(season)); break; case 12:
                     * seasonToPlayernameToMetric.put(season,
                     * calculateCorsiRelON60(season)); break;
                     */
                default:
                    seasonToPlayernameToMetric.put(season, calculateTOI60(season));
			}
			/*
			 * Read shifts for team in question. At each second, add all comp
			 * numbers to player's qoc number. Separate F from D. Keep running
			 * tally of each player's total toi. Return two maps: F comp map, D
			 * comp map
			 */
			List<Map<String, String>> playerToComp = null;
			for (String team : teamsneeded) {
				playerToComp = calculateCompFD(team, season, start, end,
                                               seasonToPlayernameToMetric, teamrosters);
                
				/* Print (if necessary at this point) */
				try {
					if (out == null && printinsamefile) {
						String path = OUTPUTFOLDERPATH + seasons.toString()
                        + " ";
						String header = "Position\tPlayer\tTeam\t";
						;
						switch (metric) {
                            case 1:
                                path = path + "GFON60 from ";
                                header = header + "F GFComp\tD GFComp\t";
                                break;
                            case 2:
                                path = path + "GAON60 from ";
                                header = header + "F GAComp\tD GAComp\t";
                                break;
                            case 3:
                                path = path + "SFON60 from ";
                                header = header + "F SFComp\tD SFComp\t";
                                break;
                            case 4:
                                path = path + "SAON60 from ";
                                header = header + "F SAComp\tD SAComp\t";
                                break;
                            case 5:
                                path = path + "FFON60 from ";
                                header = header + "F FFComp\tD FFComp\t";
                                break;
                            case 6:
                                path = path + "FAON60 from ";
                                header = header + "F FAComp\tD FAComp\t";
                                break;
                            case 7:
                                path = path + "CFON60 from ";
                                header = header + "F CFComp\tD CFComp\t";
                                break;
                            case 8:
                                path = path + "CAON60 from ";
                                header = header + "F CAComp\tD CAComp\t";
                                break;
                            default:
                                path = path + "TOIComp from ";
                                header = header + "F TOIComp\tD TOIComp\t";
                                break;
						}
						path = path + start + " " + end + " for "
                        + teamsneeded.toString() + ".txt";
						out = new BufferedWriter(new FileWriter(new File(path)));
						header = header
                        + "Approx TOI\tTotal man-secs vs F\tF secs weighted value\tTotal man-secs vs D\tD secs weighted value\n";
						out.write(header);
					}
					/* If printing only at end, merge comp maps by player name */
					if (printinsamefile)
						ptcmerged = merge(ptcmerged, playerToComp);
					else {
						if (out != null)
							out.close();
						out = new BufferedWriter(new FileWriter(new File(
                                                                         OUTPUTFOLDERPATH + season + " comp (" + metric
                                                                         + ") from " + start + " " + end
                                                                         + " for " + team + ".txt")));
						out.write("Position\tPlayer\tTeam\tF comp\tD comp\tApprox TOI\tTotal man-secs vs F\tF secs weighted value\tTotal man-secs vs D\tD secs weighted value\n");
						print(playerToComp, playerToTeam, out);
					}
                    
				} catch (IOException ioe) {
					System.out.println("Trouble writing results:\t" + ioe);
				}
			}
		}
		if (printinsamefile)
			print(ptcmerged, playerToTeam, out);
		try {
			out.close();
		} catch (IOException io) {
			System.out.println("Trouble closing writer:\t" + io);
		}
		System.out.println("Done");
	}
    
	/* Merges comp data from this season with comp data from previous seasons */
	private static List<Map<String, String>> merge(
                                                   List<Map<String, String>> m1, List<Map<String, String>> m2) {
		List<Map<String, String>> l = new ArrayList<Map<String, String>>();
		Map<String, String> temp = null;
		for (int k = 0; k <= 1; k++) {
			temp = new HashMap<String, String>();
            
			/*
			 * Create two maps: one of the man-sec TOI, and the other with the
			 * weighted comp total
			 */
			Map<String, Long> mapTot = new HashMap<String, Long>();
			Map<String, Double> mapComp = new HashMap<String, Double>();
			Map<String, String> f1 = m1.get(k);
			Map<String, String> f2 = m2.get(k);
			long tot = 0;
			double comp = 0;
			// forwards
			for (String pname : f1.keySet()) {
				tot = Long.parseLong(f1.get(pname).substring(0,
                                                             f1.get(pname).indexOf(" ")));
				comp = Double.parseDouble(f1.get(pname).substring(
                                                                  f1.get(pname).indexOf(" ") + 1));
				mapTot.put(pname, tot);
				mapComp.put(pname, comp);
			}
			for (String pname : f2.keySet()) {
				// System.out.print(mapTot.keySet().size() + "-->");
				tot = Long.parseLong(f2.get(pname).substring(0,
                                                             f2.get(pname).indexOf(" ")));
				comp = Double.parseDouble(f2.get(pname).substring(
                                                                  f2.get(pname).indexOf(" ") + 1));
                
				/*
				 * If old map has this player, add in info. Otherwise, create
				 * new entry
				 */
				if (mapTot.containsKey(pname)) {
					mapTot.put(pname, tot + mapTot.get(pname));
					mapComp.put(pname, comp + mapComp.get(pname));
				} else {
					mapTot.put(pname, tot);
					mapComp.put(pname, comp);
				}
				// System.out.println(mapTot.keySet().size() + "\t" + pname);
			}
			/*
			 * Put map back into old format of name-->string where string =
			 * [man-sec vs] [weight]
			 */
			for (String pname : mapTot.keySet())
				temp.put(pname, mapTot.get(pname) + " " + mapComp.get(pname));
			l.add(temp);
		}
		return l;
	}
    
	private static void print(List<Map<String, String>> list,
                              Map<String, String> playerToTeam, BufferedWriter out) {
        
		Map<String, String> fcomp = list.get(0);
		Map<String, String> dcomp = list.get(1);
		int ptoif = 0;
		int ptoid = 0;
		double fc = 0.0;
		double dc = 0.0;
		String name;
		double approxtoi;
		for (String pname : fcomp.keySet())
			try {
				if (pname.charAt(0) != 'G' || INCLUDEGOALIES) {
					name = pname.substring(pname.indexOf(" ") + 1).trim();
					out.write(pname.charAt(0) + "\t" + name + "\t"
                              + playerToTeam.get(name) + "\t");
					ptoif = Integer.parseInt(fcomp.get(pname).substring(0,
                                                                        fcomp.get(pname).indexOf(" ")));
					ptoid = Integer.parseInt(dcomp.get(pname).substring(0,
                                                                        dcomp.get(pname).indexOf(" ")));
					fc = Double.parseDouble(fcomp.get(pname).substring(
                                                                       fcomp.get(pname).indexOf(" ") + 1));
					dc = Double.parseDouble(dcomp.get(pname).substring(
                                                                       dcomp.get(pname).indexOf(" ") + 1));
					approxtoi = (double) (ptoif) / 180.0;
					if (ptoif != 0 && ptoid != 0)
						out.write((fc / ptoif) + "\t" + (dc / ptoid) + "\t"
                                  + approxtoi + "\t" + ptoif + "\t" + fc + "\t"
                                  + ptoid + "\t" + dc + "\n");
					else
						out.write("n/a\tn/a\t" + approxtoi + "\t" + ptoif
                                  + "\t" + fc + "\t" + dc + "\t" + ptoid + "\n");
				}
			} catch (IOException ioe) {
				System.out.println(ioe + "\nTrouble printing the file:" + pname
                                   + "\t" + fcomp.get(pname) + "\t" + dcomp.get(pname));
			}
	}
    
	/* Hopefully can add more in the future */
	private static int askForMetric() {
		String options = "Enter number of competition metric desired.\n1) GFON/60\n2) GAON/60\n3) SFON/60\n";
		options = options
        + "4) SAON/60\n5) FFON/60\n6) FAON/60\n7) CFON/60\n8) CAON/60\n9) TOI/60";
		return Integer.parseInt(JOptionPane.showInputDialog(options));
	}
    
	private static Set<String> askForTeams() {
		Set<String> list = new HashSet<String>();
		String s;
		while (true) {
			s = JOptionPane
            .showInputDialog("Enter team three-letter abbreviation (e.g. WSH), or "
                             + '"'
                             + "all"
                             + '"'
                             + " for all teams. Enter -1 to finish");
			if (s.equals("-1"))
				break;
			if (ABBREVS.containsKey(s))
				list.add(s);
			else if (s.equalsIgnoreCase("all")) {
				for (String t : ABBREVS.keySet())
					if (t.trim().length() == 3)
						if (!list.contains(t))
							list.add(t);
				break;
			} else
				JOptionPane
                .showMessageDialog(null, "Invalid entry. Try again.");
		}
		return list;
	}
    
	private static List<Integer> askForSeasons(int sizelimit) {
		Set<Integer> list = new TreeSet<Integer>();
		Integer i;
		String card;
		while (true)
			try {
				if (list.size() == sizelimit)
					break;
				switch (list.size()) {
                    case 0:
                        card = "1st";
                        break;
                    case 1:
                        card = "2nd";
                        break;
                    case 2:
                        card = "3rd";
                        break;
                    default:
                        card = (list.size() + 1) + "th";
                        break;
				}
				i = Integer
                .parseInt(JOptionPane
                          .showInputDialog("Enter "
                                           + card
                                           + " season (e.g. 2007 for 07-08); enter -1 when finished"));
				if (i == -1)
					break;
				if (i < 2007 || i > DownloadPlayByPlay.SEASON_MAX)
					throw new NumberFormatException();
				else
					list.add(i);
			} catch (NumberFormatException nfe) {
				JOptionPane
                .showMessageDialog(null, "Invalid entry. Try again.");
			}
		List<Integer> l = new ArrayList<Integer>();
		for (Integer q : list)
			l.add(q);
		return l;
	}
    
	private static Map<String, Double> calculateEventsForOrAgainstON60(
                                                                       int season, Set<String> eventtypes, String fororagainst) {
		Map<String, Double> map = new HashMap<String, Double>();
		/* Get TOI */
		Map<String, Integer> toi = calculateTOnI(season);
		/* Get Event Against totals */
		Map<String, Integer> ea = getEventsForOrAgainst(season, eventtypes,
                                                        fororagainst);
        
		/* Calculate EAON/60 */
		for (String pname : toi.keySet())
			if (ea.containsKey(pname.substring(2))) {
				map.put(pname, (double) ea.get(pname.substring(2))
						/ ((1.0 * toi.get(pname) / 3600.0)));
			} else
				map.put(pname, 0.0);
		return map;
	}
    
	public static Map<String, Double> calculateGFON60(int season) {
		Map<String, Double> map = new HashMap<String, Double>();
        
		File file = new File(OUTPUTFOLDERPATH + season + " GFON60.txt");
		String[] s;
		BufferedReader f = null;
        
		/*
		 * If GFON60 file already created, just read from that. If not,
		 * calculate.
		 */
		if (useSavedFiles && file.exists()) {
			try {
				f = new BufferedReader(new FileReader(file));
				f.readLine();
				while (true)
					try {
						s = f.readLine().split("\t");
						map.put(s[0].trim(), Double.parseDouble(s[1].trim()));
					} catch (NullPointerException npe) {
						f.close();
						System.out.println("Read GFON/60 from file");
						return map;
					}
			} catch (IOException ioe) {
				System.out.println("Trouble reading from GFON60 file for "
                                   + season + "; generating from scratch");
			}
		} else {
			Set<String> set = new HashSet<String>();
			set.add("GOAL");
			map = calculateEventsForOrAgainstON60(season, set, "for");
		}
        
		/* Print to file for use next time */
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(OUTPUTFOLDERPATH
                                                             + season + " GFON60.txt")));
			out.write("Name\tGFON/60");
			for (String pname : map.keySet())
				out.write("\n" + pname + "\t" + map.get(pname));
			out.close();
		} catch (IOException ioe) {
			System.out.println("Had trouble creating GFON60 file for " + season
                               + " for future reference");
		}
		return map;
	}
    
	public static Map<String, Double> calculateGAON60(int season) {
		Map<String, Double> map = new HashMap<String, Double>();
        
		File file = new File(OUTPUTFOLDERPATH + season + " GAON60.txt");
		String[] s;
		BufferedReader f = null;
        
		/*
		 * If GAON60 file already created, just read from that. If not,
		 * calculate.
		 */
		if (useSavedFiles && file.exists()) {
			try {
				f = new BufferedReader(new FileReader(file));
				f.readLine();
				while (true)
					try {
						s = f.readLine().split("\t");
						map.put(s[0].trim(), Double.parseDouble(s[1].trim()));
					} catch (NullPointerException npe) {
						f.close();
						System.out.println("Read GAON/60 from file");
						return map;
					}
			} catch (IOException ioe) {
				System.out.println("Trouble reading from GAON60 file for "
                                   + season + "; generating from scratch");
			}
		} else {
			Set<String> set = new HashSet<String>();
			set.add("GOAL");
			map = calculateEventsForOrAgainstON60(season, set, "against");
		}
        
		/* Print to file for use next time */
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(OUTPUTFOLDERPATH
                                                             + season + " GAON60.txt")));
			out.write("Name\tGAON/60");
			for (String pname : map.keySet())
				out.write("\n" + pname + "\t" + map.get(pname));
			out.close();
		} catch (IOException ioe) {
			System.out.println("Had trouble creating GAON60 file for " + season
                               + " for future reference");
		}
		return map;
	}
    
	public static Map<String, Double> calculateSFON60(int season) {
		Map<String, Double> map = new HashMap<String, Double>();
        
		File file = new File(OUTPUTFOLDERPATH + season + " SFON60.txt");
		String[] s;
		BufferedReader f = null;
        
		/*
		 * If GFON60 file already created, just read from that. If not,
		 * calculate.
		 */
		if (useSavedFiles && file.exists()) {
			try {
				f = new BufferedReader(new FileReader(file));
				f.readLine();
				while (true)
					try {
						s = f.readLine().split("\t");
						map.put(s[0].trim(), Double.parseDouble(s[1].trim()));
					} catch (NullPointerException npe) {
						f.close();
						System.out.println("Read SFON/60 from file");
						return map;
					}
			} catch (IOException ioe) {
				System.out.println("Trouble reading from SFON60 file for "
                                   + season + "; generating from scratch");
			}
		} else {
			Set<String> set = new HashSet<String>();
			set.add("GOAL");
			set.add("SHOT");
			set.add("POST");
			map = calculateEventsForOrAgainstON60(season, set, "for");
		}
        
		/* Print to file for use next time */
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(OUTPUTFOLDERPATH
                                                             + season + " SFON60.txt")));
			out.write("Name\tSFON/60");
			for (String pname : map.keySet())
				out.write("\n" + pname + "\t" + map.get(pname));
			out.close();
		} catch (IOException ioe) {
			System.out.println("Had trouble creating SFON60 file for " + season
                               + " for future reference");
		}
		return map;
	}
    
	public static Map<String, Double> calculateSAON60(int season) {
		Map<String, Double> map = new HashMap<String, Double>();
        
		File file = new File(OUTPUTFOLDERPATH + season + " SAON60.txt");
		String[] s;
		BufferedReader f = null;
        
		/*
		 * If GAON60 file already created, just read from that. If not,
		 * calculate.
		 */
		if (useSavedFiles && file.exists()) {
			try {
				f = new BufferedReader(new FileReader(file));
				f.readLine();
				while (true)
					try {
						s = f.readLine().split("\t");
						map.put(s[0].trim(), Double.parseDouble(s[1].trim()));
					} catch (NullPointerException npe) {
						f.close();
						System.out.println("Read SAON/60 from file");
						return map;
					}
			} catch (IOException ioe) {
				System.out.println("Trouble reading from SAON60 file for "
                                   + season + "; generating from scratch");
			}
		} else {
			Set<String> set = new HashSet<String>();
			set.add("GOAL");
			set.add("SHOT");
			set.add("POST");
			map = calculateEventsForOrAgainstON60(season, set, "against");
		}
        
		/* Print to file for use next time */
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(OUTPUTFOLDERPATH
                                                             + season + " SAON60.txt")));
			out.write("Name\tSAON/60");
			for (String pname : map.keySet())
				out.write("\n" + pname + "\t" + map.get(pname));
			out.close();
		} catch (IOException ioe) {
			System.out.println("Had trouble creating SAON60 file for " + season
                               + " for future reference");
		}
		return map;
	}
    
	public static Map<String, Double> calculateFFON60(int season) {
		Map<String, Double> map = new HashMap<String, Double>();
        
		File file = new File(OUTPUTFOLDERPATH + season + " FFON60.txt");
		String[] s;
		BufferedReader f = null;
        
		/*
		 * If GFON60 file already created, just read from that. If not,
		 * calculate.
		 */
		if (useSavedFiles && file.exists()) {
			try {
				f = new BufferedReader(new FileReader(file));
				f.readLine();
				while (true)
					try {
						s = f.readLine().split("\t");
						map.put(s[0].trim(), Double.parseDouble(s[1].trim()));
					} catch (NullPointerException npe) {
						f.close();
						System.out.println("Read FFON/60 from file");
						return map;
					}
			} catch (IOException ioe) {
				System.out.println("Trouble reading from FFON60 file for "
                                   + season + "; generating from scratch");
			}
		} else {
			Set<String> set = new HashSet<String>();
			set.add("GOAL");
			set.add("SHOT");
			set.add("POST");
			set.add("MISS");
			map = calculateEventsForOrAgainstON60(season, set, "for");
		}
        
		/* Print to file for use next time */
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(OUTPUTFOLDERPATH
                                                             + season + " FFON60.txt")));
			out.write("Name\tFFON/60");
			for (String pname : map.keySet())
				out.write("\n" + pname + "\t" + map.get(pname));
			out.close();
		} catch (IOException ioe) {
			System.out.println("Had trouble creating FFON60 file for " + season
                               + " for future reference");
		}
		return map;
	}
    
	public static Map<String, Double> calculateFAON60(int season) {
		Map<String, Double> map = new HashMap<String, Double>();
        
		File file = new File(OUTPUTFOLDERPATH + season + " FAON60.txt");
		String[] s;
		BufferedReader f = null;
        
		/*
		 * If GAON60 file already created, just read from that. If not,
		 * calculate.
		 */
		if (useSavedFiles && file.exists()) {
			try {
				f = new BufferedReader(new FileReader(file));
				f.readLine();
				while (true)
					try {
						s = f.readLine().split("\t");
						map.put(s[0].trim(), Double.parseDouble(s[1].trim()));
					} catch (NullPointerException npe) {
						f.close();
						System.out.println("Read FAON/60 from file");
						return map;
					}
			} catch (IOException ioe) {
				System.out.println("Trouble reading from FAON60 file for "
                                   + season + "; generating from scratch");
			}
		} else {
			Set<String> set = new HashSet<String>();
			set.add("GOAL");
			set.add("SHOT");
			set.add("POST");
			set.add("MISS");
			map = calculateEventsForOrAgainstON60(season, set, "against");
		}
        
		/* Print to file for use next time */
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(OUTPUTFOLDERPATH
                                                             + season + " FAON60.txt")));
			out.write("Name\tFAON/60");
			for (String pname : map.keySet())
				out.write("\n" + pname + "\t" + map.get(pname));
			out.close();
		} catch (IOException ioe) {
			System.out.println("Had trouble creating FAON60 file for " + season
                               + " for future reference");
		}
		return map;
	}
    
	public static Map<String, Double> calculateCFON60(int season) {
		Map<String, Double> map = new HashMap<String, Double>();
        
		File file = new File(OUTPUTFOLDERPATH + season + " CFON60.txt");
		String[] s;
		BufferedReader f = null;
        
		/*
		 * If GFON60 file already created, just read from that. If not,
		 * calculate.
		 */
		if (useSavedFiles && file.exists()) {
			try {
				f = new BufferedReader(new FileReader(file));
				f.readLine();
				while (true)
					try {
						s = f.readLine().split("\t");
						map.put(s[0].trim(), Double.parseDouble(s[1].trim()));
					} catch (NullPointerException npe) {
						f.close();
						System.out.println("Read CFON/60 from file");
						return map;
					}
			} catch (IOException ioe) {
				System.out.println("Trouble reading from CFON60 file for "
                                   + season + "; generating from scratch");
			}
		} else {
			Set<String> set = new HashSet<String>();
			set.add("GOAL");
			set.add("SHOT");
			set.add("POST");
			set.add("MISS");
			set.add("BLOCK");
			map = calculateEventsForOrAgainstON60(season, set, "for");
		}
        
		/* Print to file for use next time */
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(OUTPUTFOLDERPATH
                                                             + season + " CFON60.txt")));
			out.write("Name\tCFON/60");
			for (String pname : map.keySet())
				out.write("\n" + pname + "\t" + map.get(pname));
			out.close();
		} catch (IOException ioe) {
			System.out.println("Had trouble creating CFON60 file for " + season
                               + " for future reference");
		}
		return map;
	}
    
	public static Map<String, Double> calculateCAON60(int season) {
		Map<String, Double> map = new HashMap<String, Double>();
        
		File file = new File(OUTPUTFOLDERPATH + season + " CAON60.txt");
		String[] s;
		BufferedReader f = null;
        
		/*
		 * If GAON60 file already created, just read from that. If not,
		 * calculate.
		 */
		if (useSavedFiles && file.exists()) {
			try {
				f = new BufferedReader(new FileReader(file));
				f.readLine();
				while (true)
					try {
						s = f.readLine().split("\t");
						map.put(s[0].trim(), Double.parseDouble(s[1].trim()));
					} catch (NullPointerException npe) {
						f.close();
						System.out.println("Read CAON/60 from file");
						return map;
					}
			} catch (IOException ioe) {
				System.out.println("Trouble reading from CAON60 file for "
                                   + season + "; generating from scratch");
			}
		} else {
			Set<String> set = new HashSet<String>();
			set.add("GOAL");
			set.add("SHOT");
			set.add("POST");
			set.add("MISS");
			set.add("BLOCK");
			map = calculateEventsForOrAgainstON60(season, set, "against");
		}
        
		/* Print to file for use next time */
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(OUTPUTFOLDERPATH
                                                             + season + " CAON60.txt")));
			out.write("Name\tCAON/60");
			for (String pname : map.keySet())
				out.write("\n" + pname + "\t" + map.get(pname));
			out.close();
		} catch (IOException ioe) {
			System.out.println("Had trouble creating CAON60 file for " + season
                               + " for future reference");
		}
		return map;
	}
    
	private static Map<String, Integer> getEventsForOrAgainst(int season,
                                                              Set<String> types, String fororagainst) {
        
		// File file = new File(OUTPUTFOLDERPATH + season + " GA.txt");
		String[] s;
		BufferedReader f = null;
		Map<String, Integer> totalEA = new HashMap<String, Integer>();
        
		/*
		 * if (useSavedFiles && file.exists()) { try { f = new
		 * BufferedReader(new FileReader(file)); f.readLine(); while (true) try
		 * { s = f.readLine().split("\t"); totalGA.put(s[0].trim(),
		 * Integer.parseInt(s[1].trim())); } catch (NullPointerException npe) {
		 * f.close(); System.out.println("Read GA from file"); return totalGA; }
		 * } catch (IOException ioe) {
		 * System.out.println("Trouble reading from GA file for " + season +
		 * "; generating from scratch"); } }
		 */
		String homeroad;
		if (fororagainst.equalsIgnoreCase("for"))
			homeroad = "ROAD";
		else
			homeroad = "HOME";
		for (String teamname : ABBREVS.keySet())
			if (teamname.length() == 3)
				try {
					f = new BufferedReader(new FileReader(DownloadPlayByPlay.FOLDERPATHPBP
                                                          + season + "/" + teamname + ".csv"));
					f.readLine();
					f.readLine();
					/*
					 * Calculate each player's TOI in this game. Calculate OffI
					 * for players playing in this game.
					 */
					while (true)
						try {
							s = f.readLine().split(",");
							if (types.contains(s[6])
                                && (s[1].contains("@") == s[7]
                                    .equals(homeroad)))
                            /* Need goalies in */
								if (checkStrengthAndGoaliesIn("5v5", s, "pbp")) {
									for (int i = 25; i < s.length; i += 2)
										if (!totalEA.containsKey(s[i].trim()))
											totalEA.put(s[i].trim(), 1);
										else
											totalEA.put(
                                                        s[i].trim(),
                                                        totalEA.get(s[i].trim()) + 1);
								}
							/*
							 * At end of file, add this game's data to season
							 * data
							 */
						} catch (NullPointerException npe) {
							f.close();
							break;
						}
					System.out.println("Done reading " + teamname + " "
                                       + types.toString() + " " + fororagainst);
				} catch (IOException ioe) {
					System.out.println("Error when calculating "
                                       + types.toString() + ":\t" + ioe);
				}
        
		/*
		 * BufferedWriter out = null; try { out = new BufferedWriter(new
		 * FileWriter(new File(OUTPUTFOLDERPATH + season + " GA.txt")));
		 * out.write("Name\tGA"); for (String pname : totalGA.keySet())
		 * out.write("\n" + pname + "\t" + totalGA.get(pname)); out.close(); }
		 * catch (IOException ioe) {
		 * System.out.println("Had trouble creating GA file for " + season +
		 * " for future reference"); } return totalGA;
		 */
		return totalEA;
	}
    
	private static Map<String, Integer> calculateTOnI(int season) {
		Map<String, Integer> map = new HashMap<String, Integer>();
        
		File file = new File(OUTPUTFOLDERPATH + season + " TOI.txt");
		String[] s;
		BufferedReader f = null;
		String edit;
		/*
		 * If TOI file already created, just read from that. If not, calculate.
		 */
		if (useSavedFiles && file.exists()) {
			try {
				f = new BufferedReader(new FileReader(file));
				f.readLine();
				while (true)
					try {
						s = f.readLine().split("\t");
						map.put(s[0].trim(), Integer.parseInt(s[1].trim()));
					} catch (NullPointerException npe) {
						f.close();
						System.out.println("Read TOI from file");
						return map;
					}
			} catch (IOException ioe) {
				System.out.println("Trouble reading from TOI file for "
                                   + season + "; generating from scratch");
			}
		}
        
		int thisgame;
		Map<String, Integer> totalTOnI = new HashMap<String, Integer>();
		Map<String, Integer> toiInThisGame = null;
		for (String teamname : ABBREVS.keySet())
			if (teamname.length() == 3)
				try {
					f = new BufferedReader(new FileReader(DownloadPlayByPlay.FOLDERPATHTOI
                                                          + season + "/" + teamname + ".csv"));
					f.readLine();
					f.readLine();
					thisgame = 0;
					/*
					 * Calculate each player's TOI in this game. Calculate OffI
					 * for players playing in this game.
					 */
					toiInThisGame = new HashMap<String, Integer>();
					while (true)
						try {
							s = f.readLine().split(",");
							if (!s[0].equals("" + thisgame)) {
								thisgame = Integer.parseInt(s[0]);
								for (String pname : toiInThisGame.keySet()) {
									if (!totalTOnI.containsKey(pname))
										totalTOnI.put(pname, 0);
									totalTOnI.put(pname, totalTOnI.get(pname)
                                                  + toiInThisGame.get(pname));
								}
								toiInThisGame = new HashMap<String, Integer>();
							}
							/* Need goalies in */
							if (checkStrengthAndGoaliesIn("5v5", s, "toi")) {
								for (int i = 5; i <= 15; i += 2) {
									if (s[i].charAt(0) == 'G'
                                        || s[i].charAt(0) == 'D')
										edit = s[i];
									else
										edit = 'F' + s[i].substring(1);
									if (!toiInThisGame.containsKey(edit))
										toiInThisGame.put(edit, 1);
									else
										toiInThisGame.put(edit,
                                                          toiInThisGame.get(edit) + 1);
								}
							}
							/*
							 * At end of file, add this game's data to season
							 * data
							 */
						} catch (NullPointerException npe) {
							for (String pname : toiInThisGame.keySet()) {
								if (!totalTOnI.containsKey(pname))
									totalTOnI.put(pname, 0);
								totalTOnI.put(pname, totalTOnI.get(pname)
                                              + toiInThisGame.get(pname));
							}
							f.close();
							break;
						}
					System.out.println("Done reading " + season + " TOI for "
                                       + teamname);
				} catch (IOException ioe) {
					System.out.println("Error when calculating TOI:\t" + ioe);
				}
		/* Calculate TOI */
		map = totalTOnI;
        
		/* Print to file for use next time */
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(OUTPUTFOLDERPATH
                                                             + season + " TOI.txt")));
			out.write("Name\tTOI");
			for (String pname : map.keySet())
				out.write("\n" + pname + "\t" + map.get(pname));
			out.close();
		} catch (IOException ioe) {
			System.out.println("Had trouble creating TOI file for " + season
                               + " for future reference");
		}
		return map;
	}
    
	public static Map<String, Double> calculateTOI60(int season) {
        
		Map<String, Double> map = new HashMap<String, Double>();
        
		File file = new File(OUTPUTFOLDERPATH + season + " TOI60.txt");
		String[] s;
		BufferedReader f = null;
        
		if (file.exists() && useSavedFiles) {
			try {
				f = new BufferedReader(new FileReader(file));
				f.readLine();
				while (true)
					try {
						s = f.readLine().split("\t");
						map.put(s[0].trim(), Double.parseDouble(s[1].trim()));
					} catch (NullPointerException npe) {
						f.close();
						return map;
					}
			} catch (IOException ioe) {
				System.out.println("Trouble reading from TOI60 file for "
                                   + season + "; generating from scratch");
			}
		}
        
		int thisgamelength;
		int thisgame;
		Map<String, Integer> totalTOnI = new HashMap<String, Integer>();
		Map<String, Integer> totalTOffI = new HashMap<String, Integer>();
		Map<String, Integer> toiInThisGame;
		for (String teamname : ABBREVS.keySet())
			if (teamname.length() == 3)
				try {
					f = new BufferedReader(new FileReader(DownloadPlayByPlay.FOLDERPATHTOI
                                                          + season + "/" + teamname + ".csv"));
					f.readLine();
					f.readLine();
					thisgamelength = 0;
					thisgame = 0;
					toiInThisGame = new HashMap<String, Integer>();
					while (true)
						try {
							s = f.readLine().split(",");
							if (!s[0].equals("" + thisgame)) {
								thisgame = Integer.parseInt(s[0]);
								for (String pname : toiInThisGame.keySet()) {
									if (!totalTOnI.containsKey(pname))
										totalTOnI.put(pname, 0);
									if (!totalTOffI.containsKey(pname))
										totalTOffI.put(pname, 0);
									totalTOnI.put(pname, totalTOnI.get(pname)
                                                  + toiInThisGame.get(pname));
									totalTOffI
                                    .put(pname,
                                         totalTOffI.get(pname)
                                         + (thisgamelength - toiInThisGame
                                            .get(pname)));
								}
								toiInThisGame = new HashMap<String, Integer>();
								thisgamelength = 0;
							}
							if (checkStrengthAndGoaliesIn("5v5", s, "toi")) {
								thisgamelength++;
								for (int i = 5; i <= 15; i += 2)
									if (!toiInThisGame.containsKey(s[i].trim()))
										toiInThisGame.put(s[i].trim(), 1);
									else
										toiInThisGame
                                        .put(s[i].trim(), toiInThisGame
                                             .get(s[i].trim()) + 1);
							}
						} catch (NullPointerException npe) {
							for (String pname : toiInThisGame.keySet()) {
								if (!totalTOnI.containsKey(pname))
									totalTOnI.put(pname, 0);
								if (!totalTOffI.containsKey(pname))
									totalTOffI.put(pname, 0);
								totalTOnI.put(pname, totalTOnI.get(pname)
                                              + toiInThisGame.get(pname));
								totalTOffI
                                .put(pname,
                                     totalTOffI.get(pname)
                                     + (thisgamelength - toiInThisGame
                                        .get(pname)));
							}
							f.close();
							break;
						}
					System.out.println("Done reading TOI/60 for " + teamname);
				} catch (IOException ioe) {
					System.out
                    .println("Error when calculating TOI/60:\t" + ioe);
				}
		String edit;
		for (String pname : totalTOnI.keySet()) {
			if (pname.charAt(0) == 'D' || pname.charAt(0) == 'G')
				edit = pname;
			else
				edit = 'F' + pname.substring(1);
			map.put(edit,
					(double) totalTOnI.get(pname)
                    / ((1.0 * totalTOnI.get(pname) + totalTOffI
                        .get(pname)) / 60.0));
		}
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(OUTPUTFOLDERPATH
                                                             + season + " TOI60.txt")));
			out.write("Name\tTOI/60");
			for (String pname : map.keySet())
				out.write("\n" + pname + "\t" + map.get(pname));
			out.close();
		} catch (IOException ioe) {
			System.out.println("Had trouble creating TOI60 file for " + season
                               + " for future reference");
		}
		return map;
	}
    
	public static Map<String, Map<String, String>> readRosters(int season)
    throws IOException {
        
		Map<String, Map<String, String>> teamToNameToPosNum = new HashMap<String, Map<String, String>>();
		BufferedReader f = new BufferedReader(new FileReader(
                                                             DownloadPlayByPlay.FOLDERPATHTOI + season + " rosters.txt"));
		String currentteam = null;
		String line = null;
		String pos;
		while (true)
			try {
				line = f.readLine();
				if (line.contains("(")
                    && ABBREVS.containsKey(line.substring(
                                                          line.indexOf("(") + 1, line.indexOf(")")))) {
					currentteam = line.substring(line.indexOf("(") + 1,
                                                 line.indexOf(")"));
					teamToNameToPosNum.put(currentteam,
                                           new HashMap<String, String>());
				} else if (line.trim().length() > 2) {
					pos = line.substring(0, line.indexOf("\t")) + " ";
					/* Change R, L, C to F */
					if (line.charAt(line.indexOf("\t") + 1) == 'G'
                        || line.charAt(line.indexOf("\t") + 1) == 'D')
						pos = pos + line.charAt(line.indexOf("\t") + 1);
					else
						pos = pos + 'F';
					line = line.substring(line.indexOf("\t") + 2).trim();
					line = editName(line);
					teamToNameToPosNum.get(currentteam).put(line, pos);
				}
			} catch (NullPointerException npe) {
				f.close();
				break;
			}
		return teamToNameToPosNum;
	}
    
	public static List<Map<String, String>> calculateCompFD(String team,
                                                            int season, int startg, int endg,
                                                            Map<Integer, Map<String, Double>> seasonToPlayerToCompVal,
                                                            Map<String, Map<String, String>> teamrosters) {
        
		Map<String, Double> compF = new HashMap<String, Double>();
		Map<String, Double> compD = new HashMap<String, Double>();
		Map<String, Integer> totalmantoiF = new HashMap<String, Integer>();
		Map<String, Integer> totalmantoiD = new HashMap<String, Integer>();
		// just add 1 for each F faced
		String edit = null;
		for (String pname : teamrosters.get(team).keySet()) {
			edit = teamrosters.get(team).get(pname) + " " + editName(pname);
			edit = edit.substring(edit.indexOf(" ") + 1);
			if (edit.charAt(0) != 'D' && edit.charAt(0) != 'G')
				edit = 'F' + edit.substring(1);
			if (!compF.containsKey(edit)) {
				compF.put(edit, 0.0);
				compD.put(edit, 0.0);
				totalmantoiF.put(edit, 0);
				totalmantoiD.put(edit, 0);
			}
		}
        
		BufferedReader f = null;
		String[] s = null;
		double toaddF;
		double toaddD;
		int dfacing;
		int ffacing;
		String st = null;
		int gamenum;
		try {
			f = new BufferedReader(new FileReader(DownloadPlayByPlay.FOLDERPATHTOI + season
                                                  + "/" + team + ".csv"));
			f.readLine();
			f.readLine();
			while (true)
				try {
					s = f.readLine().split(",");
					gamenum = Integer.parseInt(s[0]);
					if (gamenum >= startg && gamenum <= endg
                        && checkStrengthAndGoaliesIn("5v5", s, "toi")) {
						toaddF = 0;
						toaddD = 0;
						dfacing = 0;
						ffacing = 0;
						/* Read players on ice */
						for (int i = 5; i <= 15; i += 2) {
							if (s[i].charAt(0) != 'D' && s[i].charAt(0) != 'G')
								edit = 'F' + s[i].substring(1);
							else
								edit = s[i];
							try {
								if (edit.charAt(0) == 'D') {
									toaddD = toaddD
                                    + seasonToPlayerToCompVal.get(
                                                                  season).get(edit);
									dfacing++;
								} else if (edit.charAt(0) != 'G') // forward
								{
									toaddF = toaddF
                                    + seasonToPlayerToCompVal.get(
                                                                  season).get(edit);
									ffacing++;
								}
							} catch (NullPointerException n) {
								edit = tryCorrectingName(edit);
								try {
									if (edit.charAt(0) == 'D') {
										toaddD = toaddD
                                        + seasonToPlayerToCompVal.get(
                                                                      season).get(edit);
										dfacing++;
									} else if (edit.charAt(0) != 'G') // forward
									{
										toaddF = toaddF
                                        + seasonToPlayerToCompVal.get(
                                                                      season).get(edit);
										ffacing++;
									}
								} catch (NullPointerException np) {
									System.out.println('"' + s[i] + '"'
                                                       + " not found");
								}
							}
						}
						for (int i = 17; i <= 27; i += 2) {
							st = s[i].trim();
                            
							if (st.charAt(0) != 'G' && st.charAt(0) != 'D')
								st = "F "
                                + editName(st
                                           .substring(st.indexOf(" ") + 1));
							else
								st = st.substring(0, 2)
                                + editName(st
                                           .substring(st.indexOf(" ") + 1));
							if (!compF.containsKey(st))
								st = tryCorrectingName(st);
							/* Bates Battaglia keeps messing me up */
							if (!compF.containsKey(st))
								st = "F BATES (JON) BATTAGLIA";
							compF.put(st, compF.get(st) + toaddF);
							compD.put(st, compD.get(st) + toaddD);
							totalmantoiF
                            .put(st, totalmantoiF.get(st) + ffacing);
							totalmantoiD
                            .put(st, totalmantoiD.get(st) + dfacing);
						}
					}
				} catch (NullPointerException npe) {
					f.close();
					if (s != null && Integer.parseInt(s[2]) < 3300)
						System.out.println(st + "\t" + s[0] + "\t" + s[2]
                                           + "\n" + compF.keySet().toString());
					break;
				}
			// System.out.println(season);
		} catch (IOException ioe) {
			System.out.println("Error when calculating TOI/60:\t" + ioe);
		}
        
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Map<String, String> map = new HashMap<String, String>();
		Map<String, String> map2 = new HashMap<String, String>();
		for (String key : compF.keySet()) {
			map.put(key, totalmantoiF.get(key) + " " + compF.get(key));
			map2.put(key, totalmantoiD.get(key) + " " + compD.get(key));
		}
		list.add(map);
		list.add(map2);
		System.out.println("Done calculating " + season + " comp for " + team);
		return list;
	}
    
	public static boolean checkStrengthAndGoaliesIn(String strength,
                                                    String[] s, String type) {
		for (int i = 0; i < s.length; i++)
			if (s[i].equals(strength))
				break;
			else if (i > 5)
				return false;
		int count = 0;
		if (type.equalsIgnoreCase("pbp")) {
			for (int i = 12; i < s.length; i += 2)
				if (s[i].length() > 2
                    && s[i].substring(s[i].length() - 2, s[i].length())
                    .equals(" G"))
					count++;
			return count == 2;
		} else {
			for (int i = 5; i < s.length; i++)
				if (s[i].length() > 4 && s[i].substring(0, 2).equals("G "))
					count++;
			return count == 2;
		}
	}
    
	/*
	 * Lots of players named Michael, for example, are sometimes listed as Mike.
	 * Need to standardize--chose short form (which will also cover Alexandre
	 * and Alexander mistakes). Also correct some other spelling mistakes
	 */
	private static String editName(String s) {
		/* s in format ALEX OVECHKIN */
		try {
			if (s.equals("JOSEPH CORVO"))
				return "JOE CORVO";
			if (s.equals("ANDREI KASTITSYN"))
				return "ANDREI KOSTITSYN";
			if (s.equals("ANDREI KASTSITSYN"))
				return "ANDREI KOSTITSYN";
			if (s.equals("SIMEON VARLAMOV"))
				return "SEMYON VARLAMOV";
			if (s.equals("SIARHEI KASTSITSYN"))
				return "SERGEI KOSTITSYN";
			if (s.equals("DAVID JOHNNY ODUYA"))
				return "JOHN ODUYA";
			if (s.equals("JOHNNY ODUYA"))
				return "JOHN ODUYA";
			if (s.equals("JOHN HILLEN III"))
				return "JACK HILLEN";
			if (s.equals("EDWARD PURCELL"))
				return "TEDDY PURCELL";
			if (s.contains("TIMOTHY THOMAS JR"))
				return "TIM THOMAS";
			if (s.equals("HARRISON ZOLNIERCZYK"))
				return "HARRY ZOLNIERCZYK";
			if (s.equals("DAVE STECKEL"))
				return "DAVID STECKEL";
			if (s.equals("ROBERT SCUDERI"))
				return "ROB SCUDERI";
			// if (s.equals("MICHAEL MODANO"))
			// return "MIKE MODANO";
			if (s.equals("MARTIN ST LOUIS"))
				return "MARTIN ST. LOUIS";
			if (s.equals("VACLAV PROSPAL"))
				return "VINNY PROSPAL";
			if (s.equals("VACLAV PROSPAL"))
				return "VINNY PROSPAL";
			if (s.equals("NICKLAS BERGFORS"))
				return "NICLAS BERGFORS";
			if (s.equals("NICKLAS GROSSMANN"))
				return "NICKLAS GROSSMAN";
			if (s.equals("KRISTOPHER LETANG"))
				return "KRIS LETANG";
			if (s.equals("OLIVIER MAGNAN"))
				return "OLIVIER MAGNAN-GRENIER";
			if (s.equals("J P DUMONT"))
				return "J.P. DUMONT";
			if (s.equals("PIERRE PARENTEAU"))
				return "PA PARENTEAU";
			if (s.equals("PK SUBBAN"))
				return "P.K. SUBBAN";
			if (s.equals("RJ UMBERGER"))
				return "R.J. UMBERGER";
			if (s.equals("JAMES WYMAN"))
				return "J.T. WYMAN";
			if (s.contains("MICHAEL")) {
				String s2 = s.substring(0, s.indexOf("MICHAEL"));
				s = s.substring(s.indexOf("MICHAEL"));
				s = s.substring(s.indexOf(" "));
				return s2 + "MIKE" + s;
			}
			if (s.contains("NIKOLAI")) {
				String s2 = s.substring(0, s.indexOf("NIKOLAI"));
				s = s.substring(s.indexOf("NIKOLAI"));
				s = s.substring(s.indexOf(" "));
				return s2 + "NIK" + s;
			}
			if (s.contains("NIKOLAY")) {
				String s2 = s.substring(0, s.indexOf("NIKOLAY"));
				s = s.substring(s.indexOf("NIKOLAY"));
				s = s.substring(s.indexOf(" "));
				return s2 + "NIK" + s;
			}
			if (s.contains("MATTHEW")) {
				String s2 = s.substring(0, s.indexOf("MATTHEW"));
				s = s.substring(s.indexOf("MATTHEW"));
				s = s.substring(s.indexOf(" "));
				return s2 + "MATT" + s;
			}
			if (s.contains("ALEXANDER")) {
				String s2 = s.substring(0, s.indexOf("ALEXANDER"));
				s = s.substring(s.indexOf("ALEXANDER"));
				s = s.substring(s.indexOf(" "));
				return s2 + "ALEX" + s;
			}
			if (s.contains("ALEXANDRE")) {
				String s2 = s.substring(0, s.indexOf("ALEXANDRE"));
				s = s.substring(s.indexOf("ALEXANDRE"));
				s = s.substring(s.indexOf(" "));
				return s2 + "ALEX" + s;
			}
			if (s.contains("ALEXEI")) {
				String s2 = s.substring(0, s.indexOf("ALEXEI"));
				s = s.substring(s.indexOf("ALEXEI"));
				s = s.substring(s.indexOf(" "));
				return s2 + "ALEX" + s;
			}
			if (s.contains("JACOB")) {
				String s2 = s.substring(0, s.indexOf("JACOB"));
				s = s.substring(s.indexOf("JACOB"));
				s = s.substring(s.indexOf(" "));
				return s2 + "JAKE" + s;
			}
			if (s.contains("DANIEL")) {
				String s2 = s.substring(0, s.indexOf("DANIEL"));
				s = s.substring(s.indexOf("DANIEL"));
				s = s.substring(s.indexOf(" "));
				return s2 + "DAN" + s;
			}
			if (s.contains("DANNY")) {
				String s2 = s.substring(0, s.indexOf("DANNY"));
				s = s.substring(s.indexOf("DANNY"));
				s = s.substring(s.indexOf(" "));
				return s2 + "DAN" + s;
			}
			if (s.contains("EVGENII")) {
				String s2 = s.substring(0, s.indexOf("EVGENII"));
				s = s.substring(s.indexOf("EVGENII"));
				s = s.substring(s.indexOf(" "));
				return s2 + "EVGENI" + s;
			}
			if (s.contains("EVGENY")) {
				String s2 = s.substring(0, s.indexOf("EVGENY"));
				s = s.substring(s.indexOf("EVGENY"));
				s = s.substring(s.indexOf(" "));
				return s2 + "EVGENI" + s;
			}
			if (s.contains("JONATHAN")) {
				String s2 = s.substring(0, s.indexOf("JONATHAN"));
				s = s.substring(s.indexOf("JONATHAN"));
				s = s.substring(s.indexOf(" "));
				return s2 + "JOHN" + s;
			}
			if (s.contains("NICHOLAS")) {
				String s2 = s.substring(0, s.indexOf("NICHOLAS"));
				s = s.substring(s.indexOf("NICHOLAS"));
				s = s.substring(s.indexOf(" "));
				return s2 + "NICK" + s;
			}
			if (s.contains("ANDY")) {
				String s2 = s.substring(0, s.indexOf("ANDY"));
				s = s.substring(s.indexOf("ANDY"));
				s = s.substring(s.indexOf(" "));
				return s2 + "ANDREW" + s;
			}
			if (s.contains("NATE")) {
				String s2 = s.substring(0, s.indexOf("NATE"));
				s = s.substring(s.indexOf("NATE"));
				s = s.substring(s.indexOf(" "));
				return s2 + "NATHAN" + s;
			}
			if (s.contains("JOSHUA")) {
				String s2 = s.substring(0, s.indexOf("JOSHUA"));
				s = s.substring(s.indexOf("JOSHUA"));
				s = s.substring(s.indexOf(" "));
				return s2 + "JOSH" + s;
			}
			if (s.contains("TJ ")) {
				String s2 = s.substring(0, s.indexOf("TJ "));
				s = s.substring(s.indexOf("TJ "));
				s = s.substring(s.indexOf(" "));
				return s2 + "T.J." + s;
			}
			if (s.contains("CHRISTOPHER")) {
				String s2 = s.substring(0, s.indexOf("CHRISTOPHER"));
				s = s.substring(s.indexOf("CHRISTOPHER"));
				s = s.substring(s.indexOf(" "));
				return s2 + "CHRIS" + s;
			}
			if (s.contains("BRADLEY ")) {
				String s2 = s.substring(0, s.indexOf("BRADLEY "));
				s = s.substring(s.indexOf("BRADLEY "));
				s = s.substring(s.indexOf(" "));
				return s2 + "BRAD" + s;
			}
			if (s.contains("RODNEY ")) {
				String s2 = s.substring(0, s.indexOf("RODNEY "));
				s = s.substring(s.indexOf("RODNEY "));
				s = s.substring(s.indexOf(" "));
				return s2 + "ROD" + s;
			}
			if (s.contains("MARC-ANTOINE")) {
				String s2 = s.substring(0, s.indexOf("MARC-ANTOINE"));
				s = s.substring(s.indexOf("MARC-ANTOINE"));
				s = s.substring(s.indexOf(" "));
				return s2 + "MARC" + s;
			}
			if (s.contains("STEVE ")) {
				String s2 = s.substring(0, s.indexOf("STEVE "));
				s = s.substring(s.indexOf("STEVE "));
				s = s.substring(s.indexOf(" "));
				return s2 + "STEVEN" + s;
			}
			if (s.contains("CROMBEEN"))
				return "B.J. CROMBEEN";
		} catch (StringIndexOutOfBoundsException sioobe) {
			return s;
		}
		return s;
	}
    
	private static String checkNameForPosChange(String s) {
		/* format of s: D DUSTIN BYFUGLIEN */
        
		if (s.equals("D DUSTIN BYFUGLIEN"))
			return "F DUSTIN BYFUGLIEN";
		if (s.equals("F DUSTIN BYFUGLIEN"))
			return "D DUSTIN BYFUGLIEN";
        
		if (s.equals("D MARC-ANDRE GRAGNANI"))
			return "F MARC-ANDRE GRAGNANI";
		if (s.equals("F MARC-ANDRE GRAGNANI"))
			return "D MARC-ANDRE GRAGNANI";
        
		if (s.equals("D BRENT BURNS"))
			return "F BRENT BURNS";
		if (s.equals("F BRENT BURNS"))
			return "D BRENT BURNS";
        
		if (s.equals("D JOHN SCOTT"))
			return "F JOHN SCOTT";
		if (s.equals("F JOHN SCOTT"))
			return "D JOHN SCOTT";
        
		if (s.equals("D BRAD STAUBITZ"))
			return "F BRAD STAUBITZ";
		if (s.equals("F BRAD STAUBITZ"))
			return "D BRAD STAUBITZ";
        
		if (s.equals("D TIM CONBOY"))
			return "F TIM CONBOY";
		if (s.equals("F TIM CONBOY"))
			return "D TIM CONBOY";
        
		if (s.equals("D WADE BROOKBANK"))
			return "F WADE BROOKBANK";
		if (s.equals("F WADE BROOKBANK"))
			return "D WADE BROOKBANK";
        
		/* Not sure the following players actually switched... */
		if (s.equals("D CHRISTOPH SCHUBERT"))
			return "F CHRISTOPH SCHUBERT";
		if (s.equals("F CHRISTOPH SCHUBERT"))
			return "D CHRISTOPH SCHUBERT";
        
		if (s.equals("D ALEX PICARD"))
			return "F ALEX PICARD";
		if (s.equals("F ALEX PICARD"))
			return "D ALEX PICARD";
        
		if (s.equals("D MATHIEU DANDENAULT"))
			return "F MATHIEU DANDENAULT";
		if (s.equals("F MATHIEU DANDENAULT"))
			return "D MATHIEU DANDENAULT";
        
		return null;
	}
    
	/*
	 * private static String editPosNum(String s) { // s in format 8 L String
	 * edit = s.substring(0, s.indexOf(" ") + 1); if (s.charAt(s.length() - 1)
	 * == 'G') edit = edit + "G"; else if (s.charAt(s.length() - 1) == 'D') edit
	 * = edit + "D"; else edit = edit + "F"; return edit; }
	 */
	private static String tryCorrectingName(String name) {
		if (name.contains("BA(JON) BATTAGLIA"))
			return "F BATES (JON) BATTAGLIA";
		if (name.equals("D ALEX PICARD"))
			return "F ALEX PICARD";
		if (name.equals("D ALEXANDRE PICARD"))
			return "F ALEXANDRE PICARD";
		if (name.equals("L GARTH MURRAY"))
			return "C GARTH MURRAY";
		if (name.equals("L FREDRIK SJOSTROM"))
			return "R FREDRIK SJOSTROM";
		if (name.equals("L BRANDON PRUST"))
			return "R BRANDON PRUST";
        
		return name;
	}
}