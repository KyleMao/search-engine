/**
 * This class implements the WSUM operator for Indri retrieval model.
 * 
 * @author KyleMao
 *
 */

import java.io.IOException;
import java.util.List;

public class QryopSlWsum extends QryopSl {
  
  List<Double> weights;
  
  /**
   * It is convenient for the constructor to accept a variable number of arguments.
   * 
   * @param weights A weight list for the query arguments.
   * @param q A query argument (a query operator).
   */
  public QryopSlWsum(List<Double> weights, Qryop... q) {
    this.weights = weights;
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

  /*
   * (non-Javadoc)
   * 
   * @see Qryop#evaluate(RetrievalModel)
   */
  @Override
  public QryResult evaluate(RetrievalModel r) throws IOException {
    // TODO Auto-generated method stub
    return null;
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

}
