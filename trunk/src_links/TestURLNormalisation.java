import uk.ac.gla.terrier.links.URLServer;

public class TestURLNormalisation {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String url = "http://www.dcs.gla.ac.uk/";
		String normalised = URLServer.normaliseURL(url);
		System.out.println("before: " + url + " after: " + normalised);
		
		url = "https://www.dcs.gla.ac.uk/";
		normalised = URLServer.normaliseURL(url);
		System.out.println("before: " + url + " after: " + normalised);
		
		url = "https://www.dcs.gla.ac.uk/index.html";
		normalised = URLServer.normaliseURL(url);
		System.out.println("before: " + url + " after: " + normalised);

	}

}
