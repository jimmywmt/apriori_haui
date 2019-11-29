package tw.idv.wmt35.apriori_haui;

import static java.lang.System.exit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.*;
import tw.idv.wmt35.MutableDouble;
import tw.idv.wmt35.Pair;

/**
 * This class is the main class for apriori_haui It mines the high average utility from a
 * transaction database by an effective apriori algorithm
 *
 * @author Jimmy Ming-Tai Wu
 * @version 2.00
 */
public final class App {
  private static final double VERSION = 2.00;

  private App() {}

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("h", false, "Lists Short Help");
    options.addOption("f", true, "Original Transaction Data Set Files");
    options.addOption("m", true, "Set Minimal Utility Threshold");
    options.addOption("p", true, "Set PreLarge Threshold");
    options.addOption("c", false, "Check Minimal Utility Count");
    options.addOption("nlu", false, "Don't Use Lead Upperbound");
    CommandLineParser parser = new DefaultParser();
    HelpFormatter hf = new HelpFormatter();
    hf.setWidth(150);

    String datasetFile = "";
    double minSup = 0.0;
    double preLargeThreshold = 0.0;
    long numTransaction;
    int maxItemID;
    long totalUtility;
    double minSupCount = 0.0;
    double preLargeCount = 0.0;
    boolean applyPreLarge = false;
    boolean useLeadUpperbound = true;
    String[] fileArray = null;

    try {
      CommandLine cmd = parser.parse(options, args);
      if (cmd.hasOption("c") && cmd.hasOption("f")) {
        datasetFile = cmd.getOptionValue("f");
        fileArray = datasetFile.split(",");
        minSup = Double.parseDouble(cmd.getOptionValue("m"));
        Apriori apriori = new Apriori();
        apriori.readFile(fileArray[0]);
        apriori.setMinSup(minSup);
        System.out.println((int) Math.ceil(apriori.getMinSupCount()));
        exit(0);
      }

      System.out.println("===========================================================");
      System.out.println("High Average Utility Itemsets Mining by Apriori Algorithm");
      System.out.println("===========================================================");
      System.out.println("Authoer: Jimmy Ming-Tai Wu (wmt@wmt35.idv.tw)");
      System.out.println(new StringBuilder("Version: ").append(VERSION));
      System.out.println("===========================================================");

      if (cmd.hasOption("h")) {
        hf.printHelp("java -jar AprioriHaui.jar", options, true);
        System.out.println("===========================================================");
        exit(0);
      }

      if (cmd.getOptions().length == 0) {
        hf.printHelp("java -jar AprioriHaui.jar", options, true);
        System.out.println("===========================================================");
        exit(1);
      }

      if (!cmd.hasOption("f") || !cmd.hasOption("m")) {
        hf.printHelp("java -jar AprioriHaui.jar", options, true);
        System.out.println("===========================================================");
        exit(1);
      }

      datasetFile = cmd.getOptionValue("f");
      fileArray = datasetFile.split(",");
      minSup = Double.parseDouble(cmd.getOptionValue("m"));

      if (cmd.hasOption("p")) {
        preLargeThreshold = Double.parseDouble(cmd.getOptionValue("p"));
        applyPreLarge = true;
      }

      if (minSup < 0 || minSup > 1) {
        System.out.println("Minimal Utility Threshold should be set in 0 ~ 1");
        System.out.println("===========================================================");
        exit(1);
      }

      useLeadUpperbound = !cmd.hasOption("nlu");
    } catch (ParseException e) {
      e.printStackTrace();
    }

    Apriori apriori = new Apriori();

    if (!useLeadUpperbound) {
      apriori.setLeadUpperbound(false);
    }

