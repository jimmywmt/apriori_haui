package tw.idv.wmt35.apriori_haui;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import tw.idv.wmt35.MutableDouble;
import tw.idv.wmt35.Pair;
import tw.idv.wmt35.Quaternionic;

/**
 * It is an implementation of effective apriori algorithm to find the high average utility itemsets
 * in a transaction database
 *
 * @author Jimmy Ming-Tai Wu
 */
final class Apriori {
  static final boolean DEFAULT_LEAD_UPPERBOUND = true;

  private long totalUtility;
  private List<Record> data;
  private final List<Integer> relatedTransactions;
  private double minSup;
  private Double minSupCount;
  private double preLargeThreshold;
  private Double preLargeCount;
  private Double thresholdCount;
  private Map<String, Double> highAUtilityItemsetsCount;
  private Map<String, Double> preLargeUtilityItemsetsCount;
  private ArrayList<int[]> highMUtilityItemsets;
  private int maxItemID;
  private int maxKey;
  private double maxMemory;
  private int totalCandidateCount;
  private Set<Integer> reserved;
  private ArrayList<int[]> combinedItemsets;
  private boolean leadUpperbound;

  private static final class Record {
    // first: name, second:  item utility
    final List<Pair<Integer, Integer>> items;
    int tranUtility;
    int maxUtility;

    Record() {
      items = new ArrayList<>();
    }
  }

  Apriori() {
    relatedTransactions = new LinkedList<>();
    maxMemory = 0;
    checkMemory();
    leadUpperbound = DEFAULT_LEAD_UPPERBOUND;
  }

