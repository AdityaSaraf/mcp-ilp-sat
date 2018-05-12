import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

class MutableInt {
    int value;
    public MutableInt(int value) {
        this.value = value;
    }
    public void increment() {
        value++;
    }
    public int getValue() {
        return value;
    }
}

public class TFIDF {

    private Map<String, Double> wordToIDF;
    private Map<String, MutableInt> wordCounts = new HashMap<>();
    private int numDocs;
    private boolean tfInit = false;
    // Each document is a list of Strings, each String is a sentence
    public TFIDF(List<List<String>> documents) {
        Map<String, MutableInt> wordToDocCount = new HashMap<>();
        wordToIDF = new HashMap<>();
        for (List<String> document : documents) {
            HashSet<String> docWords = new HashSet<>();
            for (String sentence : document) {
                String[] words = sentence.split(" ");
                docWords.addAll(Arrays.asList(words));
            }
            // we use a set so we only add 1 to each word's doc count per doc
            for (String word : docWords) {
                increment(wordToDocCount, word);
            }
        }
        numDocs = documents.size();
        for (Map.Entry<String, MutableInt> entry : wordToDocCount.entrySet()) {
            String word = entry.getKey();
            int docCount = entry.getValue().value;
            // Smooth IDF (also add one to doc count to account for unseen words
            double idf = calculateIdf(docCount);
            wordToIDF.put(word, idf);
        }
    }

    public void initTF(List<String> document) {
        for (String sentence : document) {
            String[] words = sentence.split(" ");
            for (String word : words) {
                increment(wordCounts, word);
            }
        }
        tfInit = true;
    }

    public void clearTF() {
        wordCounts = new HashMap<>();
        System.gc();
        tfInit = false;
    }

    public double getTFIDF(String word) {
        if (!tfInit) {
            throw new IllegalStateException("Didn't initialize the TF map!");
        }
        if (!wordCounts.containsKey(word)) {
            throw new IllegalArgumentException("Word wasn't in document!");
        }
        return getTf(wordCounts.get(word).value) * getIdf(word);
    }

    private double getTf(int freq) {
        return freq;//1 + Math.log(freq);
    }

    private double calculateIdf(int docCount) {
        return Math.log(numDocs/(1+docCount));
    }

    private double getIdf(String word) {
        if (wordToIDF.containsKey(word)) {
            return wordToIDF.get(word);
        }
        return Math.log(1 + numDocs);
    }

    private <T> void increment(Map<T, MutableInt> map, T item) {
        MutableInt count = map.get(item);
        if (count == null) {
            map.put(item, new MutableInt(1));
        } else {
            count.increment();
        }
    }
}
