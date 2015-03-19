/**
 * This class implements the WINDOW operator for all retrieval models.
 * 
 * @author KyleMao
 *
 */

import java.io.IOException;

public class QryopIlWindow extends QryopIl {

  // Max term distance of two candidate terms
  private int distance;

  /**
   * Constructor. Create a WINDOW operator with a specific distance.
   * 
   * @param distance Max term distance of two candidate terms
   */
  public QryopIlWindow(int distance) {
    this.distance = distance;
  }

  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new
   * qryopWindow (distance, arg1, arg2, arg3, ...).
   * 
   * @param distance Max term distance of two candidate terms
   * @param q A query argument (a query operator).
   */
  public QryopIlWindow(int distance, Qryop... q) {
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

    return ("#WINDOW/" + distance + "( " + result + ")");
  }

}
