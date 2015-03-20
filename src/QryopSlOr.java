/**
 * This class implements the OR operator for all retrieval models.
 * 
 * @author KyleMao
 *
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QryopSlOr extends QryopSl {

  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new qryopOr
   * (arg1, arg2, arg3, ...).
   * 
   * @param q A query argument (a query operator).
   */
  public QryopSlOr(Qryop... q) {
    for (Qryop qryop : q) {
      this.args.add(qryop);
    }
  }

  /**
   * Calculate the default score for the specified document if it does not match the query operator.
   * 
   * @param r A retrieval model that controls how the operator behaves.
   * @param docid The internal id of the document that needs a default score.
   * @return The default score.
   */
  @Override
  public double getDefaultScore(RetrievalModel r, long docid) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean)
      return (0.0);

    return 0.0;
  }

  /**
   * Appends an argument to the list of query operator arguments. This simplifies the design of some
   * query parsing architectures.
   * 
   * @param {q} q The query argument (query operator) to append
   * @return void
   * @throws IOException
   */
  @Override
  public void add(Qryop q) throws IOException {
    this.args.add(q);
  }

  /**
   * Evaluates the query operator, including any child operators and returns the result.
   * 
   * @param r A retrieval model that controls how the operator behaves
   * @return The result of evaluating the query
   * @throws IOException
   */
  @Override
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean)
      return (evaluateBoolean(r));

    return null;
  }

  /**
   * Evaluates the query operator for boolean retrieval models, including any child operators and
   * returns the result.
   * 
   * @param r A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

    // Initialization
    allocArgPtrs(r);
    QryResult result = new QryResult();

    // Put all the scores into the docScores map
    Map<Integer, Double> docScores = new HashMap<Integer, Double>();
    for (ArgPtr argPtr : argPtrs) {
      for (; argPtr.nextDoc < argPtr.scoreList.scores.size(); argPtr.nextDoc++) {
        if (r instanceof RetrievalModelUnrankedBoolean) {
          // For unranked retrieval model, add a score only if it has not appeared yet.
          if (!docScores.containsKey(argPtr.scoreList.getDocid(argPtr.nextDoc))) {
            docScores.put(argPtr.scoreList.getDocid(argPtr.nextDoc), 1.0);
          }
        } else {
          // For ranked retrieval model, add a score if it has not appeared, or update a score if
          // got a higher one.
          if (!docScores.containsKey(argPtr.scoreList.getDocid(argPtr.nextDoc))) {
            docScores.put(argPtr.scoreList.getDocid(argPtr.nextDoc),
                argPtr.scoreList.getDocidScore(argPtr.nextDoc));
          } else {
            double newScore = argPtr.scoreList.getDocidScore(argPtr.nextDoc);
            if (newScore > docScores.get(argPtr.scoreList.getDocid(argPtr.nextDoc))) {
              docScores.put(argPtr.scoreList.getDocid(argPtr.nextDoc), newScore);
            }
          }
        }
      }
    }

    // Add scores to result
    for (Map.Entry<Integer, Double> entry : docScores.entrySet()) {
      result.docScores.add(entry.getKey(), entry.getValue());
    }

    freeArgPtrs();

    return result;
  }

  /**
   * Return a string version of this query operator.
   * 
   * @return The string version of this query operator.
   */
  @Override
  public String toString() {
    String result = new String();

    for (int i = 0; i < this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#OR( " + result + ")");
  }

  @Override
  public void addWeight(double w) throws IOException {
  }

  @Override
  public boolean needWeight() {
    return false;
  }

  @Override
  public void removeWeight() throws IOException {
  }

}
