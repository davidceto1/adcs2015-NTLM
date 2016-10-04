import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.terrier.querying.Request;
import org.terrier.structures.Index;
import org.terrier.structures.outputformat.TRECDocnoOutputFormat;

/**
 * 
 */

/**
 * @author zuccong
 *
 */
public class TestGloveP2VFull {

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Usage: ");
		System.out.println("args[0]: path to terrier.home");
		System.out.println("args[1]: path to index");
		System.out.println("args[2]: path to trec query file");
		System.out.println("args[3]: path to result file (including name of result file)");
		System.out.println("args[4]: path to vectors");
		System.out.println("args[5]: glove (global vectors) or p2v (paragraph vectors)");
		System.out.println("args[6]: number of top translation terms, 0 to select threshold on similarity value");
		System.out.println("args[7]: value of mu");
		System.out.println("args[8]: threshold on similarity value");
		System.out.println("args[9]: \"_SUM\" if the query should be summed in one vector, empty otherwise");
		
		int numtopterms = Integer.parseInt(args[6]);
		double mu = Double.parseDouble(args[7]);
		
		double similarityThreshold = 0;
		//indicates if the query terms should be summed in one vector
		String sum = "";
		//Optional parameters
		if(args.length > 8)
		{
			similarityThreshold = Double.parseDouble(args[8]);
		}
		if(args.length > 9)
		{
			sum = args[9];
		}
		
		System.out.println("numtopterms set to " + numtopterms);
		System.setProperty("terrier.home", args[0]);
		Index index = Index.createIndex(args[1], "data");
		System.out.println(index.getEnd());

		
		TranslationLMManager tlm_w2v_skipgram = new TranslationLMManager(index);
		tlm_w2v_skipgram.setTranslation("w2v");
		tlm_w2v_skipgram.setRarethreshold(index.getCollectionStatistics().getNumberOfDocuments()/200);
		tlm_w2v_skipgram.setTopthreshold(index.getCollectionStatistics().getNumberOfDocuments()/2);
		tlm_w2v_skipgram.setDirMu(mu);
		tlm_w2v_skipgram.setNumber_of_top_translation_terms(numtopterms);
		tlm_w2v_skipgram.setSimilarityThreshold(similarityThreshold);
		System.out.println("Initialise word2vec translation");
		tlm_w2v_skipgram.initialiseW2V_atquerytime(args[4]);
		System.out.println("word2vec translation initialised");
	
		HashMap<String,String> trecqueries = new HashMap<String,String>();
		BufferedReader br = new BufferedReader(new FileReader(args[2]));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] input = line.split(" ");
			String qid = input[0];
			String query ="";
			for(int i=1; i<input.length;i++)
				query = query + " " + input[i];
			
			query = query.replaceAll("-", " ");
			query = query.replaceAll("\\p{Punct}", "");
			query = query.substring(1, query.length());
			trecqueries.put(qid, query.toLowerCase());
		}
		br.close();
		
		TRECDocnoOutputFormat TRECoutput_w2v_skipgram = new TRECDocnoOutputFormat(index);
		PrintWriter pt_w2v = new PrintWriter(new File(args[3] + "_dir_w2v_"+ args[5] +"_full.txt"));

		for(String qid : trecqueries.keySet()) {
			String query = trecqueries.get(qid);
			System.out.println(query + " - " + qid);

			
			System.out.println("Scoring with Dir TLM with w2v ("+args[5]+")");
			//scoring with LM dir w2v
			Request rq_w2v = new Request();
			rq_w2v.setOriginalQuery(query);
			rq_w2v.setQueryID(qid);
			rq_w2v = tlm_w2v_skipgram.runMatching(rq_w2v, "w2v_full"+sum, "dir");
			TRECoutput_w2v_skipgram.printResults(pt_w2v, rq_w2v, "dir_w2v_"+args[5]+"_full", "Q0", 1000);
			
		}
		pt_w2v.flush();
		pt_w2v.close();
		
	}
}
