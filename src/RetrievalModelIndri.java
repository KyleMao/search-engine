/**
 * Indri retrieval model.
 * 
 * @author KyleMao
 *
 */
public class RetrievalModelIndri extends RetrievalModel {

  private int mu;
  private double lambda;

  /**
   * Set a retrieval model parameter.
   * 
   * @param parameterName
   * @param value
   * @return Whether the parameter is successfully set.
   */
  @Override
  public boolean setParameter(String parameterName, double value) {
    if (parameterName.equals("lambda")) {
      this.lambda = value;
      return true;
    }
    if (parameterName.equals("mu")) {
      this.mu = (int) (value);
      return true;
    } else {
      System.err.println("Error: Unknown parameter name for retrieval model " + "Indri: "
          + parameterName);
    }
    return false;
  }

  /**
   * Set a retrieval model parameter.
   * 
   * @param parameterName
   * @param value
   * @return Always false because this retrieval model has no String type parameters.
   */
  @Override
  public boolean setParameter(String parameterName, String value) {
    System.err.println("Error: Unknown parameter type for retrieval model " + "Indri: "
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
    if (parameterName.equals("lambda")) {
      return this.lambda;
    } else if (parameterName.equals("mu")) {
      return this.mu;
    }
    
    return 0.0;
  }

}
