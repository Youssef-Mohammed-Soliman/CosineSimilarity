import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String[] filenames = { "file1.txt", "file2.txt", "file3.txt", "file4.txt", "file5.txt",
                "file6.txt", "file7.txt", "file8.txt", "file9.txt", "file10.txt" };

        WebCrawler crawler = new WebCrawler();

        Scanner scanner = new Scanner(System.in);

        InvertedIndex invertedIndex = new InvertedIndex();
        try {
            for (int i = 0; i < filenames.length; i++) {
                String filename = filenames[i];

                // random access file
                RandomAccessFile file = new RandomAccessFile(filename, "r");
                int docId = i + 1; // docId starts from 1
                String line;
                int position = 1; // position of the term in the document

                // read each line and split it into words
                while ((line = file.readLine()) != null) {
                    // split line into words and remove punctuation marks
                    line = line.replaceAll("[^a-zA-Z0-9]", " ");
                    // remove extra spaces
                    line = line.replaceAll("\\s+", " ");
                    line = line.trim();
                    line = line.toLowerCase();
                    // split line into words
                    String[] words = line.split(" ");

                    // add each word to the inverted index
                    for (String word : words) {
                        invertedIndex.addTerm(word, docId, position);
                        position++;
                    }
                }
                file.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        int choice = 0;
        while (choice != 4) {
            System.out.println("1-Enter a query");
            System.out.println("2-Print TF-IDF");
            System.out.println("3-Webcrawler");
            System.out.println("4-Exit the program");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine(); // consume the newline character

            if (choice == 1) {
                System.out.println();
                System.out.print("Enter a query: ");
                String query = scanner.nextLine();
                query = query.toLowerCase();
                System.out.println();
                Map<Integer, Double> documentScores = new HashMap<>();

                String[] queryTerms = query.split("\\W+"); // split query into words

                // calculate cosine similarity for each document and the query
                for (int docId = 1; docId <= filenames.length; docId++) {
                    double score = 0;

                    // calculate the numerator of the cosine similarity
                    for (String term : queryTerms) {
                        int termFrequencyInQuery = invertedIndex.getTFInQuery(term, queryTerms);
                        int termFrequencyInDocument = invertedIndex.getTFInDocument(term, docId);
                        score += termFrequencyInQuery * termFrequencyInDocument;
                    }

                    // calculate the denominator of the cosine similarity
                    double documentVectorLength = invertedIndex.getDocumentVectorLength(docId);
                    double queryVectorLength = invertedIndex.getQueryVectorLength(queryTerms);
                    double denominator = documentVectorLength * queryVectorLength;

                    // calculate the cosine similarity
                    score /= denominator;
                    // add the document and its score to the map
                    documentScores.put(docId, score);

                }

                // sort the documents based on cosine similarity
                List<Map.Entry<Integer, Double>> sortedDocuments = new ArrayList<>(documentScores.entrySet());
                sortedDocuments.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

                // print the ranked documents
                System.out.println("**********");
                System.out.println("Documents:");
                System.out.println("**********");
                for (Map.Entry<Integer, Double> entry : sortedDocuments) {
                    int docId = entry.getKey();
                    String filename = filenames[docId - 1];
                    double score = entry.getValue();
                    System.out.println(filename + ": cosine similarity(" + score + ")");
                    System.out.println("-------------------------------------------------");
                }
                System.out.println();

            } else if (choice == 2) {
                Map<String, Map<Integer, String>> TF_IDF = new HashMap<>();
                TF_IDF = getTF_IDF(invertedIndex, filenames.length);

                // print the TF-IDF
                int maxTermLength = 0;
                for (String term : TF_IDF.keySet()) {
                    maxTermLength = Math.max(maxTermLength, term.length());
                }
                System.out.println();
                System.out.println("***********");
                System.out.println("The TF-IDF:");
                System.out.println("***********");
                System.out
                        .println("###################################################################################");
                System.out.print("Term:");
                for (int i = 0; i < maxTermLength - 3; i++) {
                    System.out.print(" ");
                }
                for (int docId = 1; docId <= filenames.length; docId++) {
                    System.out.print("f" + docId + "     ");
                }
                System.out.println();
                System.out
                        .println("###################################################################################");

                for (String term : TF_IDF.keySet()) {
                    System.out.print(term);
                    for (int i = 0; i < maxTermLength - term.length(); i++) {
                        System.out.print(" ");
                    }
                    for (int docId = 1; docId <= filenames.length; docId++) {
                        String score = TF_IDF.get(term).get(docId);
                        System.out.print(score + "  ");
                    }
                    System.out.println();
                }
                System.out
                        .println("###################################################################################");
                System.out.println();
            } else if (choice == 3) {
                System.out.println();
                System.out.print("Enter The URL:");
                System.out.println();
                String URL = scanner.nextLine();
                System.out.println(" ");
                System.out.println("******");
                System.out.println("Links:");
                System.out.println("******");
                crawler.getPageLinks(URL);
                System.out.println(" ");

            } else if (choice == 4) {
                System.out.println("Exiting the program...");
            } else {
                System.out.println("Invalid choice. Please try again.");
            }
        }

        scanner.close();
    }

    public static Map<String, Map<Integer, String>> getTF_IDF(InvertedIndex invertedIndex, int LengthFilenames) {
        // get the length of each document
        Map<Integer, Integer> documentLengths = new HashMap<>();
        for (int docId = 1; docId <= LengthFilenames; docId++) {
            int length = invertedIndex.getDocumentWordsLength(docId);
            documentLengths.put(docId, length);
        }
        // get the term frequency in each document
        Map<String, Map<Integer, Integer>> termFrequencies = new HashMap<>();
        for (String term : invertedIndex.getTerms()) {
            Map<Integer, Integer> frequencies = new HashMap<>();
            for (int docId = 1; docId <= LengthFilenames; docId++) {
                int frequency = invertedIndex.getTFInDocument(term, docId);
                frequencies.put(docId, frequency);
            }
            termFrequencies.put(term, frequencies);
        }

        // get the normalized Term Frequency (TF)
        Map<String, Map<Integer, Double>> normalizedTermFrequencies = new HashMap<>();
        for (String term : invertedIndex.getTerms()) {
            Map<Integer, Double> frequencies = new HashMap<>();
            for (int docId = 1; docId <= LengthFilenames; docId++) {
                int frequency = termFrequencies.get(term).get(docId);
                int length = documentLengths.get(docId);
                double normalizedFrequency = frequency / (double) length;
                frequencies.put(docId, normalizedFrequency);
            }
            normalizedTermFrequencies.put(term, frequencies);
        }

        // get the Inverse Document Frequency (IDF)
        Map<String, Double> inverseDocumentFrequencies = new HashMap<>();
        for (String term : invertedIndex.getTerms()) {
            double IDF = invertedIndex.getInverseDocumentFrequency(term);
            inverseDocumentFrequencies.put(term, IDF);
        }

        // get the TF-IDF
        Map<String, Map<Integer, String>> TF_IDF = new HashMap<>();
        for (String term : invertedIndex.getTerms()) {
            Map<Integer, String> TF_IDF_scores = new HashMap<>();
            for (int docId = 1; docId <= LengthFilenames; docId++) {
                double TF = normalizedTermFrequencies.get(term).get(docId);
                double IDF = inverseDocumentFrequencies.get(term);
                // double is 3 number after point if zero must be 3 number after point
                DecimalFormat df = new DecimalFormat("0.000");
                String TF_IDF_score = df.format(TF * IDF);
                TF_IDF_scores.put(docId, TF_IDF_score);
            }
            TF_IDF.put(term, TF_IDF_scores);
        }
        return TF_IDF;

    }
}
