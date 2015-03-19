/**
 * This class implements the WINDOW operator for all retrieval models.
 * 
 * @author KyleMao
 *
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

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

  /**
   * Evaluates the query operator, including any child operators and returns the result.
   * 
   * @param r A retrieval model that controls how the operator behaves
   * @return The result of evaluating the query
   * @throws IOException
   */
  @Override
  public QryResult evaluate(RetrievalModel r) throws IOException {

    // Initialization
    allocArgPtrs(r);
    QryResult result = new QryResult();

    // WINDOW is should be based on AND. Exact-match AND requires that ALL invLists contain a
    // document id. Use the first list to control the search for matches.
    ArgPtr ptr0 = this.argPtrs.get(0);

    EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.invList.df; ptr0.nextDoc++) {
      int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);

      // Do the other query arguments have the ptr0Docid?
      for (int j = 1; j < this.argPtrs.size(); j++) {

        ArgPtr ptrj = this.argPtrs.get(j);

        while (true) {
          if (ptrj.nextDoc >= ptrj.invList.df)
            break EVALUATEDOCUMENTS; // No more docs can match.
          else if (ptrj.invList.getDocid(ptrj.nextDoc) > ptr0Docid)
            continue EVALUATEDOCUMENTS; // The ptr0docid can't match.
          else if (ptrj.invList.getDocid(ptrj.nextDoc) < ptr0Docid)
            ptrj.nextDoc++; // Not yet at the right doc.
          else
            break; // Same document found.
        }
      }

      // Already satisfies AND condition, check for WINDOW condition
      List<Integer> locations = new ArrayList<Integer>();
      List<Integer> allPos = new ArrayList<Integer>(Collections.nCopies(this.args.size(), 0));
      EVALUATELOCATIONS: while (true) {

        // Check whether any of the position lists is exhausted
        for (int j = 0; j < allPos.size(); j++) {
          ArgPtr ptrj = this.argPtrs.get(j);
          if (allPos.get(j) >= getPositions(ptrj).size()) {
            break EVALUATELOCATIONS;
          }
        }

        int lowerBound = getPositions(ptr0).get(allPos.get(0));
        int upperBound = lowerBound;
        for (int j = 0; j < allPos.size(); j++) {

          ArgPtr ptrj = this.argPtrs.get(j);
          int jPos = getPositions(ptrj).get(allPos.get(j));

          if (j == 0) {
            lowerBound = jPos;
            upperBound = jPos;
          } else {
            if (jPos < upperBound - distance) { // Current position too small.
              incListElem(allPos, j);
              if (allPos.get(j) >= getPositions(ptrj).size()) {
                break EVALUATELOCATIONS;
              }
              j--; // Backtrack
            } else if (jPos > lowerBound + distance) { // Current position too large.
              incListElem(allPos, 0);
              if (allPos.get(0) >= getPositions(ptr0).size()) {
                break EVALUATELOCATIONS;
              }
              continue EVALUATELOCATIONS;
            } else { // Good so far.
              if (jPos < lowerBound) {
                lowerBound = jPos;
              } else if (jPos > upperBound) {
                upperBound = jPos;
              }
            }
          }
        }

        // Add the location of the last term to locations
        ArgPtr ptr = this.argPtrs.get(allPos.size() - 1);
        locations.add(getPositions(ptr).get(allPos.get(allPos.size() - 1)));
        for (int i = 0; i < allPos.size(); i++) {
          incListElem(allPos, i);
        }
      }

      if (!locations.isEmpty()) {
        result.invertedList.appendPosting(ptr0Docid, locations);
      }
    }
    result.invertedList.field = ptr0.invList.field;

    freeArgPtrs();

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

    return ("#WINDOW/" + distance + "( " + result + ")");
  }

  /*
   * Increment an element in a list of integers by one.
   * 
   * @param list The list to be dealt with
   * 
   * @param idx The index of the integer to be incremented
   */
  private void incListElem(List<Integer> list, int idx) {
    int tmp = list.get(idx);
    list.set(idx, tmp + 1);
  }

  /*
   * Get the positions in the document currently under evaluation in the ArgPtr.
   * 
   * @param argPtr
   * 
   * @return The positions in the document currently under evaluation
   */
  private Vector<Integer> getPositions(ArgPtr argPtr) {
    return argPtr.invList.postings.get(argPtr.nextDoc).positions;
  }

}
