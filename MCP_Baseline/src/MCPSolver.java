
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.sf.javailp.Constraint;
import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryGLPK;
import net.sf.javailp.SolverFactoryLpSolve;
import net.sf.javailp.VarType;

public class MCPSolver {

    private TFIDF tfidf;

    public MCPSolver(TFIDF tfidf) {
        this.tfidf = tfidf;
    }

    public Set<Integer> simpleGreedy(List<String> sentenceStrings, int numSummarySentences) {

        List<Set<String>> sentenceSets = new ArrayList<>(sentenceStrings.size());
        for (int i = 0; i < sentenceStrings.size(); i++) {
            String sentenceStr = sentenceStrings.get(i);
            String[] words = sentenceStr.split(" ");
            Set<String> sentenceSet = new HashSet<>(Arrays.asList(words));
            sentenceSets.add(sentenceSet);
        }

        Set<Integer> chosenIndices = new HashSet<>();
        Set<String> chosenWords = new HashSet<>();
        for (int i = 0; i < numSummarySentences; i++) {
            int bestSentence = -1;
            double bestScore = -1;
            for (int j = 0; j < sentenceSets.size(); j++) {
                Set<String> sentenceSet = sentenceSets.get(j);
                double score = 0;
                for (String word : sentenceSet) {
                    if (!chosenWords.contains(word)) {
                        score++;
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestSentence = j;
                }
            }
            chosenIndices.add(bestSentence);
            chosenWords.addAll(sentenceSets.get(bestSentence));
        }
        return chosenIndices;
    }

    public Set<Integer> unweightedILP(List<String> sentenceStrings,
            int numSummarySentences) {
        Set<String> vocabulary = new HashSet<>();
        List<Set<String>> sentenceSets = new ArrayList<>(sentenceStrings.size());
        Map<String, Set<Integer>> wordToSentIdx = new HashMap<>();
        for (int i = 0; i < sentenceStrings.size(); i++) {
            String sentenceStr = sentenceStrings.get(i);
            String[] words = sentenceStr.split(" ");
            Set<String> sentenceSet = new HashSet<>(Arrays.asList(words));
            sentenceSets.add(sentenceSet);
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
        SolverFactory factory = new SolverFactoryGLPK();
        Problem problem = new Problem();
        // the OPT function is the sum of all words
        Linear optFunction = new Linear();
        // these constraints ensure that each word that's counted in OPT comes
        // from some chosen set
        Linear wordConstraints = new Linear();
        for (String word : vocabulary) {
            String name = word;
            if (name.length() > 250) {
                name = name.substring(0, 250);
            }
            optFunction.add(1, name);
            for (Integer idx : wordToSentIdx.get(word)) {
                wordConstraints.add(1, idx);
            }
            wordConstraints.add(-1, name);
            problem.add(name, wordConstraints, ">=", 0);
            wordConstraints = new Linear();
        }
        problem.setObjective(optFunction, OptType.MAX);
        // this constraint ensures that we choose at most k sets,
        // where k = numSummarySentences
        Linear setConstraints = new Linear();
        for (Integer i = 0; i < sentenceSets.size(); i++) {
            setConstraints.add(1, i);
        }
        problem.add("Set Constraint", setConstraints, "<=", numSummarySentences);
        for (Object variable : problem.getVariables()) {
            problem.setVarType(variable, VarType.BOOL);
        }
        for (Constraint constraint : problem.getConstraints()) {
            String name = constraint.getName();
            if (name.length() > 255) {
                System.out.println(name);
                System.out.println(name.length());
                System.out.println();
            }
        }
        Solver solver = factory.get();
        solver.setParameter(Solver.VERBOSE, 0);
        Result result = solver.solve(problem);
//        System.out.println(result);
        Set<Integer> chosenIndices = new HashSet<>();
        for (Integer i = 0; i < sentenceSets.size(); i++) {
            if (result.getBoolean(i)) {
                chosenIndices.add(i);
            }
        }
        return chosenIndices;
    }

    public Set<Integer> weightedILP(List<String> sentenceStrings,
            int numSummarySentences) {
        Set<String> vocabulary = new HashSet<>();
        List<Set<String>> sentenceSets = new ArrayList<>(sentenceStrings.size());
        Map<String, Set<Integer>> wordToSentIdx = new HashMap<>();
//        List<String> lemmatizedSentenceStrings = new ArrayList<>();
        for (int i = 0; i < sentenceStrings.size(); i++) {
            String sentenceStr = sentenceStrings.get(i);
            if (sentenceStr.equals("")) {
                continue;
            }
            String[] words = sentenceStr.split(" ");
            Set<String> sentenceSet = new HashSet<>(Arrays.asList(words));
//            List<String> lemmas = sentence.lemmas();
//            StringBuilder sb = new StringBuilder();
//            for (String lemma: lemmas) {
//                sb.append(lemma).append(" ");
//            }
//            lemmatizedSentenceStrings.add(sb.toString());
//            Set<String> sentenceSet = new HashSet<>(lemmas);
            sentenceSets.add(sentenceSet);
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
        tfidf.initTF(sentenceStrings);
        //        Map<Integer, String> dictionary = new HashMap<>();
        //        Map<String, Integer> reverseDictionary = new HashMap<>();
        //        int i = 0;
        //        for (String word : vocabulary) {
        //            dictionary.put(i, word);
        //            reverseDictionary.put(word, i);
        //            i++;
        //        }
        SolverFactory factory = new SolverFactoryLpSolve();
        Problem problem = new Problem();
        // the OPT function is the sum of all words
        Linear optFunction = new Linear();
        // these constraints ensure that each word that's counted in OPT comes
        // from some chosen set
        Linear wordConstraints = new Linear();
        for (String word : vocabulary) {
            optFunction.add(tfidf.getTFIDF(word), word);
            for (Integer idx : wordToSentIdx.get(word)) {
                wordConstraints.add(1, idx);
            }
            wordConstraints.add(-1, word);
            problem.add(wordConstraints, ">=", 0);
            wordConstraints = new Linear();
        }
        problem.setObjective(optFunction, OptType.MAX);
        // this constraint ensures that we choose at most k sets,
        // where k = numSummarySentences
        Linear setConstraints = new Linear();
        for (Integer i = 0; i < sentenceSets.size(); i++) {
            setConstraints.add(1, i);
        }
        problem.add(setConstraints, "<=", numSummarySentences);
        for (Object variable : problem.getVariables()) {
            problem.setVarType(variable, VarType.BOOL);
        }
        Solver solver = factory.get();
        solver.setParameter(Solver.VERBOSE, 0);
        Result result = solver.solve(problem);
        //        System.out.println(result);
        Set<Integer> chosenIndices = new HashSet<>();
        for (Integer i = 0; i < sentenceSets.size(); i++) {
            if (result.getBoolean(i)) {
                chosenIndices.add(i);
            }
        }
        tfidf.clearTF();
        return chosenIndices;
    }

    public Set<Integer> weightedMaxSAT(List<String> sentenceStrings, int numSummarySentences) {
        tfidf.initTF(sentenceStrings);
        MCP_MaxSAT m = new MCP_MaxSAT(sentenceStrings, numSummarySentences, tfidf);
        tfidf.clearTF();
        String satEncoding = m.toSAT();

        try {
            try (PrintWriter out = new PrintWriter("maxsat_encoding.txt")) {
                out.println(satEncoding);
            }

            System.out.println("Running Z3");

            String z3Path = "C:\\Users\\Rory\\Documents\\Programming\\z3-4.6.0-x64-win\\bin\\z3.exe";
            Process process = Runtime.getRuntime().exec(z3Path + " -smt2 maxsat_encoding.txt");
            process.waitFor(60, TimeUnit.SECONDS);
            process.destroy();
            Scanner s = new Scanner(process.getInputStream());

            if (s.hasNext()) {
                // The process finished correctly
                System.out.println("Finished");
                s.next();
                Set<Integer> results = new HashSet();
                int i = 0;
                while (s.hasNext()) {
                    if (s.next().equals("true")) {
                        results.add(i);
                    }
                    i++;
                }
                return results;
            } else {
                // The process didn't terminate in time
                System.out.println("Process didn't terminate in time");
                return new HashSet();
            }

        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
