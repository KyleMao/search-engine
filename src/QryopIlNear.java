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

    // Initialization
    allocArgPtrs(r);
    QryResult result = new QryResult();

    // NEAR is should be based on AND. Exact-match AND requires that ALL scoreLists contain a
    // document id. Use the first list to control the search for matches.
    ArgPtr ptr0 = this.argPtrs.get(0);

    EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc++) {

      int ptr0Docid = ptr0.scoreList.getDocid(ptr0.nextDoc);
      double docScore = 0.0;

      // Do the other query arguments have the ptr0Docid?
      for (int j = 1; j < this.argPtrs.size(); j++) {

        ArgPtr ptrj = this.argPtrs.get(j);

        while (true) {
          if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
            break EVALUATEDOCUMENTS; // No more docs can match
          else if (ptrj.scoreList.getDocid(ptrj.nextDoc) > ptr0Docid)
            continue EVALUATEDOCUMENTS; // The ptr0docid can't match.
          else if (ptrj.scoreList.getDocid(ptrj.nextDoc) < ptr0Docid)
            ptrj.nextDoc++; // Not yet at the right doc.
          else {
            // ptrj matches ptr0Docid, use the term vector of ptr0Docid to test for NEAR conditions
            //TermVector tv = new TermVector(ptr0Docid, ptr0.scoreList.scores.get(ptr0.nextDoc).)
            break;
          }
        }
      }

      // The ptr0Docid matched all query arguments, so save it.
      result.docScores.add(ptr0Docid, docScore);
    }

    return result;
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

    return ("#NEAR/" + distance + "( " + result + ")");
  }

}
