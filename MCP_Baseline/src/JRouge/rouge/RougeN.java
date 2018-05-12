package JRouge.rouge;

import JRouge.interfaces.IRouge;
import JRouge.interfaces.IRougeSummaryModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import JRouge.common.ScoreType;

public class RougeN implements IRouge
{
    private IRougeSummaryModel peer;
    private Set<IRougeSummaryModel> models;
    private int byteLimit;
    private int wordLimit;
    private int n;
    private char scoreMode;
    private double alpha;
    public static boolean DEBUG = false;

    /**
     * Constructor for the RougeN metric
     * 
     * @param peer
     *            The test document
     * @param models
     *            The gold standard documents
     * @param byteLimit
     *            Limit by bytes
     * @param wordLimit
     *            Limit by words
     * @param n
     *            The n in n gram ;)
     * @param scoreMode
     *            Score mode A for average B for best
     * @param alpha
     *            Alpha for F score
     */
    public RougeN(IRougeSummaryModel peer, Set<IRougeSummaryModel> models, int byteLimit, int wordLimit, int n, char scoreMode, double alpha)
    {
	this.peer = peer;
	this.models = models;
	this.byteLimit = byteLimit;
	this.wordLimit = wordLimit;
	this.scoreMode = scoreMode;
	this.n = n;
	this.alpha = alpha;
    }

    /**
     * Creates a map of ngrams and the number of times they appeared in the given sentences
     * 
     * @param sentences
     *            The sentences from which to construct ngrams
     * @param n
     *            The gram size
     * @return A map of ngrams->count
     */
    public Map<String, Integer> createNGram(IRougeSummaryModel summary, int n)
    {
	Map<String, Integer> results = new HashMap<String, Integer>();

	int wordCount = 0;
	int byteCount = 0;

	if (results.size() == 0)
	{
	    results.put("_cn_", 0);
	}

	int count = 0;
	String tokens[] = summary.asText().replaceAll("[^\\p{L}\\p{N}]", " ").replaceAll("[ ]+", " ").split("\\s+");

	List<String> usedTokens = new ArrayList<String>();

	for (int i = 0; (i < tokens.length) && (wordCount <= wordLimit) && (byteCount <= byteLimit); i++)
	{
	    if (!tokens[i].matches("[\\p{L}\\p{N}]+"))
	    {
		continue;

	    }
	    wordCount++;
	    byteCount += stringByteSize(tokens[i]);
	    usedTokens.add(tokens[i].toLowerCase());
	}

	for (int i = 0; i < usedTokens.size() - n + 1; i++)
	{
	    String gram = usedTokens.get(i);

	    for (int j = i + 1; j < i + n; j++)
	    {
		gram += " " + usedTokens.get(j);
	    }

	    count++;

	    if (results.containsKey(gram))
	    {
		results.put(gram, results.get(gram) + 1);
	    }
	    else
	    {
		results.put(gram, 1);
	    }
	}

	results.put("_cn_", count);

	return results;
    }

    /**
     * Calculate the size in byte a string takes
     * 
     * @param s
     *            The string to work on
     * @return The amount of bytes in the string
     */
    private int stringByteSize(String s)
    {
    	return s.getBytes().length;
    }

    /**
     * Calculate the score of the ngrams in the test text and the reference text.
     * <p>
     * The score is the ratio between the hit count - how many ngrams are common to both the test text<br>
     * and the reference text - and the total number of ngrams in the reference text
     * 
     * @param model_grams
     *            The ngrams of the referenced text
     * @param peer_grams
     *            The ngrams of the test text
     * @param hit
     *            The nubmber of hits between the 2 maps
     * @param score
     *            The score calculated by the number of hits divided by the total number of ngram in the model
     */
    public void ngramScore(Map<String, Integer> model_grams, Map<String, Integer> peer_grams, MutableInt hit, MutableDouble score)
    {
	hit.setValue(0);
	Set<String> tokens = model_grams.keySet();

	for (String t : tokens)
	{
	    if (!t.equals("_cn_"))
	    {
		int h = 0;
		if (peer_grams.containsKey(t))
		{
		    h = peer_grams.get(t) <= model_grams.get(t) ? peer_grams.get(t) : model_grams.get(t);
		    hit.setValue(hit.intValue() + h);
		}
	    }
	}

	if (model_grams.get("_cn_").intValue() != 0)
	{
	    score.setValue((double) (hit.intValue()) / model_grams.get("_cn_").doubleValue());
	}
	else
	{
	    score.setValue(0);
	}
    }

