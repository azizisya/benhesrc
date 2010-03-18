import org.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.optimisation.ManyVariableFunction;
/** Performs a 'grid' search of the QE parameter space. Will set <tt>opt.querying</tt> to 
  * <tt>uk.ac.gla.terrier.applications.TRECQueryingExpansion</tt> if it remains at it's
  * default value. */
public class GridQESettings
{
	public static void main(String args[])
	{
		if (args.length < 5)
		{
			System.err.println("Usage: GridQESettings ManyVariableFunction ExpansionTermsMin ExpansionTermsMax ExpansionDocsMin ExpansionDocsMax [ExpansionTermsDelta]");
			System.exit(1);
		}
		try {
			//disable caching
			ApplicationSetup.setProperty("opt.cache", "false");
			String defaultQuerying = ApplicationSetup.getProperty("opt.querying", null);
			if (defaultQuerying == null //if it is not set, or it is the default, correct it for them
				|| defaultQuerying.equals("uk.ac.gla.terrier.applications.TRECQuerying") 
				|| defaultQuerying.equals("TRECQuerying"))
			{
				ApplicationSetup.setProperty("opt.querying","uk.ac.gla.terrier.applications.TRECQueryingExpansion");
			}
				
			//ApplicationSetup.setProperty("querying.default.controls", ApplicationSetup.getProperty("querying.default.controls",null)+",qe:on");
			final Class functionClass = Class.forName(args[0]);
			final ManyVariableFunction f = (ManyVariableFunction) functionClass.newInstance();
			
		
			final int minTerms = Integer.parseInt(args[1]);	
			final int maxTerms = Integer.parseInt(args[2]);
			final int minDocs = Integer.parseInt(args[3]);
			final int maxDocs = Integer.parseInt(args[4]);

			int stepTerms = minTerms < maxTerms ? 1 : -1;
			final int stepDocs = minDocs < maxDocs ? 1 : -1;
			if (args.length > 5)
			   stepTerms = stepTerms * Integer.parseInt(args[5]);
			final double c = 10.08d;//1.0d; //TODO: careful for c values

			for(int terms=minTerms;terms<=(maxTerms+stepTerms);terms+=stepTerms)
			{
				for(int docs=minDocs;docs<=(maxDocs+stepDocs);docs+=stepDocs)
				{
					System.out.println("ApplicationSetup.EXPANSION_TERMS="+terms);
					System.out.println("ApplicationSetup.EXPANSION_DOCUMENTS="+docs);
					ApplicationSetup.EXPANSION_TERMS=terms;
					ApplicationSetup.EXPANSION_DOCUMENTS = docs;
					f.evaluate(new double[]{c});
				}
			}
			f.close();
			System.err.println("GridQESettings done");
		}
		catch (Exception e) {
			System.err.println("GridQESettings failed : "+ e);
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
