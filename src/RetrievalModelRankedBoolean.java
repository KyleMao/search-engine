/**
 * Ranked boolean retrieval model.
 * 
 * @author KyleMao
 *
 */
public class RetrievalModelRankedBoolean extends RetrievalModel {

  /**
   * Set a retrieval model parameter.
   * 
   * @param parameterName
   * @param value
   * @return Always false because this retrieval model has no parameters.
   */
  @Override
  public boolean setParameter(String parameterName, double value) {
    System.err.println("Error: Unknown parameter name for retrieval model " + "RankedBoolean: "
        + parameterName);
    return false;
  }

  /**
   * Set a retrieval model parameter.
   * 
   * @param parameterName
   * @param value
   * @return Always false because this retrieval model has no parameters.
   */
  @Override
  public boolean setParameter(String parameterName, String value) {
    System.err.println("Error: Unknown parameter name for retrieval model " + "RankedBoolean: "
        + parameterName);
    return false;
  }
  
  /**
   *  Get a retrieval model parameter.
   *  
   *  @param parameterName The name of the parameter to set.
   *  @return value of the parameter.
   */
  @Override
  public double getParameter (String parameterName) {
    System.err.println("Error: Unknown parameter name for retrieval model " + "RankedBoolean: "
        + parameterName);
    return 0.0;
  }

}
