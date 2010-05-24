package stemming;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;

/* This is the Porter stemming algorithm, coded up in JAVA by Gianni Amati.
   All comments were made by Porter, but few ones due to some implementation
   choices. 

   Porter says " 
   It may be be regarded as cononical, in that it follows the
   algorithm presented in

   Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14,
   no. 3, pp 130-137,

   only differing from it at the points marked --DEPARTURE-- below.
   The algorithm as described in the paper could be exactly replicated
   by adjusting the points of DEPARTURE, but this is barely necessary,
   because (a) the points of DEPARTURE are definitely improvements, and
   (b) no encoding of the Porter stemmer I have seen is anything like
   as exact as this version, even with the points of DEPARTURE!"

 */


/**
 * This is the Porter stemming algorithm, coded up in JAVA by Gianni Amati.
 * All comments were made by Porter, but few ones due to some implementation
 * choices. Porter says "It may be be regarded as cononical, in that it follows the
 * algorithm presented in Porter, 1980, An algorithm for suffix stripping, Program, 
 * Vol. 14, no. 3, pp 130-137, only differing from it at the points marked 
 * --DEPARTURE-- below. The algorithm as described in the paper could be exactly 
 * replicated by adjusting the points of DEPARTURE, but this is barely necessary,
 * because (a) the points of DEPARTURE are definitely improvements, and
 * (b) no encoding of the Porter stemmer I have seen is anything like
 * as exact as this version, even with the points of DEPARTURE!"
 * Creation date: (27/05/2003 14:31:09)
 * @author Gianni Amati
 * @version 1
 */
public class PorterStemmer implements Stemmer {
	public static int[] b; /* buffer for word to be stemmed */
    private static int k;
    private static int k0;
    private static int j; /* j is a general offset into the string */

    /* cons(i) is TRUE <=> b[i] is a consonant. */
    private static boolean cons(int i) {
        switch (b[i]) {
            case 'a' :
            case 'e' :
            case 'i' :
            case 'o' :
            case 'u' :
                return false;
            case 'y' :
                return (i == k0) ? true : !cons(i - 1);
            default :
                return true;
        }
    }
    private static boolean consonantinstem() {
        int i;
        for (i = k0; i <= j; i++)
            if (cons(i))
                return true;
        return false;
    }
    /* cvc(i) is TRUE <=> i-2,i-1,i has the form consonant - vowel - consonant
       and also if the second c is not w,x or y. this is used when trying to
       restore an e at the end of a short word. e.g.
    
          cav(e), lov(e), hop(e), crim(e), but
          snow, box, tray.
    
    */

    private static boolean cvc(int i) {
        if (i < k0 + 2 || !cons(i) || cons(i - 1) || !cons(i - 2))
            return false;
        {
            int ch = b[i];
            if (ch == 'w' || ch == 'x' || ch == 'y')
                return false;
        }
        return true;
    }
    private static void defineBuffer(String s) {
        StringReader sr = new StringReader(s);
        int ch;
        int i = 0;
        b = new int[s.length()];
        try {

            while ((ch = sr.read()) != -1) {
                b[i] = ch;
                i++;
            }
            sr.close();
        } catch (IOException e) {
            System.out.println(e + " in Stemmer");
        }
    }
    /* doublec(j) is TRUE <=> j,(j-1) contain a double consonant. */

    private static boolean doublec(int j) {
        if (j < k0 + 1)
            return false;
        if (b[j] != b[j - 1])
            return false;
        return cons(j);
    }
    /* ends(s) is TRUE <=> k0,...k ends with the string s. 
    This can be optimized. I used Java' endsWith() method!*/

    private static boolean ends(String s) {
        StringWriter sw = new StringWriter();
        for (int i = k0; i <= k; i++) {
            sw.write(b[i]);
        }
        boolean b;
        if (b = sw.toString().endsWith(s))
            j = k - s.length();
        return b;
    }
    /* m() measures the number of consonant sequences between k0 and j. if c is
     a consonant sequence and v a vowel sequence, and <..> indicates arbitrary
     presence,
    
        <c><v>       gives 0
        <c>vc<v>     gives 1
        <c>vcvc<v>   gives 2
        <c>vcvcvc<v> gives 3
    ....*/
    private static int m() {
        int n = 0;
        int i = k0;
        while (true) {
            if (i > j)
                return n;
            if (!cons(i))
                break;
            i++;
        }
        i++;
        while (true) {
            while (true) {
                if (i > j)
                    return n;
                if (cons(i))
                    break;
                i++;
            }
            i++;
            n++;
            while (true) {
                if (i > j)
                    return n;
                if (!cons(i))
                    break;
                i++;
            }
            i++;
        }
    }
    /* setto(s) sets (j+1),...k to the characters in the string s, readjusting
       k and j. */