  private void checkMemory() {
    double currentMemory =
        (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d / 1024d;
    if (currentMemory > maxMemory) {
      maxMemory = currentMemory;
    }
  }

  public double getMaxMemory() {
    return maxMemory;
  }

  public int getTotalCandidateCount() {
    return totalCandidateCount;
  }

  public boolean getLeadUpperbound() {
    return leadUpperbound;
  }

  public void setLeadUpperbound(final boolean leadUpperbound) {
    this.leadUpperbound = leadUpperbound;
  }

  public void readFile(String inputFileName) {

    // clear data set
    maxItemID = 0;
    totalUtility = 0;
    relatedTransactions.clear();
    totalCandidateCount = 0;
    data = new ArrayList<>();

    try {
      BufferedReader read =
          new BufferedReader(new InputStreamReader(new FileInputStream(inputFileName)));
      // each line is a transaction
      String line;
      while ((line = read.readLine()) != null) {
        Record aRecord = new Record();
        String[] split = line.split(":");
        // items : tranUtility : utilityValues
        String[] items = split[0].split(" ");
        String[] utilityValues = split[2].split(" ");
        aRecord.tranUtility = Integer.parseInt(split[1]);
        for (int i = 0; i < items.length; ++i) {
          int name = Integer.parseInt(items[i]);
          int utility = Integer.parseInt(utilityValues[i]);
          maxItemID = Integer.max(maxItemID, name);
          aRecord.maxUtility = Integer.max(aRecord.maxUtility, utility);
          Pair<Integer, Integer> entry = new Pair<>(name, utility);
          aRecord.items.add(entry);
        }
        totalUtility += aRecord.tranUtility;
        // initial relatedTransactions, data.size() is the ID for aRecord
        relatedTransactions.add(data.size());
        data.add(aRecord);
      }
      // maxKey is the maximal key for the recent level, maxItemID is for global
      maxKey = maxItemID;
      read.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void readFile(List<String> inputFileArray) {

    // clear data set
    maxItemID = 0;
    totalUtility = 0;
    relatedTransactions.clear();
    data = new ArrayList<>();

    try {
      for (String file : inputFileArray) {
        BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        while ((line = read.readLine()) != null) {
          Record aRecord = new Record();
          String[] split = line.split(":");
          String[] items = split[0].split(" ");
          String[] utilityValue = split[2].split(" ");
          aRecord.tranUtility = Integer.parseInt(split[1]);
          for (int i = 0; i < items.length; ++i) {
            int name = Integer.parseInt(items[i]);
            int utility = Integer.parseInt(utilityValue[i]);
            maxItemID = Integer.max(maxItemID, name);
            aRecord.maxUtility = Integer.max(aRecord.maxUtility, utility);
            Pair<Integer, Integer> entry = new Pair<>(name, utility);
            aRecord.items.add(entry);
          }
          totalUtility += aRecord.tranUtility;
          relatedTransactions.add(data.size());
          data.add(aRecord);
        }
        read.close();
      }
      maxKey = maxItemID;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public double getMinSupCount() {
    return minSupCount;
  }

  public double getPreLargeCount() {
    return preLargeCount;
  }

  public long getTotalUtility() {
    return totalUtility;
  }

  public long getNumTransactions() {
    return data.size();
  }

  public int getMaxItemID() {
    return maxItemID;
  }

  public void setMinSup(double minSup) {
    this.minSup = minSup;
    minSupCount = totalUtility * minSup;

    thresholdCount = minSupCount;
  }

  public void setPreLargeThreshold(double preLargeThreshold) {
    // preLargeThreshold is less or equal to minSup
    this.preLargeThreshold = Double.min(preLargeThreshold, minSup);
    preLargeCount = totalUtility * this.preLargeThreshold;

    thresholdCount = preLargeCount;
  }

  private Pair<Integer, int[]> mergeItemsets(int[] one, int[] two) {
    int i = 0, j = 0, k = 0, d = 0;
    int length = one.length;
    int[] newItemset = new int[length + 1];
    int tempIndex = -1;

    while (i < length) {
      if (one[i] == two[j]) {
        newItemset[k] = one[i];
        ++i;
        ++j;
      } else if (one[i] < two[j]) {

        ++d;
        if (d < 2) {
          newItemset[k] = one[i];
          tempIndex = i;
          ++i;
        } else {
          newItemset[0] = tempIndex;
          return new Pair<>(tempIndex, newItemset);
        }
      } else {
        newItemset[0] = tempIndex;
        return new Pair<>(tempIndex, newItemset);
      }
      ++k;
    }

    newItemset[k] = two[j];
    return new Pair<>(tempIndex, newItemset);
  }

  // find the order in previous level HAUIs
  private int itemsetCompare(int[] one, int[] two) {

    for (int i = 0; i < one.length; ++i) {
      if (one[i] < two[i]) {
        return -1;
      } else if (one[i] > two[i]) {
        return 1;
      }
    }

    return 0;
  }

  private int findPosition(int[] target, int start) {
    // check start
    if (start >= combinedItemsets.size()) {
      return -1;
    }
    if (itemsetCompare(target, combinedItemsets.get(start)) == 0) {
      return start;
    }

    int end = combinedItemsets.size() - 1;
    if (start > end) {
      return -1;
    }

    int middle = (start + end) / 2;
    boolean find = false;

    while (!find) {
      int result = itemsetCompare(target, combinedItemsets.get(middle));

      if (result < 0) {
        end = middle - 1;
      } else if (result > 0) {
        start = middle + 1;
      } else {
        if (middle != start) {
          int result2 = itemsetCompare(target, combinedItemsets.get(middle - 1));

          if (result2 == 0) {
            end = middle - 1;
          } else {
            find = true;
          }
        } else {
          find = true;
        }
      }

      if (!find) {
        if (start > end) {
          return -1;
        }
        middle = (start + end) / 2;
      }
    }

    return middle;
  }

  private static class Candidate {
    int[] itemset;
    int tUtility;
    double mUtility;
    double gMUtility;
    int tempTUtility;
    int checkIndex;
    Record checkRecord;
  }

  private static String idArrayString(int[] itemset) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < itemset.length - 1; ++i) stringBuilder.append(itemset[i]).append(",");

    stringBuilder.append(itemset[itemset.length - 1]);

    return stringBuilder.toString();
  }

  private static int[] idStringArray(String idString) {
    String[] stringArray = idString.split(",");
    int[] intArray = new int[stringArray.length];
    for (int i = 0; i < stringArray.length; ++i) intArray[i] = Integer.parseInt(stringArray[i]);

    return intArray;
  }

  private void nextLevel() {
    ArrayList<int[]> tempMUtilityItemsets = new ArrayList<>();
    ArrayList<int[]> tempCombinedItemsets = new ArrayList<>();
    Set<Integer> tempReserved = new HashSet<>();
    int length = highMUtilityItemsets.get(0).length + 1;
    if (combinedItemsets.size() >= length) {
      ListIterator<int[]> i = highMUtilityItemsets.listIterator();
      List<Candidate> candidateItemset = new ArrayList<>();
      @SuppressWarnings("unchecked")
      List<Candidate>[] mapCandidates = new List[maxKey + 1];
      maxKey = 0;

      // generate candidate list
      while (i.hasNext()) {
        int[] oneItemset = i.next();
        int start = i.nextIndex();
        int skipPostion = oneItemset.length - 1;

        int position;
        int shift = oneItemset[oneItemset.length - 1];
        int[] candidateCount = new int[maxItemID + 1 - shift];
        boolean findCombine;
        while (skipPostion >= 0) {
          if (oneItemset.length > 1) {
            findCombine = false;

            if (skipPostion != oneItemset.length - 1) {

              int[] targetArray = new int[oneItemset.length - 1];
              System.arraycopy(oneItemset, 0, targetArray, 0, skipPostion);
              System.arraycopy(
                  oneItemset,
                  skipPostion + 1,
                  targetArray,
                  skipPostion,
                  oneItemset.length - 1 - skipPostion);
              position = findPosition(targetArray, start);

            } else {
              position = findPosition(oneItemset, start);
              if (position == -1) {
                position = start;
              } else {
                ++position;
              }
            }

            if (position == -1) {
              break;
            }

            for (int j = position; j < combinedItemsets.size(); ++j) {
              int[] twoItemset = combinedItemsets.get(j);

              Pair<Integer, int[]> mergeResult = mergeItemsets(oneItemset, twoItemset);
              int[] threeItemset = mergeResult.second;
              int mergeSkip = mergeResult.first;

              if (threeItemset[threeItemset.length - 1] != 0 && mergeSkip == skipPostion) {
                if (skipPostion != 0) {
                  int index = threeItemset[threeItemset.length - 1] - shift;
                  if ((candidateCount[index] += 1) == oneItemset.length - skipPostion) {
                    findCombine = true;
                  }
                } else {
                  int value = candidateCount[threeItemset[threeItemset.length - 1] - shift];
                  if (value + 1 == oneItemset.length) {
                    Candidate aCandidate = new Candidate();
                    aCandidate.itemset = threeItemset;
                    aCandidate.tUtility = 0;
                    aCandidate.mUtility = 0;
                    candidateItemset.add(aCandidate);
                    for (int name : aCandidate.itemset) {
                      List<Candidate> record = mapCandidates[name];
                      maxKey = Integer.max(maxKey, name);
                      boolean newRecord = record == null;
                      if (newRecord) {
                        record = new LinkedList<>();
                      }
                      record.add(aCandidate);
                      if (newRecord) {
                        mapCandidates[name] = record;
                      }
                    }
                  }
                }
              } else {
                start = j;
                break;
              }
            }

            if (!findCombine) {
              break;
            }
          } else {
            for (int j = start; j < combinedItemsets.size(); ++j) {
              int[] twoItemst = combinedItemsets.get(j);
              Candidate aCandidate = new Candidate();
              aCandidate.itemset = mergeItemsets(oneItemset, twoItemst).second;
              aCandidate.tUtility = 0;
              aCandidate.mUtility = 0;
              candidateItemset.add(aCandidate);
              for (int name : aCandidate.itemset) {
                List<Candidate> record = mapCandidates[name];
                maxKey = Integer.max(maxKey, name);
                boolean newRecord = record == null;
                if (newRecord) {
                  record = new LinkedList<>();
                }
                record.add(aCandidate);
                if (newRecord) {
                  mapCandidates[name] = record;
                }
              }
            }
          }
          --skipPostion;
        }
        totalCandidateCount += candidateItemset.size();
      }

      // scan database
      ListIterator<Integer> index = relatedTransactions.listIterator();
      while (index.hasNext()) {
        boolean noUse = true;
        // boolean noNext = false;
        Record aRecord = data.get(index.next());

        for (int j = 0; j < aRecord.items.size(); ++j) {
          Pair<Integer, Integer> item = aRecord.items.get(j);
          if (item.first > maxKey) {
            break;
          }

          List<Candidate> candidates = mapCandidates[item.first];
          if (candidates != null) {
            for (Candidate c : candidates) {
              if (c.itemset[0] == item.first) {
                c.checkIndex = 0;
                c.checkRecord = aRecord;
                c.tempTUtility = item.second;
              } else if ((aRecord == c.checkRecord && c.itemset[c.checkIndex + 1] == item.first)) {
                ++c.checkIndex;
                c.tempTUtility += item.second;
                if (c.checkIndex + 1 == c.itemset.length) {
                  noUse = false;
                  c.tUtility += c.tempTUtility;

                  Quaternionic<Integer, Integer, Integer, Integer> maxFollowResults =
                      checkMaxFollow(c.itemset, aRecord.items, j + 1);
                  if (maxFollowResults.third > 0) {
                    if (((double) c.tempTUtility / (double) c.itemset.length)
                        >= maxFollowResults.first) {
                      c.gMUtility +=
                          (double) (c.tempTUtility + maxFollowResults.first)
                              / (double) (c.itemset.length + 1);
                    } else {
                      c.gMUtility +=
                          (double)
                                  (c.tempTUtility + maxFollowResults.third * maxFollowResults.first)
                              / (double) (c.itemset.length + maxFollowResults.third);
                    }
                  }
                  if (maxFollowResults.fourth > 0) {
                    if (((double) c.tempTUtility / (double) c.itemset.length)
                        >= maxFollowResults.second) {
                      c.mUtility +=
                          (double) (c.tempTUtility + maxFollowResults.second)
                              / (double) (c.itemset.length + 1);
                    } else {
                      c.mUtility +=
                          (double)
                                  (c.tempTUtility
                                      + maxFollowResults.fourth * maxFollowResults.second)
                              / (double) (c.itemset.length + maxFollowResults.fourth);
                    }
                  }
                }
              }
            }
          }
        }

        if (noUse) {
          index.remove();
        }
      }

      // check utility
      for (Candidate c : candidateItemset) {
        double aUtility = (double) c.tUtility / (double) c.itemset.length;

        if (aUtility >= minSupCount) {
          highAUtilityItemsetsCount.put(idArrayString(c.itemset), aUtility);
        } else if (preLargeCount != null && aUtility >= preLargeCount) {
          preLargeUtilityItemsetsCount.put(idArrayString(c.itemset), aUtility);
        }

        if (leadUpperbound) {
          if (c.mUtility >= thresholdCount) {
            tempMUtilityItemsets.add(c.itemset);
          }
        } else {
          if (c.gMUtility >= thresholdCount) {
            tempMUtilityItemsets.add(c.itemset);
          }
        }

        if (c.gMUtility >= thresholdCount) {
          tempCombinedItemsets.add(c.itemset);
          addReserved(tempReserved, c.itemset);
        }
      }
    }

    highMUtilityItemsets = tempMUtilityItemsets;
    combinedItemsets = tempCombinedItemsets;
    reserved = tempReserved;
    checkMemory();
  }

  private Quaternionic<Integer, Integer, Integer, Integer> checkMaxFollow(
      int[] candidateItemset, List<Pair<Integer, Integer>> itemsets, int position) {
    int max = 0;
    int maxFollow = 0;
    int countOther = 0;
    int countFollowOther = 0;

    for (int i = 0; i < itemsets.size(); ++i) {
      int name = itemsets.get(i).first;
      if (name > maxKey) {
        break;
      }

      if (Arrays.binarySearch(candidateItemset, name) < 0 && reserved.contains(name)) {
        countOther++;
        int value = itemsets.get(i).second;
        if (value > max) {
          max = value;
        }
        if (i >= position) {
          countFollowOther++;
          if (value > maxFollow) {
            maxFollow = value;
          }
        }
      }
    }
    return new Quaternionic<>(max, maxFollow, countOther, countFollowOther);
  }

  private void addReserved(Set<Integer> tempReserved, int[] itemsets) {
    for (int i : itemsets) tempReserved.add(i);
  }

  public void run() {
    if (data.size() != 0 && minSup != 0.0) {
      highAUtilityItemsetsCount = new HashMap<>();
      preLargeUtilityItemsetsCount = new HashMap<>();
      highMUtilityItemsets = new ArrayList<>();
      combinedItemsets = new ArrayList<>();

      // find large-1 itemsets
      ArrayList<Pair<Integer, Integer>> utilityInfos = new ArrayList<>(); // utility, mUtility
      utilityInfos.ensureCapacity(maxItemID + 1);
      totalCandidateCount = maxItemID + 1;
      for (int i = 0; i <= maxItemID; ++i) utilityInfos.add(new Pair<>(0, 0));

      for (Record aRecord : data) {
        List<Pair<Integer, Integer>> items = aRecord.items;

        for (Pair<Integer, Integer> item : items) {
          int name = item.first;
          int utility = item.second;
          utilityInfos.get(name).first += utility;
          utilityInfos.get(name).second += aRecord.maxUtility;
        }
      }

      reserved = new HashSet<>();
      for (int i = 1; i <= maxItemID; ++i) {
        int[] itemset = {i};
        if (utilityInfos.get(i).first >= minSupCount) {
          highAUtilityItemsetsCount.put(idArrayString(itemset), (double) utilityInfos.get(i).first);
        } else if (preLargeCount != null && utilityInfos.get(i).first >= preLargeCount) {
          preLargeUtilityItemsetsCount.put(
              idArrayString(itemset), (double) utilityInfos.get(i).first);
        }

        if (utilityInfos.get(i).second >= thresholdCount) {
          highMUtilityItemsets.add(itemset);
          combinedItemsets.add(itemset);
          reserved.add(i);
        }
      }
      while (highMUtilityItemsets.size() != 0) {
        nextLevel();
      }
    }
  }

  public Map<String, Double> getHighAUtilityItemsetsCount() {
    return highAUtilityItemsetsCount;
  }

  public Map<String, Double> getPreLargeUtilityItemsetsCount() {
    return preLargeUtilityItemsetsCount;
  }

  private static class ItemsetInfo {
    int[] itemset;
    MutableDouble aUtility;
    int tempTUtility = 0;
    int checkIndex;
    Record checkRecord;
  }

  public void reScan(
      List<Pair<String, MutableDouble>> itemsetList1,
      List<Pair<String, MutableDouble>> itemsetList2) {
    HashMap<Integer, List<ItemsetInfo>> mapInfos = new HashMap<>();
    int maxKey = 0;

    for (Pair<String, MutableDouble> itemset : itemsetList1) {
      ItemsetInfo info = new ItemsetInfo();
      info.itemset = idStringArray(itemset.first);
      info.aUtility = itemset.second;

      for (int name : info.itemset) {
        List<ItemsetInfo> infos = mapInfos.get(name);
        maxKey = Integer.max(name, maxKey);
        boolean newInfo = infos == null;
        if (newInfo) {
          infos = new ArrayList<>();
        }
        infos.add(info);
        if (newInfo) {
          mapInfos.put(name, infos);
        }
      }
    }

    for (Pair<String, MutableDouble> itemset : itemsetList2) {
      ItemsetInfo info = new ItemsetInfo();
      info.itemset = idStringArray(itemset.first);
      info.aUtility = itemset.second;

      for (int name : info.itemset) {
        List<ItemsetInfo> infos = mapInfos.get(name);
        maxKey = Integer.max(maxKey, name);
        boolean newInfo = infos == null;
        if (newInfo) {
          infos = new ArrayList<>();
        }
        infos.add(info);
        if (newInfo) {
          mapInfos.put(name, infos);
        }
      }
    }

    for (Record record : data) {
      for (Pair<Integer, Integer> item : record.items) {
        if (item.first > maxKey) {
          break;
        }

        List<ItemsetInfo> infos = mapInfos.get(item.first);
        if (infos != null) {
          for (ItemsetInfo i : infos) {
            if (i.itemset[0] == item.first) {
              i.checkIndex = 0;
              i.checkRecord = record;
              i.tempTUtility = item.second;
            } else if ((record == i.checkRecord && i.itemset[i.checkIndex + 1] == item.first)) {
              ++i.checkIndex;
              i.tempTUtility += item.second;
              if (i.checkIndex + 1 == i.itemset.length) {
                i.aUtility.setValue(
                    i.aUtility.getValue() + (double) i.tempTUtility / (double) i.itemset.length);
              }
            }
          }
        }
      }
    }
  }
}
