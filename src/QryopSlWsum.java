/**
 * This class implements the WSUM operator for Indri retrieval model.
 * 
 * @author KyleMao
 *
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QryopSlWsum extends QryopSl {

  List<Double> weights;

  /**
   * It is convenient for the constructor to accept a variable number of arguments.
   * 
   * @param weights A weight list for the query arguments.
   * @param q A query argument (a query operator).
   */
  public QryopSlWsum(Qryop... q) {
    this.weights = new ArrayList<Double>();
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  /**
   * Calculate the default score for the specified document if it does not match the query operator.
   * This score is 0 for many retrieval models, but not all retrieval models.
   * 
   * @param r A retrieval model that controls how the operator behaves.
   * @param docid The internal id of the document that needs a default score.
   * @return The default score.
   */
  @Override
  public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
    return 0.0;
  }

  /**
   * Appends an argument to the list of query operator arguments. This simplifies the design of some
   * query parsing architectures.
   * 
   * @param q The query argument (query operator) to append.
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

    if (r instanceof RetrievalModelIndri) {
      return (evaluateIndri(r));
    }

    return null;
  }

  /**
   * Evaluates the query operator for Indri retrieval model, including any child operators and
   * returns the result.
   * 
   * @param r A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  public QryResult evaluateIndri(RetrievalModel r) throws IOException {

    // Initialization
    allocArgPtrs(r);
    QryResult result = new QryResult();

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
      result += this.weights.get(i) + " " + this.args.get(i).toString() + " ";

    return ("#WSUM( " + result + ")");
  }

  /**
   * Appends a weight to the list of weights. This simplifies the design of some query parsing
   * architectures.
   * 
   * @param w The weight to append.
   * @return void
   * @throws IOException
   */
  @Override
  public void addWeight(double w) throws IOException {
    this.weights.add(w);
  }

  /**
   * Checks whether a query operator needs to read weight.
   * 
   * @return needWeight
   */
  @Override
  public boolean needWeight() {
    return (this.weights.size() <= this.args.size());
  }

  /**
   * Removes the last weight from the list of weights. This simplifies the design of some query
   * parsing architectures.
   * 
   * @return void
   * @throws IOException
   */
  public void removeWeight() throws IOException {
    this.weights.remove(this.weights.size() - 1);
  }

}
