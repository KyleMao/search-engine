/**
 * Indri retrieval model.
 * 
 * @author KyleMao
 *
 */
public class RetrievalModelIndri extends RetrievalModel {

  protected double mu;
  protected double lambda;

  /**
   * Set a retrieval model parameter.
   * 
   * @param parameterName
   * @param parametervalue
   * @return Whether the parameter is successfully set.
   */
  @Override
  public boolean setParameter(String parameterName, double value) {
    if (parameterName.equals("mu")) {
      this.mu = value;
      return true;
    } else if (parameterName.equals("lambda")) {
      this.lambda = value;
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
   * @param parametervalue
   * @return Always false because this retrieval model has no String type parameters.
   */
  @Override
  public boolean setParameter(String parameterName, String value) {
    System.err.println("Error: Unknown parameter type for retrieval model " + "Indri: "
        + parameterName);
    return false;
  }

}
