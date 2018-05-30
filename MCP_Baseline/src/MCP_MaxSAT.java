
import net.sf.javailp.Linear;

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

    public List<Set<String>> sentences;
    public int maxSentences;
    public Map<String, Double> wordWeights;
    private Map<String, Set<Integer>> wordToSentIdx = new HashMap<>();
    private Set<String> vocabulary;
    private Map<Integer, String> dictionary;
    private Map<String, Integer> reverseDictionary;

    public MCP_MaxSAT(List<String> sentenceStrings, int maxSentences, TFIDF tfidf) {
        this.maxSentences = maxSentences;
        wordWeights = new HashMap<>();

        sentences = new ArrayList<>(sentenceStrings.size());
        vocabulary = new HashSet<>();
        for (int i = 0; i < sentenceStrings.size(); i++) {
            String sentenceStr = sentenceStrings.get(i);
            if (sentenceStr.equals("")) {
                continue;
            }
            String[] words = sentenceStr.split(" ");
            Set<String> sentenceSet = new HashSet<>(Arrays.asList(words));
            sentences.add(sentenceSet);
            for (String word : sentenceSet) {
                Set<Integer> set;
                if ((set = wordToSentIdx.get(word)) == null) {
                    set = new HashSet<>();
                    set.add(i);
                    wordToSentIdx.put(word, set);
                } else {
                    set.add(i);
                }
            }
            vocabulary.addAll(sentenceSet);
        }
        if (tfidf != null) {
            for (String word : vocabulary) {
                wordWeights.put(word, tfidf.getTFIDF(word));
            }
        }
        dictionary = new HashMap<>();
        reverseDictionary = new HashMap<>();
        int c = 0;
        for (String word : vocabulary) {
            dictionary.put(c, word);
            reverseDictionary.put(word, c);
            c++;
        }
    }

    // This function converts the MCP to a partial max-SAT encoding
    public String toSAT() {
        StringBuilder result = new StringBuilder();

        // These variables represent whether each sentence is in the set
        List<String> sentenceVars = new ArrayList();
        for (int i = 0; i < sentences.size(); i++) {
            String varName = String.format("x_%d", i);
            sentenceVars.add(varName);
            result.append(String.format("(declare-const %s Bool)\n", varName));
        }

        // This part represents the constraint that we must choose a limited number of sentences
//        result.append(additionCircuit_Unary(sentenceVars, maxSentences));
        result.append(additionZ3(sentenceVars, maxSentences));

        // This part represents the goal of maximizing the number of covered words
        for (String word : vocabulary) {
            result.append(String.format("(assert-soft %s :weight %d)\n",
                    wordCoveredCircuit(sentenceVars, word), 1));
        }

//        // These variables represent whether the a word is chosen
//        for (int i = 0; i < vocabulary.size(); i++) {
//            // this var corresponds to the word: dictionary.get(i)
//            String varName = String.format("y_%d", i);
//            result.append(String.format("(declare-const %s Bool)\n", varName));
//        }
//
//        // This represents the constraints that chosen words must come from some chosen sentence and
//        // assigns weights to the word vars
//        for (String word : vocabulary) {
//            String varName = String.format("y_%d", reverseDictionary.get(word));
//            result.append(String.format("(assert-soft %s :weight %d)\n",
//                                            varName, 1));
//            String wordCircuit = wordCoveredCircuit(sentenceVars, word, varName);
//            result.append(String.format("(assert %s)\n", wordCircuit));
//        }

        // Get the results from the solver
        result.append("(check-sat)\n");
        for (String varName : sentenceVars) {
            result.append(String.format("(eval %s)\n", varName));
        }

        return result.toString();
    }

    // This function returns a formula that represents whether a word is covered by the chosen sentences -- w/o wordVars
    public String wordCoveredCircuit(List<String> vars, String word) {
        Set<Integer> sentences = wordToSentIdx.get(word);
        if (sentences.size() == 1) {
            return vars.get(sentences.iterator().next());
        }
        StringBuilder formula = new StringBuilder("(or");
        for (int i : sentences) {
            formula.append(" ").append(vars.get(i));
        }
        return formula + ")";
    }

    // This function returns a formula that represents whether a word is covered by the chosen sentences -- with wordVars
        public String wordCoveredCircuit(List<String> vars, String word, String wordVarName) {
        Set<Integer> sentences = wordToSentIdx.get(word);
        StringBuilder formula = new StringBuilder(String.format("(or (not %s)", wordVarName));

        for (int i : sentences) {
            formula.append(" ").append(vars.get(i));
        }
        return formula + ")";
    }

    // This function implements addition by converts the bools to integers
    public String additionZ3(List<String> vars, int k) {
        StringBuilder result = new StringBuilder();
        result.append("(define-fun b2i ((b Bool)) Int\n" + "  (ite b 1 0)\n" + ")\n");
        StringBuilder sum = new StringBuilder("(+");
        for (String var : vars) {
            sum.append(" (b2i ").append(var).append(" )");
        }
        sum.append(")");
        result.append(String.format("(assert (<= %s %d))", sum.toString(), k));
        return result.toString()+"\n";
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