    private static void setto(int i1, int i2, String str) {
        StringReader sr = new StringReader(str);
        try {
            for (int i = k - i1 + 1; i <= k - i1 + i2; i++) {
                b[i] = sr.read();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        k = k - i1 + i2;
        j = k;
    }
    /* In stem(p,i,j), p is a char pointer, and the string to be stemmed is from
       p[i] to p[j] inclusive. Typically i is zero and j is the offset to the last
       character of a string, (p[j+1] == '\0'). The stemmer adjusts the
       characters p[i] ... p[j] and returns the new end-point of the string, k.
       Stemming never increases word length, so i <= k <= j. To turn the stemmer
       into a module, declare 'stem' as extern, and delete the remainder of this
       file.
    */

    public String stem(String s) {
     	try {   
		    k = s.length() - 1;
	        k0 = 0;
	        j = k;
	        defineBuffer(s);
	        if (k <= k0 + 1)
	            return s; /*-DEPARTURE-*/
	        /* With this line, strings of length 1 or 2 don't go through the
	        stemming process, although no mention is made of this in the
	        published algorithm. Remove the line to match the published
	        algorithm. */
	        step1ab();
	        step1c();
	        step2();
	        step3();
	        step4();
	        step5();
	        StringWriter sw = new StringWriter();
	        for (int i = 0; i <= k; i++)
	            sw.write(b[i]);

	        return sw.toString();
		} catch(Exception e) {
	     	return s;
		}
    }
    /* step1ab() gets rid of plurals and -ed or -ing. e.g.
    
           caresses  ->  caress
           ponies    ->  poni
           ties      ->  ti
           caress    ->  caress
           cats      ->  cat
    
           feed      ->  feed
           agreed    ->  agree
           disabled  ->  disable
    
           matting   ->  mat
           mating    ->  mate
           meeting   ->  meet
           milling   ->  mill
           messing   ->  mess
    
           meetings  ->  meet
    
    */

    private static void step1ab() {
        if (b[k] == 's') {
            if (ends("sses"))
                k -= 2;
            else
                if (ends("ies") && k > 2)
                    setto(3, 1, "i");
                else
                    if (b[k - 1] != 's' && k > 2)
                        k--;
        }
        if (ends("eed")) {
            if (m() > 0)
                k--;
        } else
            if ((ends("ed") || ends("ing")) && vowelinstem() && consonantinstem()) {
                k = j;
                if (ends("at"))
                    setto(2, 3, "ate");
                else
                    if (ends("bl"))
                        setto(2, 3, "ble");
                    else
                        if (ends("iz"))
                            setto(2, 3, "ize");
                        else
                            if (doublec(k)) {
                                k--;
                                {
                                    int ch = b[k];
                                    if (ch == 'l' || ch == 's' || ch == 'z')
                                        k++;
                                }
                            } else
                                if (m() == 1 && cvc(k))
                                    setto(0, 1, "e");
            }
    }
    /* step1c() turns terminal y to i when there is another vowel in the stem. */
    private static void step1c() {
        if (ends("y") && vowelinstem())
            b[k] = 'i';
    }
    /* step2() maps double suffices to single ones. so -ization ( = -ize plus
     -ation) maps to -ize etc. note that the string before the suffix must give
     m() > 0. */
    private static void step2() {
        switch (b[k - 1]) {
            case 'a' :
                if (ends("ational")) {
                    if (m() > 0)
                        setto(7, 3, "ate");
                    break;
                }
                if (ends("tional")) {
                    if (m() > 0)
                        setto(6, 4, "tion");
                    break;
                }
                break;
            case 'c' :
                if (ends("enci")) {
                    if (m() > 0)
                        setto(4, 4, "ence");
                    break;
                }
                if (ends("anci")) {
                    if (m() > 0)
                        setto(4, 4, "ance");
                    break;
                }
                break;
            case 'e' :
                if (ends("izer")) {
                    if (m() > 0)
                        setto(4, 3, "ize");
                    break;
                }
                break;
            case 'l' :
                if (ends("bli")) {
                    if (m() > 0)
                        setto(3, 3, "ble");
                    break;
                } /*-DEPARTURE-*/
                /* To match the published algorithm, replace this line with
                case 'l': if (ends("\04" "abli")) { r("\04" "able"); break; } */
                if (ends("alli")) {
                    if (m() > 0)
                        setto(4, 2, "al");
                    break;
                }
                if (ends("entli")) {
                    if (m() > 0)
                        setto(5, 3, "ent");
                    break;
                }
                if (ends("eli")) {
                    if (m() > 0)
                        setto(3, 1, "e");
                    break;
                }
                if (ends("ousli")) {
                    if (m() > 0)
                        setto(5, 3, "ous");
                    break;
                }
                break;
            case 'o' :
                if (ends("ization")) {
                    if (m() > 0)
                        setto(7, 3, "ize");
                    break;
                }
                if (ends("ation")) {
                    if (m() > 0)
                        setto(5, 3, "ate");
                    break;
                }
                if (ends("ator")) {
                    if (m() > 0)
                        setto(4, 3, "ate");
                    break;
                }
                break;
            case 's' :
                if (ends("alism")) {
                    if (m() > 0)
                        setto(5, 2, "al");
                    break;
                }
                if (ends("iveness")) {
                    if (m() > 0)
                        setto(7, 3, "ive");
                    break;
                }
                if (ends("fulness")) {
                    if (m() > 0)
                        setto(7, 3, "ful");
                    break;
                }
                if (ends("ousness")) {
                    if (m() > 0)
                        setto(7, 3, "ous");
                    break;
                }
                break;
            case 't' :
                if (ends("aliti")) {
                    if (m() > 0)
                        setto(5, 2, "al");
                    break;
                }
                if (ends("iviti")) {
                    if (m() > 0)
                        setto(5, 3, "ive");
                    break;
                }
                if (ends("biliti")) {
                    if (m() > 0)
                        setto(6, 3, "ble");
                    break;
                }
                break;
            case 'g' :
                if (ends("logi")) {
                    if (m() > 0)
                        setto(4, 3, "log");
                    break;
                }
                /*-DEPARTURE-*/
                /* To match the published algorithm, delete this line */
        }
    }
    /* step3() deals with -ic-, -full, -ness etc. similar strategy to step2. */

    private static void step3() {
        switch (b[k]) {
            case 'e' :
                if (ends("icate")) {
                    if (m() > 0)
                        setto(5, 2, "ic");
                    break;
                }
                if (ends("ative")) {
                    if (m() > 0)
                        setto(5, 0, "");
                    break;
                }
                if (ends("alize")) {
                    if (m() > 0)
                        setto(5, 2, "al");
                    break;
                }
                break;
            case 'i' :
                if (ends("iciti")) {
                    if (m() > 0)
                        setto(5, 2, "ic");
                    break;
                }
                break;
            case 'l' :
                if (ends("ical")) {
                    if (m() > 0)
                        setto(4, 2, "ic");
                    break;
                }
                if (ends("ful")) {
                    if (m() > 0)
                        setto(3, 0, "");
                    break;
                }
                break;
            case 's' :
                if (ends("ness")) {
                    if (m() > 0)
                        setto(4, 0, "");
                    break;
                }
                break;
        }
    }
    /* step4() takes off -ant, -ence etc., in context <c>vcvc<v>. */

    private static void step4() {
        switch (b[k - 1]) {
            case 'a' :
                if (ends("al"))
                    break;
                return;
            case 'c' :
                if (ends("ance"))
                    break;
                if (ends("ence"))
                    break;
                return;
            case 'e' :
                if (ends("er"))
                    break;
                return;
            case 'i' :
                if (ends("ic"))
                    break;
                return;
            case 'l' :
                if (ends("able"))
                    break;
                if (ends("ible"))
                    break;
                return;
            case 'n' :
                if (ends("ant"))
                    break;
                if (ends("ement"))
                    break; /*-DEPARTURE-*/
                if (ends("ment"))
                    break;

                /* To match the published algorithm, replace the previous two lines with
                
                             if (ends("\05" "ement")) if (m()>1) { k=j; return; };
                             if (ends("\04" "ment")) if (m()>1) { k=j; return; }; */

                if (ends("ent"))
                    break;
                return;
            case 'o' :
                if (ends("tion"))
                    break;
                if (ends("sion"))
                    break;
                if (ends("ou"))
                    break;
                return;
                /* takes care of -ous */
            case 's' :
                if (ends("ism"))
                    break;
                return;
            case 't' :
                if (ends("ate"))
                    break;
                if (ends("iti"))
                    break;
                return;
            case 'u' :
                if (ends("ous"))
                    break;
                return;
            case 'v' :
                if (ends("ive"))
                    break;
                return;
            case 'z' :
                if (ends("ize"))
                    break;
                return;
            default :
                return;
        }
        if (m() > 1)
            k = j;
    }
    /* step5() removes a final -e if m() > 1, and changes -ll to -l if
      m() > 1. */

    private static void step5() {
        j = k;
        if (b[k] == 'e') {
            int a = m();
            if (a > 1 || a == 1 && !cvc(k - 1))
                k--;
        }
        if (b[k] == 'l' && doublec(k) && m() > 1)
            k--;
    }
    /* vowelinstem() is TRUE <=> k0,...j contains a vowel */

    private static boolean vowelinstem() {
        int i;
        for (i = k0; i <= j; i++)
            if (!cons(i))
                return true;
        return false;
    }
}
