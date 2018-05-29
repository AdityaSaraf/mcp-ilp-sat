
import JRouge.common.RougeSummaryModel;
import JRouge.common.ScoreType;
import JRouge.interfaces.IRougeSummaryModel;
import JRouge.rouge.RougeN;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

class LabelledDocument {

    public String fileName;
    public List<String> originalSentences;
    public List<Integer> answerNums;
    public List<String> filteredSentences;
    public List<String> abstractiveSentences;
    public Set<String> vocabulary;
    public int lengthSummary;

    public LabelledDocument() {
    }

    public LabelledDocument(String fileName, List<String> originalSentences, List<Integer> answerNums, List<String> filteredSentences,
            List<String> abstractiveSentences, Set<String> vocabulary, int lengthSummary) {
        this.fileName = fileName;
        this.originalSentences = originalSentences;
        this.answerNums = answerNums;
        this.filteredSentences = filteredSentences;
        this.abstractiveSentences = abstractiveSentences;
        this.vocabulary = vocabulary;
        this.lengthSummary = lengthSummary;
    }
}

public class Main {

    static List<String> stopWords = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        //File directory = new File("data/law_stories_top_100");
        File directory = new File("data/CNN_DailyMail_1k");
        File[] files = directory.listFiles();

        File stopWordFile = new File("stopwords.txt");
        populateStopWordList(stopWordFile);

        File outputDir = new File("error_analysis/mcp10");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        List<LabelledDocument> documents = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                documents.add(parseFile(file));
            }
        }
        List<List<String>> documentSentences = new ArrayList<>();
        for (LabelledDocument document : documents) {
            documentSentences.add(document.filteredSentences);
        }

        TFIDF tfidf = new TFIDF(documentSentences);
        MCPSolver mcpSolver = new MCPSolver(tfidf);
        // warm up the JVM
        for (int i = 0; i < 3; i++) {
            long time = run(documents, mcpSolver);
            System.out.println("WARMUP: Time elapsed for iteration " + i + " in ms: " + time);
        }
        // actual trials
        long total = 0;
        for (int i = 0; i < 10; i++) {
            long time = run(documents, mcpSolver);
            System.out.println("Time elapsed for iteration " + i + " in ms: " + time);
            total += time;
        }
        System.out.println("Average time: " + (total / 10));
    }

    private static long run(List<LabelledDocument> documents, MCPSolver mcpSolver) {
        long startTime = System.currentTimeMillis();
        double total = 0;
        for (int i1 = 0; i1 < documents.size(); i1++) {
            LabelledDocument document = documents.get(i1);
//            System.out.println(document.fileName);
            if (i1 % 20 == 0) {
                System.out.println("Finished with " + i1);
                System.out.println("Time elapsed: " + (System.currentTimeMillis() - startTime));
                System.gc();
            }
            //            Set<Integer> generatedSummary = mcpSolver.simpleGreedy(document.filteredSentences, document.lengthSummary);
            //Set<Integer> generatedSummary = mcpSolver.unweightedILP(document.filteredSentences, document.lengthSummary);
            //            Set<Integer> generatedSummary = mcpSolver.weightedILP(document.filteredSentences, 3);
            Set<Integer> generatedSummary = mcpSolver.weightedMaxSAT(document.filteredSentences, document.lengthSummary);

            List<String> systemSummary = new ArrayList<>();
            List<String> gsSummary = new ArrayList<>();
            for (Integer i : generatedSummary) {
                systemSummary.add(document.originalSentences.get(i));
            }
            for (int i : document.answerNums) {
                gsSummary.add(document.originalSentences.get(i));
            }
            double rougeScore = test(systemSummary, gsSummary);
            total += rougeScore;
            //                        System.out.println(rougeScore);
            //            //             prints the generated summary
            //            File outputFile = new File(outputDir, document.fileName);
            //            if (outputFile.exists()) {
            //                outputFile.delete();
            //            }
            //            outputFile.createNewFile();
            //            PrintWriter writer = new PrintWriter(outputFile);
            //            for (int i : generatedSummary) {
            //                String sent = document.originalSentences.get(i);
            //                //                System.out.println(sent);
            //                writer.println(sent);
            //            }
            //            writer.flush();
        }
        System.out.println("Average ROUGE: " + total / documents.size());
        return System.currentTimeMillis() - startTime;
    }

    private static double test(List<String> systemSummary, List<String> goldStandardSummary) {
        //            int numCorrect = 0;
        //            for (int i : document.answerNums) {
        //                if (generatedSummary.contains(i)) {
        //                    numCorrect++;
        //                }
        //            }
        //            double ratioCorrect = (double) numCorrect / document.answerNums.size();
        RougeSummaryModel sysSum = new RougeSummaryModel(null);
        for (String sentence : systemSummary) {
            sysSum.addSentence(sentence);
        }
        RougeSummaryModel gs = new RougeSummaryModel(null);
        for (String sentence : goldStandardSummary) {
            gs.addSentence(sentence);
        }
        Set<IRougeSummaryModel> s = new HashSet<>();
        s.add(gs);
        RougeN rouge = new RougeN(sysSum, s, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 'A', 0.5);
        Map<ScoreType, Double> results = rouge.computeNGramScore();
        return results.get(ScoreType.R);
    }

    private static void populateStopWordList(File file) throws FileNotFoundException {
        FileReader reader = new FileReader(file);
        Scanner scanner = new Scanner(reader);
        while (scanner.hasNextLine()) {
            stopWords.add(scanner.nextLine());
        }
    }

    private static LabelledDocument parseFile(File file) throws FileNotFoundException {
        FileReader reader = new FileReader(file);
        Scanner scanner = new Scanner(reader);
        List<String> filteredSentences = new ArrayList<>();
        Set<String> vocabulary = new HashSet<>();
        List<Integer> answerNums = new ArrayList<>();
        List<String> originalSentences = new ArrayList<>();
        List<String> summarySentences = new ArrayList<>();
        int lineNum = 0;
        int lengthSummary = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("2 ")) {
                lengthSummary++;
                summarySentences.add(line.substring(2));
                continue;
            }
            String[] sentence = line.substring(2).split(" ");
            StringBuilder sb = new StringBuilder();
            for (String word : sentence) {
                if (!stopWords.contains(word)) {
                    sb.append(word).append(" ");
                    vocabulary.add(word);
                }
            }
            filteredSentences.add(sb.toString());
            originalSentences.add(line.substring(2));
            if (line.startsWith("1 ")) {
                answerNums.add(lineNum);
            }
            lineNum++;
        }
        return new LabelledDocument(file.getName(), originalSentences, answerNums, filteredSentences,
                summarySentences, vocabulary, lengthSummary);
    }

    private static void filter(Set<String> sentenceSet) {
        sentenceSet.removeIf(word -> stopWords.contains(word));
    }

}
