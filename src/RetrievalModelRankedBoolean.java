/**
 * Ranked boolean retrieval model.
 * 
 * @author KyleMao
 *
 */
public class RetrievalModelRankedBoolean extends RetrievalModel {

  /*
   * (non-Javadoc)
   * 
   * @see RetrievalModel#setParameter(java.lang.String, double)
   */
  @Override
  public boolean setParameter(String parameterName, double value) {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see RetrievalModel#setParameter(java.lang.String, java.lang.String)
   */
  @Override
  public boolean setParameter(String parameterName, String value) {
    return false;
  }

}
