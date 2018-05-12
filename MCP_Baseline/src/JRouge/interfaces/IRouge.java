package JRouge.interfaces;

import java.util.Map;

import JRouge.common.ScoreType;

/**
 * This interface represents the minimal contract a class should obey in order to be an evaluator
 * 
 * @author nocgod
 */
public interface IRouge
{
    /**
     * Method runs the evaluation and returns the resulting score
     * 
     * @return The score
     */
    Map<ScoreType, Double> evaluate();
}
