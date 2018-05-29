
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MCP_MaxSAT {

    public List<Collection<String>> sentences;
    public int maxSentences;
    public Map<String, Double> wordWeights;

    public MCP_MaxSAT(List<String> sentenceStrings, int maxSentences, TFIDF tfidf) {
        sentences = sentenceStrings.stream()
                .map(s -> Arrays.asList(s.split(" "))).collect(Collectors.toList());
        this.maxSentences = maxSentences;
        wordWeights = new HashMap();
        for (String word : getAllWords()) {
            wordWeights.put(word, tfidf.getTFIDF(word));
        }
    }

    public Set<String> getAllWords() {
        Set<String> allWords = new HashSet();
        sentences.forEach(allWords::addAll);
        return allWords;
    }

    public List<Integer> sentencesContaining(String word) {
        List<Integer> result = new ArrayList();
        for (int i = 0; i < sentences.size(); i++) {
            if (sentences.get(i).contains(word)) {
                result.add(i);
            }
        }
        return result;
    }

    // This function converts the MCP to a partial max-SAT encoding
    public String toSAT() {
        StringBuilder result = new StringBuilder();

        // These variables represent whether each sentence is in the set
        List<String> vars = new ArrayList();
        for (int i = 0; i < sentences.size(); i++) {
            String varName = String.format("x_%d", i);
            vars.add(varName);
            result.append(String.format("(declare-const %s Bool)\n", varName));
        }

        // This part represents the constraint that we must choose a limited number of sentences
        result.append(additionCircuit_Unary(vars, maxSentences));

        // This part represents the goal of maximizing the number of covered words
        for (String word : getAllWords()) {
            if (wordWeights.get(word) > 0) {
                result.append(String.format("(assert-soft %s :weight %.2f)\n",
                        wordCoveredCircuit(vars, word), wordWeights.get(word)));
            }
        }

        // Get the results from the solver
        result.append("(check-sat)\n");
        for (String varName : vars) {
            result.append(String.format("(eval %s)\n", varName));
        }

        return result.toString();
    }

    // This function returns a formula that represents whether a word is covered by the chosen sentences
    public String wordCoveredCircuit(List<String> vars, String word) {
        List<Integer> l = sentencesContaining(word);
        if (l.size() == 1) {
            return vars.get(l.get(0));
        }
        String formula = "(or";
        for (int i : sentencesContaining(word)) {
            formula += " " + vars.get(i);
        }
        return formula + ")";
    }

    // This function implements the sequential counter described here:
    // http://www.carstensinz.de/papers/CP-2005.pdf
    public String additionCircuit_Unary(List<String> vars, int k) {
        StringBuilder result = new StringBuilder();
        for (int i = 1; i <= vars.size(); i++) {
            for (int j = 1; j <= k; j++) {
                result.append(String.format("(declare-const s_%d_%d Bool)\n", i, j));
            }
        }
        result.append(String.format("(assert (or (not %s) s_1_1))\n", vars.get(0)));
        for (int j = 2; j <= k; j++) {
            result.append(String.format("(assert (not s_1_%d))\n", j));
        }
        for (int i = 2; i < vars.size(); i++) {
            result.append(String.format("(assert (or (not %s) s_%d_1))\n", vars.get(i - 1), i));
            result.append(String.format("(assert (or (not s_%d_1) s_%d_1))\n", i - 1, i));
            for (int j = 2; j <= k; j++) {
                result.append(String.format("(assert (or (not %s) (not s_%d_%d) s_%d_%d))\n", vars.get(i - 1), i - 1, j - 1, i, j));
                result.append(String.format("(assert (or (not s_%d_%d) s_%d_%d))\n", i - 1, j, i, j));
            }
            result.append(String.format("(assert (or (not %s) (not s_%d_%d)))\n", vars.get(i - 1), i - 1, k));
        }
        result.append(String.format("(assert (or (not %s) (not s_%d_%d)))\n", vars.get(vars.size() - 1), vars.size() - 1, k));
        return result.toString();
    }
}
