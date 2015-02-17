import java.io.IOException;

/**
 * This class implements the SUM operator for all retrieval models.
 * 
 * @author KyleMao
 *
 */
public class QryopSlSum extends QryopSl {

  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new
   * qryopAnd (arg1, arg2, arg3, ...).
   * 
   * @param q A query argument (a query operator).
   */
  public QryopSlSum(Qryop... q) {
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

    return ("#SUM( " + result + ")");
  }

}
