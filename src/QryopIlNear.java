/**
 * This class implements the AND operator for all retrieval models.
 * 
 * @author KyleMao
 *
 */

import java.io.IOException;

public class QryopIlNear extends QryopIl {

  private int distance;

  /**
   * Constructor. Create a NEAR operator with a specific distance.
   * 
   * @param distance
   */
  public QryopIlNear(int distance) {
    this.distance = distance;
  }
  
  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new
   * qryopNear (distance, arg1, arg2, arg3, ...).
   * 
   * @param q A query argument (a query operator).
   */
  public QryopIlNear(int distance, Qryop... q) {
    this.distance = distance;
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
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
    return null;
  }

  /*
   * Return a string version of this query operator.
   * 
   * @return The string version of this query operator.
   */
  @Override
  public String toString() {

    String result = new String();

    for (int i = 0; i < this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#NEAR/" + distance +"( " + result + ")");
  }

}