    /**
     * Computes the n-gram score of the texts with which the object is initialized
     * 
     * @return A map containing the scores.<br>
     *         The map will contain the following fields: totalGramCount, totalGramHit, gramScore, totalGramCountP, gramScoreP,
     *         gramScoreF
     */
    public Map<ScoreType, Double> computeNGramScore()
    {
	Map<ScoreType, Double> results = new HashMap<ScoreType, Double>();

	MutableInt gramHit = new MutableInt(0);
	MutableDouble gramScore = new MutableDouble(0);
	// #------------------------------------------------
	// # read model file and create model n-gram maps
	int totalGramHit = 0;
	int totalGramCount = 0;
	double gramScoreBest = -1;
	double gramScoreP = 0; //# precision
	double gramScoreF = 0; //# f-measure
	int totalGramCountP = 0;

	Map<String, Integer> peer_grams = createNGram(peer, this.n);

	if (DEBUG)
	{
	    System.out.println(peer.getSourceFile());
	    System.out.println(peer.asText());
	    int i = 0;
	    System.out.print("[");
	    for (String key : peer_grams.keySet())
	    {
		System.out.print(key + ":" + peer_grams.get(key).intValue());
		if (i != peer_grams.size() - 1)
		{
		    System.out.print("|");
		}
	    }
	    System.out.println("]");
	}
	for (IRougeSummaryModel model : models)
	{
	    Map<String, Integer> model_grams = createNGram(model, this.n);
	    if (DEBUG)
	    {
		System.out.println(model.getSourceFile());
		System.out.println(model.asText());
		int i = 0;
		System.out.print("[");
		for (String key : model_grams.keySet())
		{
		    System.out.print(key + ":" + model_grams.get(key).intValue());
		    if (i != model_grams.size() - 1)
		    {
			System.out.print("|");
		    }
		}
		System.out.println("]");
	    }
	    ngramScore(model_grams, peer_grams, gramHit, gramScore);

	    switch (scoreMode)
	    {
		case 'A':
		case 'a':
		{
		    totalGramHit += gramHit.intValue();
		    totalGramCount += model_grams.get("_cn_");
		    totalGramCountP += peer_grams.get("_cn_");
		    break;
		}
		case 'B':
		case 'b':
		{
		    if (gramScore.doubleValue() > gramScoreBest)
		    {
			//# only take a better score (i.e. better match)
			gramScoreBest = gramScore.doubleValue();
			totalGramHit = gramHit.intValue();
			totalGramCount = model_grams.get("_cn_");
			totalGramCountP = peer_grams.get("_cn_");
		    }
		    break;
		}
		default:
		{
		    System.out.println("Warning: Unknown scoring mode - using average mode");
		    totalGramHit += gramHit.intValue();
		    totalGramCount += model_grams.get("_cn_");
		    totalGramCountP += peer_grams.get("_cn_");
		}
	    }
	}

	results.put(ScoreType.TOTAL_GRAM_COUNT, (double) totalGramCount); // total number of ngrams in models
	results.put(ScoreType.TOTAL_GRAM_HIT, (double) totalGramHit);
	if (totalGramCount != 0)
	{
	    gramScore.setValue((double) totalGramHit / (double) totalGramCount);
	}
	else
	{
	    gramScore.setValue(0);
	}
	results.put(ScoreType.R, gramScore.doubleValue());
	results.put(ScoreType.TOTAL_GRAM_HIT_P, (double) totalGramCountP); // total number of ngrams in peers
	if (totalGramCountP != 0)
	{
	    gramScoreP = (double) totalGramHit / (double) totalGramCountP;
	}
	else
	{
	    gramScoreP = 0;
	}
	results.put(ScoreType.P, gramScoreP); // precision score
	if (((1 - alpha) * gramScoreP + alpha * gramScoreP) > 0)
	{
	    gramScoreF = (gramScoreP * gramScore.doubleValue()) / ((1 - alpha) * gramScoreP + alpha * gramScore.doubleValue());
	}
	else
	{
	    gramScoreF = 0;
	}
	results.put(ScoreType.F, gramScoreF);

	if (DEBUG)
	{
	    System.out.println("total " + n + "-gram model count: " + totalGramCount);
	    System.out.println("total " + n + "-gram peer count: " + totalGramCountP);
	    System.out.println("total " + n + "-gram hit: " + totalGramHit);
	    System.out.println("total ROUGE-" + n + "-R: " + gramScore);
	    System.out.println("total ROUGE-" + n + "-P: " + gramScoreP);
	    System.out.println("total ROUGE-" + n + "-F: " + gramScoreF);
	}

	return results;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<ScoreType, Double> evaluate()
    {
	return computeNGramScore();
    }
}