    if (fileArray.length == 1) {
      System.out.println("(Running Time Start TimeStamp)");
      long startTime = System.currentTimeMillis();
      System.out.println("Data Importing...");

      apriori.readFile(fileArray[0]);
      apriori.setMinSup(minSup);
      if (applyPreLarge) apriori.setPreLargeThreshold(preLargeThreshold);

      numTransaction = apriori.getNumTransactions();
      maxItemID = apriori.getMaxItemID();
      totalUtility = apriori.getTotalUtility();
      minSupCount = apriori.getMinSupCount();
      if (applyPreLarge) preLargeCount = apriori.getPreLargeCount();

      System.out.println();
      System.out.println("--------------------");
      System.out.println("Experimental Profile");
      System.out.println("--------------------");
      System.out.println(new StringBuilder("Dataset:                   ").append(datasetFile));
      System.out.println(new StringBuilder("Number of Transaction:     ").append(numTransaction));
      System.out.println(new StringBuilder("Maximal ID:                ").append(maxItemID));
      System.out.println(new StringBuilder("Total Utility:             ").append(totalUtility));
      System.out.println(new StringBuilder("Minimal Utility Threshold: ").append(minSup));
      System.out.println(new StringBuilder("Minimal Utility Count:     ").append(minSupCount));
      if (applyPreLarge) {
        System.out.println(
            new StringBuilder("PreLarge Threshold:        ").append(preLargeThreshold));
        System.out.println(new StringBuilder("PreLarge Count:            ").append(preLargeCount));
      }
      System.out.print("Use Lead Upperbound:       ");
      if (apriori.getLeadUpperbound()) {
        System.out.println("Yes");
      } else {
        System.out.println("No");
      }
      System.out.println("--------------------");
      System.out.println();
      System.out.println("Process Running...");
      apriori.run();
      System.out.println("Process Finish...");
      long stopTime = System.currentTimeMillis();
      System.out.println("(Running Time Stop TimeStamp)");
      Map<String, Double> highAUtilityItemsetsCount = apriori.getHighAUtilityItemsetsCount();
      Map<String, Double> preLargeUtilityItemsetsCount = apriori.getPreLargeUtilityItemsetsCount();
      System.out.println();
      System.out.println("--------------------");
      System.out.println("Experimental Results");
      System.out.println("--------------------");
      System.out.println(
          new StringBuilder("Running Time: ").append(stopTime - startTime).append("ms"));
      System.out.println(
          new StringBuilder("Number of Candidates: ").append(apriori.getTotalCandidateCount()));
      System.out.println(
          new StringBuilder("Number of HAUIs: ").append(highAUtilityItemsetsCount.size()));
      if (highAUtilityItemsetsCount.size() != 0) {
        System.out.println("ITEMSET : AVERAGE UTILITY");
        for (Map.Entry<String, Double> pair : highAUtilityItemsetsCount.entrySet()) {
          System.out.println(
              new StringBuilder(pair.getKey()).append(" : ").append(pair.getValue()));
        }
      }

      if (applyPreLarge) {
        System.out.println(
            new StringBuilder("Number of PreLarges: ").append(preLargeUtilityItemsetsCount.size()));
        if (preLargeUtilityItemsetsCount.size() != 0) {
          System.out.println("ITEMSET : AVERAGE UTILITY");
          for (Map.Entry<String, Double> pair : preLargeUtilityItemsetsCount.entrySet()) {
            System.out.println(
                new StringBuilder(pair.getKey()).append(" : ").append(pair.getValue()));
          }
        }
      }

      System.out.println("===========================================================");
    } else if (fileArray.length > 1) {
      if (!applyPreLarge) {
        System.out.println("Need to Setup PreLarge Threshold!!");
        exit(1);
      }
      System.out.println();
      System.out.println("-----------------------------------------------");
      System.out.println("Experimental Profile (Incremental experiment!!)");
      System.out.println("-----------------------------------------------");
      System.out.print("Use Lead Upperbound:       ");
      if (apriori.getLeadUpperbound()) {
        System.out.println("Yes");
      } else {
        System.out.println("No");
      }
      System.out.println(new StringBuilder("The Number of Datasets: ").append(fileArray.length));
      int reScanRemainingUtility = 0;
      List<String> reScanDBList = new ArrayList<>();
      Map<String, Double> highAUtilityItemsetsCount = null;
      Map<String, Double> preLargeUtilityItemsetsCount = null;
      int countReScan = 0;
      int countDB = 0;
      for (String db : fileArray) {
        ++countDB;
        System.out.println(new StringBuilder("DB (").append(countDB).append(") : ").append(db));
      }
      System.out.println(new StringBuilder("Minimal Support: ").append(minSup));
      System.out.println(new StringBuilder("Prelarge Threshold: ").append(preLargeThreshold));

      System.out.println("(Running Time Start TimeStamp)");
      long startTime = System.currentTimeMillis();
      totalUtility = 0;
      long iTotalUtility;
      for (String db : fileArray) {
        long startDBTime = System.currentTimeMillis();
        reScanDBList.add(db);
        System.out.println(new StringBuilder("Data Importing... (").append(db).append(")"));
        apriori.readFile(db);
        apriori.setMinSup(minSup);
        apriori.setPreLargeThreshold(preLargeThreshold);
        numTransaction = apriori.getNumTransactions();
        iTotalUtility = apriori.getTotalUtility();
        totalUtility += iTotalUtility;
        minSupCount = totalUtility * minSup;
        preLargeCount = totalUtility * preLargeThreshold;
        System.out.println(new StringBuilder("Number of Transaction:     ").append(numTransaction));
        System.out.println(new StringBuilder("Total Utility:             ").append(iTotalUtility));

        if (iTotalUtility >= reScanRemainingUtility) {
          if (reScanDBList.size() == 1) {
            System.out.println("Process Running...");
            apriori.run();
            System.out.println("Process Finish...");
            System.out.println();
          } else {
            System.out.println("Re-Scan...");
            System.out.println("Merged Data Importing...");
            apriori.readFile(reScanDBList);
            apriori.setMinSup(minSup);
            apriori.setPreLargeThreshold(preLargeThreshold);
            totalUtility = apriori.getTotalUtility();
            System.out.println("Process Running...");
            apriori.run();
            System.out.println("Process Finish...");
          }
          reScanRemainingUtility =
              (int) Math.ceil(((minSup - preLargeThreshold) * totalUtility) / minSup);
          highAUtilityItemsetsCount = apriori.getHighAUtilityItemsetsCount();
          preLargeUtilityItemsetsCount = apriori.getPreLargeUtilityItemsetsCount();
        } else {
          System.out.println("Do Incremental Process...");
          System.out.println("Process Running...");
          apriori.run();
          System.out.println("Process Finish...");

          // update large itemsets and prelarge
          Map<String, Double> tempHigh = apriori.getHighAUtilityItemsetsCount();
          Map<String, Double> tempPreLarge = apriori.getPreLargeUtilityItemsetsCount();
          List<Pair<String, Double>> tempToPreLarge = new ArrayList<>();
          List<Pair<String, MutableDouble>> tempHToReScan = new ArrayList<>();
          List<Pair<String, MutableDouble>> tempPToReScan = new ArrayList<>();
          List<String> removeKeyList = new ArrayList<>();
          int numReScan = 0;

          for (Map.Entry<String, Double> entry : highAUtilityItemsetsCount.entrySet()) {
            String key = entry.getKey();
            Double originalValue = entry.getValue();
            Double value = tempHigh.get(key);

            if (value != null) {
              highAUtilityItemsetsCount.replace(key, originalValue + value);
            } else {
              value = tempPreLarge.get(key);

              if (value != null) {
                value += originalValue;

                if (value >= minSupCount) {
                  highAUtilityItemsetsCount.replace(key, value);
                } else {
                  removeKeyList.add(key);
                  tempToPreLarge.add(new Pair<>(key, value));
                }
              } else {
                tempHToReScan.add(new Pair<>(key, new MutableDouble(originalValue)));
              }
            }
          }
          for (String removeKey : removeKeyList) highAUtilityItemsetsCount.remove(removeKey);

          removeKeyList.clear();

          for (Map.Entry<String, Double> entry : preLargeUtilityItemsetsCount.entrySet()) {
            String key = entry.getKey();
            Double originalValue = entry.getValue();
            Double value = tempHigh.get(key);

            if (value != null) {
              value += originalValue;
              if (value >= minSupCount) {
                removeKeyList.add(key);
                highAUtilityItemsetsCount.put(key, value);
              } else preLargeUtilityItemsetsCount.replace(key, value);
            } else {
              value = tempPreLarge.get(key);
              if (value != null) preLargeUtilityItemsetsCount.replace(key, originalValue + value);
              else tempPToReScan.add(new Pair<>(key, new MutableDouble(originalValue)));
            }
          }

          for (String removeKey : removeKeyList) preLargeUtilityItemsetsCount.remove(removeKey);

          for (Pair<String, Double> entry : tempToPreLarge)
            preLargeUtilityItemsetsCount.put(entry.first, entry.second);

          apriori.reScan(tempHToReScan, tempPToReScan);

          for (Pair<String, MutableDouble> entry : tempHToReScan) {
            String key = entry.first;
            double value = entry.second.getValue();

            if (value < minSupCount) {
              highAUtilityItemsetsCount.remove(key);
              if (value >= preLargeCount) {
                preLargeUtilityItemsetsCount.put(key, value);
              }
            } else highAUtilityItemsetsCount.replace(key, value);
          }

          for (Pair<String, MutableDouble> entry : tempPToReScan) {
            String key = entry.first;
            double value = entry.second.getValue();

            if (value < preLargeCount) preLargeUtilityItemsetsCount.remove(key);
            else preLargeUtilityItemsetsCount.replace(key, value);
          }

          numReScan += tempHToReScan.size();
          numReScan += tempPToReScan.size();
          countReScan += numReScan;

          reScanRemainingUtility -= iTotalUtility;
          System.out.println(new StringBuilder("Number of Rescan: ").append(numReScan));
        }
        long stopDBTime = System.currentTimeMillis();
        System.out.println(
            new StringBuilder("Number of Candidates: ").append(apriori.getTotalCandidateCount()));
        System.out.println(
            new StringBuilder("DB Running Time: ").append(stopDBTime - startDBTime).append("ms"));
        System.out.println();
      }
      long stopTime = System.currentTimeMillis();
      System.out.println("--------------------");
      System.out.println("Experimental Results");
      System.out.println("--------------------");
      System.out.println(new StringBuilder("Total Utility:             ").append(totalUtility));
      System.out.println(new StringBuilder("Minimal Utility Threshold: ").append(minSup));
      System.out.println(new StringBuilder("Minimal Utility Count:     ").append(minSupCount));
      System.out.println(new StringBuilder("Number of Rescan: ").append(countReScan));
      System.out.println(
          new StringBuilder("Number of HAUIs: ").append(highAUtilityItemsetsCount.size()));
      System.out.println(
          new StringBuilder("Running Time: ").append(stopTime - startTime).append("ms"));
      System.out.println(
          new StringBuilder("The Maximum Memory Usage: ")
              .append(apriori.getMaxMemory())
              .append("MB"));
      if (highAUtilityItemsetsCount.size() != 0) {
        System.out.println("ITEMSET : AVERAGE UTILITY");
        for (Map.Entry<String, Double> pair : highAUtilityItemsetsCount.entrySet()) {
          System.out.println(
              new StringBuilder(pair.getKey()).append(" : ").append(pair.getValue()));
        }
      }
    }
  }
}
