import JRouge.common.RougeSummaryModel;
import JRouge.common.ScoreType;
import JRouge.interfaces.IRougeSummaryModel;
import JRouge.rouge.RougeN;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class RougeEval {
    static final String generatedPath = "error_analysis/mcp10";
    static final String dataPath = "extracted_10k";
    public static void main(String[] args) throws FileNotFoundException {
        FilenameFilter storyFilter = (dir, name) -> {
            String lowercaseName = name.toLowerCase();
            return lowercaseName.endsWith(".story");
        };
        File dir = new File(generatedPath);
        File dataDir = new File(dataPath);
        File[] files = dir.listFiles(storyFilter);
        double recallTotal = 0;
        double f1Total = 0;
        double recall2total = 0;

        for (int i = 0, filesLength = files.length; i < filesLength; i++) {
            if (i % 500 == 0) {
                System.out.println(i + " completed");
            }
            File file = files[i];
            String fileName = file.getName();
            RougeSummaryModel goldStandard = getAnswers(new File(dataDir, fileName));
            RougeSummaryModel systemSummary = getSystemSummary(file);
            Set<IRougeSummaryModel> s = new HashSet<>();
            s.add(goldStandard);
            RougeN rouge = new RougeN(systemSummary, s, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 'A', 0.5);
            RougeN rouge2 = new RougeN(systemSummary, s, Integer.MAX_VALUE, Integer.MAX_VALUE, 2, 'A', 0.5);
            Map<ScoreType, Double> results = rouge.computeNGramScore();
            recall2total += rouge2.computeNGramScore().get(ScoreType.R);
            recallTotal += results.get(ScoreType.R);
            //            System.out.println(results.get(ScoreType.R));
            f1Total += results.get(ScoreType.F);
        }

        System.out.println("Average R1: " + (recallTotal/files.length));
        System.out.println("Average F1: " + (f1Total/files.length));
        System.out.println("Average R2: " + (recall2total/files.length));

    }


    // gets abstractive summaries from the reference files
    private static RougeSummaryModel getSystemSummary(File file) throws FileNotFoundException {
        RougeSummaryModel sysSum = new RougeSummaryModel(null);
        FileReader reader = new FileReader(file);
        Scanner scanner =  new Scanner(reader);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            sysSum.addSentence(line);
        }
        return sysSum;
    }

    // gets abstractive summaries from the reference files
    private static RougeSummaryModel getAnswers(File file) throws FileNotFoundException {
        RougeSummaryModel gs = new RougeSummaryModel(null);
        FileReader reader = new FileReader(file);
        Scanner scanner =  new Scanner(reader);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("2 ")) {
                String sentence = line.substring(2);
                gs.addSentence(sentence);
            }
        }
        return gs;
    }
}
