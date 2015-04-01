/**
 * This class implements the Indri relevance feedback and pseudo relevance feedback methods
 * 
 * @author KyleMao
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Scanner;

public class QryEvalFb {

  private RetrievalModel model;
  private int fbDocs;
  private int fbTerms;
  private int fbMu;
  private double fbOrigWeight;
  private BufferedWriter fbExpansionQueryWriter;
  private boolean hasInitialRankings;
  private Map<String, List<String>> initialRankings;

  /**
   * Constructor. Reads necessary inputs. If initial ranking file is provided, reads in the ranking
   * file.
   * 
   * @param params Parameters for this query set.
   * @param model The retrieval model used.
   * @throws IOException
   */
  public QryEvalFb(Map<String, String> params, RetrievalModel model) throws IOException {

    // Check if the retrieval model is Indri, if not, throw exception
    if (!(model instanceof RetrievalModelIndri)) {
      throw new RuntimeException();
    }

    // Read the needed parameters
    this.model = model;
    this.fbDocs = Integer.parseInt(params.get("fbDocs"));
    this.fbTerms = Integer.parseInt(params.get("fbTerms"));
    this.fbMu = Integer.parseInt(params.get("fbMu"));
    this.fbOrigWeight = Double.parseDouble(params.get("fbOrigWeight"));
    if (params.containsKey("fbExpansionQueryFile")) {
      this.fbExpansionQueryWriter =
          new BufferedWriter(new FileWriter(new File(params.get("fbExpansionQueryFile"))));
    } else {
      this.fbExpansionQueryWriter = null;
    }

    // Read the initial ranking file if available
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
   * Clean up the QryEvalFb object.
   * 
   * @throws IOException
   */
  public void finish() throws IOException {

    if (fbExpansionQueryWriter != null) {
      fbExpansionQueryWriter.close();
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
  public QryResult evaluate(Qryop qTree, String queryId, String query) throws Exception {

    // A map from internal document ID to its initial Indri score
    Map<Integer, Double> indriDocScores = new HashMap<Integer, Double>();
    if (hasInitialRankings) {
      // Get the scores from the initial ranking file
      for (String ranking : initialRankings.get(queryId)) {
        String[] parts = ranking.split(" ");
        indriDocScores.put(QryEval.getInternalDocid(parts[2]), Double.parseDouble(parts[4]));
      }
    } else {
      // Use the initial query to retrieve documents and then get the scores
      DocScore docScore = new DocScore(qTree.evaluate(model));
      for (int i = 0; i < fbDocs && i < docScore.scores.size(); i++) {
        indriDocScores.put(QryEval.getInternalDocid(docScore.getExternalDocid(i)),
            docScore.getDocidScore(i));
      }
    }

    // Perform query expansion
    String expansionQuery = expandQuery(qTree, indriDocScores);

    // Write the expansion query to a file if needed
    if (fbExpansionQueryWriter != null) {
      fbExpansionQueryWriter.write(queryId + ": " + expansionQuery + '\n');
    }

    // Create a combined query and use the combined query to retrieve documents
    String combinedQuery =
        "#WAND(" + fbOrigWeight + " #AND(" + query + ") " + (1 - fbOrigWeight) + " "
            + expansionQuery + ")";
    Qryop expandedQTree = QryEval.parseQuery(combinedQuery, model);
    QryResult result = expandedQTree.evaluate(model);

    return result;
  }

  /*
   * Expand the query using relevance feedback.
   */
  private String expandQuery(Qryop qTree, Map<Integer, Double> indriDocScores) throws IOException {

    double colLen = QryEval.READER.getSumTotalTermFreq("body");

    // A map from expansion terms to their weigthts
    Map<String, Double> expansionTermWeights = new HashMap<String, Double>();
    // A map from expansion terms to ther ctf's
    Map<String, Long> ctfMap = new HashMap<String, Long>();
    // A map from expansion terms to their completed file counts
    Map<String, Integer> termFileCount = new HashMap<String, Integer>();

    // First pass through the terms to gather all terms
    for (Entry<Integer, Double> docScoreEntry : indriDocScores.entrySet()) {
      int docId = docScoreEntry.getKey();
      TermVector termVector = new TermVector(docId, "body");

      for (int i = 1; i < termVector.stemsLength(); i++) {
        String stem = termVector.stemString(i);
        if (!termFileCount.containsKey(stem)) {
          termFileCount.put(stem, 0);
          expansionTermWeights.put(stem, 0.0);
          ctfMap.put(stem, termVector.totalStemFreq(i));
        }
      }
    }

    // Second pass through all terms, calculate the term weigts
    int docNum = 0;
    for (Entry<Integer, Double> docScoreEntry : indriDocScores.entrySet()) {
      docNum++;
      int docId = docScoreEntry.getKey();
      double p_I_d = docScoreEntry.getValue();
      double docLen = QryEval.dls.getDocLength("body", docId);
      TermVector termVector = new TermVector(docId, "body");

      // If term is in a document, add its score to the weights
      for (int i = 1; i < termVector.stemsLength(); i++) {
        String stem = termVector.stemString(i);

        double tf = termVector.stemFreq(i);
        double ctf = ctfMap.get(stem);
        double p_mle = ctf / colLen;
        double p_t_d = (tf + fbMu * p_mle) / (docLen + fbMu);
        double idf = Math.log(colLen / ctf);

        expansionTermWeights.put(stem, expansionTermWeights.get(stem) + p_t_d * p_I_d * idf);
        termFileCount.put(stem, termFileCount.get(stem) + 1);
      }

      // If term is not in this document, add its default score to the weights
      for (Entry<String, Double> termWeightEntry : expansionTermWeights.entrySet()) {
        String stem = termWeightEntry.getKey();
        if (termFileCount.get(stem) < docNum) {
          double ctf = ctfMap.get(stem);
          double p_mle = ctf / colLen;
          double p_t_d = ((double) fbMu * p_mle) / (docLen + fbMu);
          double idf = Math.log(colLen / ctf);

          expansionTermWeights.put(stem, expansionTermWeights.get(stem) + p_t_d * p_I_d * idf);
          termFileCount.put(stem, termFileCount.get(stem) + 1);
        }
      }
    }

    // Use a max heap to store the scores and get the top fbTerms terms
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

    // Build the expansion query string
    StringBuffer queryBuffer = new StringBuffer();
    for (int i = 0; i < fbTerms; i++) {
      Entry<String, Double> termHeapEntry = termHeap.remove();
      queryBuffer.append(termHeapEntry.getValue() + " " + termHeapEntry.getKey() + " ");
    }

    return "#WAND( " + queryBuffer.toString() + ')';
  }

}
