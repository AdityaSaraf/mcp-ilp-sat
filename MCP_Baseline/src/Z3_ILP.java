
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Z3_ILP {

    private final List<Set<String>> sentences;
    private final int maxSentences;
    private final TFIDF tfidf;
    private final List<String> allWords;

    public Z3_ILP(List<String> sentenceStrings, int maxSentences, TFIDF tfidf) {
        this.maxSentences = maxSentences;
        this.tfidf = tfidf;

        sentences = new ArrayList();
        Set<String> allWordsSet = new HashSet();

        for (String s : sentenceStrings) {
            String[] words = s.split(" ");
            Set<String> wordSet = new HashSet(Arrays.asList(words));
            sentences.add(wordSet);
            allWordsSet.addAll(wordSet);
        }

        allWords = new ArrayList(allWordsSet);
    }

    // This function converts the MCP to a partial max-SAT encoding
    public String toILP() {
        StringBuilder result = new StringBuilder();

        // These variables represent whether each sentence is in the set
        List<String> sentenceVars = new ArrayList();
        for (int i = 0; i < sentences.size(); i++) {
            String varName = String.format("s_%d", i);
            sentenceVars.add(varName);
            result.append(String.format("(declare-const %s Int)\n", varName));
            result.append(String.format("(assert (>= %s 0))\n", varName));
            result.append(String.format("(assert (<= %s 1))\n", varName));
        }

        // These variables represent whether each word is covered
        List<String> wordVars = new ArrayList();
        for (int i = 0; i < allWords.size(); i++) {
            String varName = String.format("w_%d", i);
            wordVars.add(varName);
            result.append(String.format("(declare-const %s Int)\n", varName));
            result.append(String.format("(assert (>= %s 0))\n", varName));
            result.append(String.format("(assert (<= %s 1))\n", varName));
        }

        // This part represents the constraint that each chosen word must be covered by a chosen sentence
        for (int i = 0; i < allWords.size(); i++) {
            String word = allWords.get(i);
            List<String> containingSentences = IntStream.range(0, sentences.size())
                    .filter(j -> sentences.get(j).contains(word))
                    .mapToObj(sentenceVars::get)
                    .collect(Collectors.toList());
            result.append(String.format("(assert (<= %s (+", wordVars.get(i)));
            for (String s : containingSentences) {
                result.append(" ").append(s);
            }
            result.append(")))\n");
        }

        // This part represents the constraint that we must choose a limited number of sentencesresult.append("(assert (<= (+");
        result.append("(assert (<= (+");
        for (String varName : sentenceVars) {
            result.append(" ").append(varName);
        }
        result.append(String.format(") %d))\n", maxSentences));

        // This part represents the goal of maximizing the number of covered words
        result.append("(maximize (+");
        for (int i = 0; i < allWords.size(); i++) {
            if (tfidf == null) {
                result.append(String.format(" %s", wordVars.get(i)));

            } else {
                result.append(String.format(" (* %f %s)", tfidf.getTFIDF(allWords.get(i)), wordVars.get(i)));
            }
        }
        result.append("))\n");

        // Get the results from the solver
        result.append("(check-sat)\n");
        for (String varName : sentenceVars) {
            result.append(String.format("(eval %s)\n", varName));
        }

        return result.toString();
    }
}
