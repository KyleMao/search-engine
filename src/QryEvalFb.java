import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Scanner;

/**
 * This class implements the Indri relevance feedback and pseudo relevance feedback methods
 * 
 * @author KyleMao
 * 
 */

public class QryEvalFb {

  private RetrievalModel model;
  private int fbDocs;
  private int fbTerms;
  private double fbMu;
  private double fbOrigWeight;
  private String fbExpansionQueryFile;
  private boolean hasInitialRankings;
  private Map<String, List<String>> initialRankings;

  /**
   * Constructor. Reads necessary inputs. If initial ranking file is provided, reads in the ranking
   * file.
   * 
   * @param params Parameters for this query set.
   * @param model The retrieval model used.
   * @throws FileNotFoundException
   */
  public QryEvalFb(Map<String, String> params, RetrievalModel model) throws FileNotFoundException {

    if (!(model instanceof RetrievalModelIndri)) {
      throw new RuntimeException();
    }

    this.model = model;
    this.fbDocs = Integer.parseInt(params.get("fbDocs"));
    this.fbTerms = Integer.parseInt(params.get("fbTerms"));
    this.fbMu = Double.parseDouble(params.get("fbMu"));
    this.fbOrigWeight = Double.parseDouble(params.get("fbOrigWeight"));
    this.fbExpansionQueryFile = params.get("fbExpansionQeuryFile");

    if (params.containsKey("fbInitialRankingFile")) {
      this.hasInitialRankings = true;
      this.initialRankings = new HashMap<String, List<String>>();
      Scanner rankingScanner =
          new Scanner(new BufferedReader(new FileReader(params.get("fbInitialRankingFile"))));
      while (rankingScanner.hasNextLine()) {
        String line = rankingScanner.nextLine();
        String queryId = line.split(" ")[0];
        if (this.initialRankings.containsKey(queryId)) {
          if (this.initialRankings.get(queryId).size() < this.fbDocs) {
            this.initialRankings.get(queryId).add(line);
          }
        } else {
          this.initialRankings.put(queryId, new ArrayList<String>());
          this.initialRankings.get(queryId).add(line);
        }
      }
      rankingScanner.close();
    } else {
      this.hasInitialRankings = false;
    }
  }

  /**
   * Evaluate the query, and returns the result.
   * 
   * @param qTree The query tree.
   * @param queryId The String containing ID of the query.
   * @return Query result.
   * @throws Exception
   */
  public QryResult evaluate(Qryop qTree, String queryId) throws Exception {

    Map<Integer, Double> indriDocScores = new HashMap<Integer, Double>();
    if (hasInitialRankings) {
      for (String ranking : initialRankings.get(queryId)) {
        String[] parts = ranking.split(" ");
        indriDocScores.put(QryEval.getInternalDocid(parts[2]), Double.parseDouble(parts[4]));
      }
    } else {
      DocScore docScore = new DocScore(qTree.evaluate(model));
      for (int i = 0; i < fbDocs && i < docScore.scores.size(); i++) {
        indriDocScores.put(QryEval.getInternalDocid(docScore.getExternalDocid(i)),
            docScore.getDocidScore(i));
      }
    }

    String expansionQuery = expandQuery(qTree, indriDocScores);

    System.out.println(expansionQuery);

    return null;
  }

  /*
   * Expand the query using relevance feedback.
   */
  private String expandQuery(Qryop qTree, Map<Integer, Double> indriDocScores) throws IOException {

    double colLen = QryEval.READER.getSumTotalTermFreq("body");

    Map<String, Double> expansionTermWeights = new HashMap<String, Double>();

    for (Entry<Integer, Double> docScoreEntry : indriDocScores.entrySet()) {
      int docId = docScoreEntry.getKey();
      double p_I_d = docScoreEntry.getValue();
      double docLen = QryEval.dls.getDocLength("body", docId);
      TermVector termVector = new TermVector(docId, "body");

      for (int i = 1; i < termVector.stemsLength(); i++) {
        String stem = termVector.stemString(i);

        double tf = termVector.stemFreq(i);
        double ctf = termVector.totalStemFreq(i);
        double p_mle = ctf / colLen;
        double p_t_d = (tf + fbMu * p_mle) / (docLen + fbMu);
        double idf = Math.log(colLen / ctf);

        if (expansionTermWeights.containsKey(stem)) {
          expansionTermWeights.put(stem, expansionTermWeights.get(stem) + p_t_d * p_I_d * idf);
        } else {
          expansionTermWeights.put(stem, p_t_d * p_I_d * idf);
        }
      }
    }

    PriorityQueue<Entry<String, Double>> termHeap =
        new PriorityQueue<Map.Entry<String, Double>>(expansionTermWeights.size(),
            new Comparator<Entry<String, Double>>() {
              @Override
              public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
              }
            });

    for (Entry<String, Double> termWeightEntry : expansionTermWeights.entrySet()) {
      termHeap.add(termWeightEntry);
    }

    StringBuffer queryBuffer = new StringBuffer();
    for (int i = 0; i < fbTerms; i++) {
      Entry<String, Double> termHeapEntry = termHeap.remove();
      queryBuffer.append(termHeapEntry.getValue() + " " + termHeapEntry.getKey() + " ");
    }

    return "#wand (" + queryBuffer.toString() + ')';
  }

}
